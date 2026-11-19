package ro.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(max = 100) String name) {
    public UpdateProfileRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
