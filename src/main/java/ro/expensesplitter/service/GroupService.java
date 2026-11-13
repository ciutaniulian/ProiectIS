package ro.expensesplitter.service;


import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.AddMemberRequest;
import ro.expensesplitter.dto.CreateGroupRequest;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.dto.MemberResponse;
import ro.expensesplitter.entity.Group;
import ro.expensesplitter.entity.GroupMember;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMemberRepository;
import ro.expensesplitter.repository.GroupRepository;
import ro.expensesplitter.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor

public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Group group = Group.builder()
                .name(request.name().strip())
                .description(request.description())
                .createdBy(creator)
                .build();
        Group savedGroup = groupRepository.save(group);
        GroupMember membership = GroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .build();
        groupMemberRepository.save(membership);

        return  new GroupResponse(savedGroup.getId(), savedGroup.getName(), savedGroup.getDescription(), creator.getId());
    }

    @Transactional(readOnly = true)
    public boolean isUserMember(Long groupId, Long userId){
        return groupMemberRepository.existsByGroupIdAndUserId(groupId,userId);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups(String userEmail){
        User user = userRepository.findByEmail(userEmail).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<Group> userGroups = groupMemberRepository.findGroupsByUserId(user.getId());
        return userGroups.stream()
                .map(x -> new GroupResponse(x.getId(), x.getName(), x.getDescription(), x.getCreatedBy().getId()))
                .toList();
    }

    @Transactional
    public void addMember(Long groupId, AddMemberRequest memberRequest, String requesterEmail){
        Group group = groupRepository.findById(groupId).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Requester not found"));
        boolean isRequesterPartOfGroup = isUserMember(group.getId(), requester.getId());
        if( ! isRequesterPartOfGroup)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        User newMember = userRepository.findByEmail(memberRequest.email()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "New Member not found"));
        boolean isNewMemberPartOfGroup =  isUserMember(group.getId(),newMember.getId());
        if(isNewMemberPartOfGroup)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "New Member is already a member in this group");
        GroupMember groupMember = GroupMember.builder()
                .group(group)
                .user(newMember)
                .build();
        groupMemberRepository.save(groupMember);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getGroupMembers(Long groupId, String requesterEmail){
        Group group = groupRepository.findById(groupId).orElseThrow(()->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Requester not found"));
        boolean isRequesterPartOfGroup = isUserMember(group.getId(), requester.getId());
        if( ! isRequesterPartOfGroup)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(groupId);
        return groupMembers.stream()
                .map(x->new MemberResponse(x.getUser().getId(),x.getUser().getName(),x.getUser().getEmail(),x.getJoinedAt()))
                .toList();
    }

}
