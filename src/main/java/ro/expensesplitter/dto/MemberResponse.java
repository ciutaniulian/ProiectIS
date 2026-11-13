package ro.expensesplitter.dto;

import java.time.Instant;

public record MemberResponse(
        Long userId,
        String name,
        String email,
        Instant joinedAt
) {
}
