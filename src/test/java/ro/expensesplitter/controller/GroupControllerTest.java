package ro.expensesplitter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import ro.expensesplitter.dto.AddMemberRequest;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.dto.MemberResponse;
import ro.expensesplitter.service.GroupService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupControllerTest {

    private GroupService groupService;
    private Authentication authentication;
    private GroupController controller;

    @BeforeEach
    void setup() {
        groupService = mock(GroupService.class);
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("andrei@gmail.com");
        controller = new GroupController(groupService);
    }

    @Test
    void getMyGroupsReturns200WithGroups() {
        List<GroupResponse> groups = List.of(new GroupResponse(10L, "Trip", "desc", 1L));
        when(groupService.getMyGroups("andrei@gmail.com")).thenReturn(groups);

        ResponseEntity<List<GroupResponse>> response = controller.getMyGroups(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(groups, response.getBody());
    }

    @Test
    void addMemberReturns201WithoutBodyAndUsesAuthenticatedUser() {
        AddMemberRequest request = new AddMemberRequest("ana@gmail.com");

        ResponseEntity<Void> response = controller.addMember(10L, request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
        verify(groupService).addMember(10L, request, "andrei@gmail.com");
    }

    @Test
    void getGroupMembersReturns200WithMembers() {
        List<MemberResponse> members = List.of(
                new MemberResponse(2L, "Ana", "ana@gmail.com", Instant.parse("2026-01-01T10:00:00Z")));
        when(groupService.getGroupMembers(10L, "andrei@gmail.com")).thenReturn(members);

        ResponseEntity<List<MemberResponse>> response = controller.getGroupMembers(10L, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(members, response.getBody());
    }
}
