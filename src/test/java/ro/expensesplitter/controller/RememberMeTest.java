package ro.expensesplitter.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.expensesplitter.config.SecurityConfig;
import ro.expensesplitter.dto.UserResponse;
import ro.expensesplitter.entity.Role;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.UserRepository;
import ro.expensesplitter.service.DatabaseUserDetailsService;
import ro.expensesplitter.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        DatabaseUserDetailsService.class
})
public class RememberMeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginWithRememberMeIssuesCookie() throws Exception {
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .param("username","ana@test.com")
                .param("password",password)
                .param("remember-me","true"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("remember-me");

        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    void cookieAuthenticatesWithoutPreviousSession() throws Exception
    {
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .param("username","ana@test.com")
                .param("password",password)
                .param("remember-me","true"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("remember-me");

        assertNotNull(cookie);

        UserResponse profile = new UserResponse(1L, "Ana", "ana@test.com", Role.USER);

        when(userService.getProfile("ana@test.com")).thenReturn(profile);

        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("ana@test.com"));
    }

    @Test
    void loginWithoutRememberMeDoesNotIssuePersistentCookie() throws Exception{
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username","ana@test.com")
                        .param("password",password))
                        .andExpect(status().isOk())
                        .andReturn();
        Cookie cookie = result.getResponse().getCookie("remember-me");

        assertNull(cookie);
    }
}
