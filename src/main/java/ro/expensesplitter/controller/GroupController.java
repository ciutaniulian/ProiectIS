package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ro.expensesplitter.dto.AddMemberRequest;
import ro.expensesplitter.dto.CreateGroupRequest;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.dto.MemberResponse;
import ro.expensesplitter.service.GroupService;

import java.util.List;

@RestController
@RequestMapping(value="/api/groups")
@RequiredArgsConstructor

public class GroupController {
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody @Valid CreateGroupRequest request, Authentication authentication) {
        GroupResponse groupResponse=groupService.createGroup(request,authentication.getName());
        return new ResponseEntity<>(groupResponse, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(Authentication authentication)
    {
        List<GroupResponse> groupResponses=groupService.getMyGroups(authentication.getName());
        return ResponseEntity.ok(groupResponses);
    }

    @PostMapping(value ="/{groupId}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long groupId, @RequestBody @Valid AddMemberRequest request, Authentication authentication){
        groupService.addMember(groupId,request,authentication.getName());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<MemberResponse>> getGroupMembers(@PathVariable Long groupId, Authentication authentication){
        List<MemberResponse> memberResponses= groupService.getGroupMembers(groupId,authentication.getName());
        return ResponseEntity.ok(memberResponses);
    }
}
