package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ro.expensesplitter.dto.CreateGroupRequest;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.service.GroupService;

@RestController
@RequestMapping(value="api/groups")
@RequiredArgsConstructor

public class GroupController {
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody @Valid CreateGroupRequest request, Authentication authentication) {
        GroupResponse groupResponse=groupService.createGroup(request,authentication.getName());
        return new ResponseEntity<>(groupResponse, HttpStatus.CREATED);
    }
}
