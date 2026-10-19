package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ro.expensesplitter.dto.RegisterRequest;
import ro.expensesplitter.dto.UserResponse;
import ro.expensesplitter.service.UserService;

@RestController
@RequestMapping( "/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName();
        return userService.getProfile(email);
    }

}
