package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupInvitation;

import java.time.Instant;

public final class GroupInvitationResponse {
    private GroupInvitationResponse() {}

    public record Received(Long id, Long groupId, String groupName, Instant expiresAt,
                           GroupInvitation.Status status) {}
    public record Created(Long id, String email, GroupInvitation.Status status, Instant expiresAt, String token) {}
}
