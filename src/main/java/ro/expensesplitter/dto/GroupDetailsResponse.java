package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupInvitation;

import java.time.Instant;
import java.util.List;

public record GroupDetailsResponse(GroupResponse group, List<Member> members, List<Invitation> invitations) {
    public record Member(Long userId, String name) {}
    public record Invitation(Long id, String email, GroupInvitation.Status status, Instant expiresAt) {}
}
