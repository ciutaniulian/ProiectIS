package ro.expensesplitter.service;


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
import ro.expensesplitter.entity.GroupRole;
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
                .role(GroupRole.ADMIN)
                .build();
        groupMemberRepository.save(membership);

        return toGroupResponse(membership);
    }

    @Transactional(readOnly = true)
    public boolean isUserMember(Long groupId, Long userId){
        return groupMemberRepository.existsByGroupIdAndUserId(groupId,userId);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getMyGroups(String userEmail){
        User user = userRepository.findByEmail(userEmail).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return groupMemberRepository.findMembershipsByUserId(user.getId()).stream()
                .map(GroupService::toGroupResponse)
                .toList();
    }

    private static GroupResponse toGroupResponse(GroupMember membership) {
        Group group = membership.getGroup();
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(),
                group.getCreatedBy().getId(), membership.getRole());
    }

    @Transactional
    public void addMember(Long groupId, AddMemberRequest memberRequest, String requesterEmail){
        Group group = lockGroup(groupId);
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
                .map(x->new MemberResponse(x.getUser().getId(),x.getUser().getName(),x.getUser().getEmail(),x.getJoinedAt(),x.getRole()))
                .toList();
    }

    @Transactional
    public void promoteMember(Long groupId, Long userId, String requesterEmail) {
        lockGroup(groupId);
        requireAdmin(groupId, requesterEmail);
        findTargetMember(groupId, userId).setRole(GroupRole.ADMIN);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId, String requesterEmail) {
        lockGroup(groupId);
        GroupMember admin = requireAdmin(groupId, requesterEmail);
        if (admin.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the leave operation for your own membership");
        }
        groupMemberRepository.delete(findTargetMember(groupId, userId));
    }

    @Transactional
    public void leaveGroup(Long groupId, String requesterEmail) {
        lockGroup(groupId);
        GroupMember member = requireMembership(groupId, requesterEmail);
        if (member.getRole() == GroupRole.ADMIN
                && groupMemberRepository.countByGroupIdAndRole(groupId, GroupRole.ADMIN) == 1
                && groupMemberRepository.countByGroupId(groupId) > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Promote another member before leaving");
        }
        groupMemberRepository.delete(member);
    }

    private Group lockGroup(Long groupId) {
        return groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private GroupMember requireMembership(Long groupId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group"));
    }

    private GroupMember requireAdmin(Long groupId, String email) {
        GroupMember membership = requireMembership(groupId, email);
        if (membership.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group administrators can manage roles and remove members");
        }
        return membership;
    }

    private GroupMember findTargetMember(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
    }
}
