package ro.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendMessageRequest(
        @NotBlank @Size(max = 100) String message,
        UUID requestId
) {
    public SendMessageRequest(String message) {
        this(message, null);
    }
}
