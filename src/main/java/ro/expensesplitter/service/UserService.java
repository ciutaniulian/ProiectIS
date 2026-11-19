package ro.expensesplitter.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.RegisterRequest;
import ro.expensesplitter.dto.UpdateProfileRequest;
import ro.expensesplitter.dto.UserResponse;
import ro.expensesplitter.entity.Role;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.UserRepository;

import java.util.Locale;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request){
        String name = request.name().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user= new User(name, email, passwordHash);

        try {
            userRepository.saveAndFlush(user);
        }
        catch (DataIntegrityViolationException exception)
        {
            if(userRepository.existsByEmail(email))
            {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            throw exception;
        }
        return toResponse(user);
    }

    public UserResponse getProfile(String email){
        return toResponse(findUserByEmail(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);
        user.rename(request.name());
        return toResponse(user);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
