package ro.expensesplitter.controller;

import com.jayway.jsonpath.JsonPath;
import com.mysql.cj.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.expensesplitter.config.PasswordConfig;
import ro.expensesplitter.config.SecurityConfig;
import ro.expensesplitter.dto.UserResponse;
import ro.expensesplitter.entity.Role;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.UserRepository;
import ro.expensesplitter.service.DatabaseUserDetailsService;
import ro.expensesplitter.service.UserService;

import javax.security.auth.login.LoginContext;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, CsrfController.class})
@Import({
        SecurityConfig.class,
        PasswordConfig.class,
        DatabaseUserDetailsService.class
        })
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "ana@test.com", roles = "USER")
    void authenticatedUserCanAccessProfile() throws Exception {
        UserResponse profile=new UserResponse(1L,"Ana","ana@test.com", Role.USER);

        when(userService.getProfile("ana@test.com")).thenReturn(profile);

        mockMvc.perform(get("/api/auth/me")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void unauthenticatedUserCannotAccessProfile() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCanRegister() throws Exception{
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Ana",
                            "email": "ana@test.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void registerWithoutCsrfFails() throws Exception{
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "name": "Ana",
                            "email": "ana@test.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanLoginWithCorrectCredentials() throws Exception{
        String password="password123";

        User user= new User("Ana","ana@test.com",passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .param("username","ana@test.com")
                .param("password",password))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("ana@test.com"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception{
        String password="password123";
        User user= new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .param("username", "ana@test.com")
                .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void loggedInUserCanAccessProfileUsingSession() throws Exception{
        String password="password123";
        User user=new User("Ana","ana@test.com",passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").with(csrf())
                                        .param("username","ana@test.com")
                                        .param("password",password))
                                        .andExpect(status().isOk())
                                        .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

       UserResponse profile = new UserResponse(1L,"Ana","ana@test.com", Role.USER);

       when(userService.getProfile("ana@test.com")).thenReturn(profile);

       mockMvc.perform(get("/api/auth/me").session(session))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("ana@test.com"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "ana@test.com")
                        .param("password", password))
                        .andExpect(status().isOk())
                        .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    @Test
    void loginWithoutCsrfIsRejected() throws Exception{
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                .param("username","ana@test.com")
                .param("password",password))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    void logoutWithoutCsrfIsRejected() throws Exception{
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "ana@test.com")
                        .param("password", password))
                        .andExpect(status().isOk())
                        .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithInvalidCsrfIsRejected() throws Exception{
        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login").with(csrf().useInvalidToken())
                        .param("username", "ana@test.com")
                        .param("password", password))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanLoginAndLogoutWithCsrfTokenFromEndpoint() throws Exception{

        String password = "password123";
        User user = new User("Ana", "ana@test.com", passwordEncoder.encode(password));
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

        assertNotNull(session);

        String csrfToken = csrfResult.getResponse().getContentAsString();

        String token = JsonPath.read(csrfToken,"$.token");
        String headerName = JsonPath.read(csrfToken,"$.headerName");

        MvcResult loginResult=mockMvc.perform(post("/api/auth/login").session(session).header(headerName,token)
                .param("username","ana@test.com")
                .param("password",password))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("ana@test.com"))
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        MvcResult authenticatedCsrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("ana@test.com"))
                .andReturn();

        String authenticatedCsrfBody =
                authenticatedCsrfResult.getResponse().getContentAsString();
        token = JsonPath.read(authenticatedCsrfBody, "$.token");
        headerName = JsonPath.read(authenticatedCsrfBody, "$.headerName");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header(headerName, token))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

}
