package ro.expensesplitter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.AddMemberRequest;
import ro.expensesplitter.dto.CreateGroupRequest;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.entity.Group;
import ro.expensesplitter.entity.GroupMember;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMemberRepository;
import ro.expensesplitter.repository.GroupRepository;
import ro.expensesplitter.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupServiceTest {

    private GroupRepository groupRepository;
    private GroupMemberRepository groupMemberRepository;
    private UserRepository userRepository;
    private GroupService groupService;

    private User requester;
    private User newUser;
    private Group group;

    @BeforeEach
    void setup() {
        groupRepository = mock(GroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        userRepository = mock(UserRepository.class);
        groupService = new GroupService(groupRepository, groupMemberRepository, userRepository);

        requester = new User("Andrei", "andrei@gmail.com", "hash");
        ReflectionTestUtils.setField(requester, "id", 1L);
        newUser = new User("Ana", "ana@gmail.com", "hash");
        ReflectionTestUtils.setField(newUser, "id", 2L);

        group = Group.builder().id(10L).name("Trip").description("desc").createdBy(requester).build();
    }

    private ResponseStatusException assertAddMemberFails(HttpStatus expected) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> groupService.addMember(10L, new AddMemberRequest("ana@gmail.com"), "andrei@gmail.com"));
        assertEquals(expected, ex.getStatusCode());
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
        return ex;
    }

    // ---------- createGroup ----------

    @Test
    void createGroupSavesGroupAndRegistersCreatorAsMember() {
        when(userRepository.findByEmail("andrei@gmail.com")).thenReturn(Optional.of(requester));
        when(groupRepository.save(any(Group.class))).thenReturn(group);

        GroupResponse response = groupService.createGroup(new CreateGroupRequest("  Trip  ", "desc"), "andrei@gmail.com");

        assertEquals(10L, response.id());
        assertEquals(1L, response.createdById());
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    // ---------- isUserMember ----------

    @Test
    void isUserMemberDelegatesToRepository() {
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);

        assertTrue(groupService.isUserMember(10L, 1L));
        assertFalse(groupService.isUserMember(10L, 2L));
    }

    // ---------- getMyGroups ----------

    @Test
    void getMyGroupsReturnsMappedResponses() {
        when(userRepository.findByEmail("andrei@gmail.com")).thenReturn(Optional.of(requester));
        when(groupMemberRepository.findGroupsByUserId(1L)).thenReturn(List.of(group));

        List<GroupResponse> result = groupService.getMyGroups("andrei@gmail.com");

        assertEquals(1, result.size());
        assertEquals(new GroupResponse(10L, "Trip", "desc", 1L), result.get(0));
    }

    @Test
    void getMyGroupsUnknownUserReturns404() {
        when(userRepository.findByEmail("x@gmail.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> groupService.getMyGroups("x@gmail.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ---------- addMember ----------

    @Test
    void addMemberSuccessSavesMembership() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("andrei@gmail.com")).thenReturn(Optional.of(requester));
        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(newUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);

        groupService.addMember(10L, new AddMemberRequest("ana@gmail.com"), "andrei@gmail.com");

        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    void addMemberGroupNotFoundReturns404() {
        when(groupRepository.findById(10L)).thenReturn(Optional.empty());

        assertAddMemberFails(HttpStatus.NOT_FOUND);
    }

    @Test
    void addMemberRequesterNotMemberReturns403() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("andrei@gmail.com")).thenReturn(Optional.of(requester));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(false);

        assertAddMemberFails(HttpStatus.FORBIDDEN);
    }

    @Test
    void addMemberUnknownNewUserReturns404() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("andrei@gmail.com")).thenReturn(Optional.of(requester));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.empty());

        assertAddMemberFails(HttpStatus.NOT_FOUND);
    }

    @Test
    void addMemberAlreadyMemberReturns409() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("andrei@gmail.com")).thenReturn(Optional.of(requester));
        when(userRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(newUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(true);

        assertAddMemberFails(HttpStatus.CONFLICT);
    }
}
