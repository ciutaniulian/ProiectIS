package ro.expensesplitter.dto;

import ro.expensesplitter.entity.GroupRole;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String name,
        String email,
        Instant joinedAt,
        GroupRole role
) {
}
