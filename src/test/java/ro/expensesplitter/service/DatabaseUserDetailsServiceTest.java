package ro.expensesplitter.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseUserDetailsServiceTest {

    @Test
    void loadsExstingUserForAuthentication() {
        UserRepository repository = mock(UserRepository.class);

        User user= new User("Ana","ana@test.com","password-hash");

        when(repository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        DatabaseUserDetailsService service = new DatabaseUserDetailsService(repository);

        UserDetails details= service.loadUserByUsername("ana@test.com");

        assertEquals("ana@test.com", details.getUsername());
        assertEquals("password-hash", details.getPassword());

        assertTrue(
                details.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_USER"))
        );
    }

    @Test
    void rejectsUnknownUser() {
        UserRepository repository = mock(UserRepository.class);

        when(repository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        DatabaseUserDetailsService service = new DatabaseUserDetailsService(repository);

        assertThrows(UsernameNotFoundException.class, ()->service.loadUserByUsername("unknown@test.com"));
    }

    @Test
    void normalisesEmailBeforeLoadingUse() {
        UserRepository repository = mock(UserRepository.class);

        User user= new User("Ana","ana@test.com","password");

        when(repository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        DatabaseUserDetailsService service = new DatabaseUserDetailsService(repository);

        UserDetails userDetails = service.loadUserByUsername("ANA@test.com");

        assertEquals("ana@test.com",userDetails.getUsername());
    }

}
