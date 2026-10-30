package ro.expensesplitter.service;


import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.CreateGroupRequest;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.entity.Group;
import ro.expensesplitter.entity.GroupMember;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMemberRepository;
import ro.expensesplitter.repository.GroupRepository;
import ro.expensesplitter.repository.UserRepository;

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

}
