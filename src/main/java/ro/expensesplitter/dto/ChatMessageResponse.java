package ro.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        @NotNull Long messageId,
        @NotNull Long groupId,
        @NotNull Long senderId,
        @NotBlank String senderName,
        @NotBlank String message,
        @NotNull LocalDateTime createdAt
        ) {
}
