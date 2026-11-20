package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.expensesplitter.dto.GroupActivityResponse;
import ro.expensesplitter.dto.GroupDetailsResponse;
import ro.expensesplitter.dto.GroupInvitationResponse;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.service.GroupOperationException;
import ro.expensesplitter.service.GroupService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class GroupController {
    private final GroupService groups;

    public GroupController(GroupService groups) {
        this.groups = groups;
    }

    @GetMapping("/groups")
    public List<GroupResponse> list(Principal principal) {
        return groups.list(principal.getName());
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody GroupDetailsRequest request, Principal principal) {
        return groups.create(request.name(), request.description(), principal.getName());
    }

    @GetMapping("/groups/{id}")
    public GroupDetailsResponse details(@PathVariable Long id, Principal principal) {
        return groups.details(id, principal.getName());
    }

    @PatchMapping("/groups/{id}")
    public GroupDetailsResponse edit(@PathVariable Long id, @Valid @RequestBody GroupDetailsRequest request,
                                     Principal principal) {
        return groups.update(id, request.name(), request.description(), principal.getName());
    }

    @DeleteMapping("/groups/{id}/members/{userId}")
    public GroupDetailsResponse removeMember(@PathVariable Long id, @PathVariable Long userId, Principal principal) {
        return groups.removeMember(id, userId, principal.getName());
    }

    @PostMapping("/groups/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, @RequestBody(required = false) LeaveRequest request, Principal principal) {
        groups.leave(id, request != null && request.confirmClose(), principal.getName());
    }

    @PostMapping("/groups/{id}/archive")
    public GroupDetailsResponse archive(@PathVariable Long id, Principal principal) {
        return groups.archive(id, principal.getName());
    }

    @PostMapping("/groups/{id}/restore")
    public GroupDetailsResponse restore(@PathVariable Long id, Principal principal) {
        return groups.restore(id, principal.getName());
    }

    @GetMapping("/groups/{id}/activity")
    public GroupActivityResponse activity(@PathVariable Long id,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                          Principal principal) {
        return groups.activity(id, principal.getName(), page, size);
    }

    @PostMapping("/groups/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupInvitationResponse.Created invite(@PathVariable Long id, @Valid @RequestBody InviteRequest request,
                                                  Principal principal) {
        return groups.invite(id, request.email(), principal.getName());
    }

    @DeleteMapping("/groups/{id}/invitations/{invitationId}")
    public GroupDetailsResponse revoke(@PathVariable Long id, @PathVariable Long invitationId, Principal principal) {
        return groups.revoke(id, invitationId, principal.getName());
    }

    @GetMapping("/group-invitations")
    public List<GroupInvitationResponse.Received> inbox(Principal principal) {
        return groups.inbox(principal.getName());
    }

    @GetMapping("/group-invitations/link/{token}")
    public GroupInvitationResponse.Received invitationByLink(@PathVariable String token, Principal principal) {
        return groups.invitationByLink(token, principal.getName());
    }

    @PostMapping("/group-invitations/{id}/accept")
    public GroupResponse accept(@PathVariable Long id, Principal principal) {
        return groups.accept(id, principal.getName());
    }

    @PostMapping("/group-invitations/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable Long id, Principal principal) {
        groups.decline(id, principal.getName());
    }

    @ExceptionHandler(GroupOperationException.class)
    ResponseEntity<GroupError> groupError(GroupOperationException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new GroupError(exception.getCode(), exception.getReason()));
    }

    public record GroupDetailsRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}

    public record InviteRequest(@NotBlank @Email @Size(max = 100) String email) {
        public InviteRequest {
            if (email != null) {
                email = email.strip();
            }
        }
    }

    public record LeaveRequest(boolean confirmClose) {}

    private record GroupError(String code, String message) {}
}
