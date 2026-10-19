package ro.expensesplitter.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.UserRepository;

import java.util.Locale;


@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository repository;
    public DatabaseUserDetailsService(UserRepository repository) {
        this.repository=repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail=email.strip().toLowerCase(Locale.ROOT);
        User user= repository.findByEmail(normalizedEmail).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                   .password(user.getPasswordHash())
                   .roles(user.getRole().name())
                   .build();
    }
}
