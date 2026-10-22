package ro.expensesplitter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.RegisterRequest;
import ro.expensesplitter.dto.UserResponse;
import ro.expensesplitter.entity.Role;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository repository;
    private PasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setup(){
        repository = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new UserService(repository, encoder);
    }

    @Test
    void rejectsRegistratonWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest("Iulian", "iulian@test.com", "password123");

        when(repository.existsByEmail("iulian@test.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, ()->service.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(repository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registersNewAccountWithUserRole(){

        RegisterRequest request = new RegisterRequest("Ana","ana@test.com","password123");

        when(repository.existsByEmail("ana@test.com")).thenReturn(false);

        UserResponse response= service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        verify(repository).saveAndFlush(captor.capture());

        User savedUser=captor.getValue();

        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(Role.USER, response.role());
    }

    @Test
    void storesPasswordHashInsteadOfPlain() {
        String password = "password123";

        RegisterRequest request = new RegisterRequest("Ana","ana@test.com",password);

        when(repository.existsByEmail("ana@test.com")).thenReturn(false);

        service.register(request);

        ArgumentCaptor<User> captor =  ArgumentCaptor.forClass(User.class);
        verify(repository).saveAndFlush(captor.capture());

        String storedHash=captor.getValue().getPasswordHash();

        assertNotEquals(password, storedHash);
        assertTrue(encoder.matches(password, storedHash));
    }

    @Test
    void propagatesUnrelatedDataIntegrityFailure() {
        RegisterRequest request = new RegisterRequest(
                "Ana",
                "ana@test.com",
                "Example-password-2026!"
        );


        when(repository.existsByEmail("ana@test.com")).thenReturn(false);

        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("Simulated database failure");

        when(repository.saveAndFlush(any(User.class)))
                .thenThrow(failure);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.register(request)
        );

        assertSame(failure, thrown);
    }

    @Test
    void reportsConflictWhenEmailBecomesTakenDuringRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Ana",
                "ana@test.com",
                "Example-password-2026!"
        );

        when(repository.existsByEmail("ana@test.com"))
                .thenReturn(false, true);

        when(repository.saveAndFlush(any(User.class)))
                .thenThrow(
                        new DataIntegrityViolationException("Duplicate email")
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
