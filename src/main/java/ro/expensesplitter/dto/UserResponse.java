package ro.expensesplitter.dto;

import ro.expensesplitter.entity.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
){}
