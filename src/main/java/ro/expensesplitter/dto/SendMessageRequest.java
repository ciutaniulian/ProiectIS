package ro.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ro.expensesplitter.entity.User;

public record SendMessageRequest(
        @NotBlank @Size(max = 100) String message
) {
}
