package ro.expensesplitter.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthHttpIntegrationTest {
    @LocalServerPort
    private int port;

    private CookieManager cookies;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Test
    void duplicateRegistrationPreservesConflictThroughErrorDispatch() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        assertEquals(201, post("register", registration(email), "application/json").statusCode());
        assertEquals(409, post("register", registration(email), "application/json").statusCode());
    }

    @Test
    void invalidRegistrationPreservesBadRequestThroughErrorDispatch() throws Exception {
        assertEquals(400, post("register", registration("invalid-email"), "application/json").statusCode());
    }

    @Test
    void anonymousProfileIsStillUnauthorized() throws Exception {
        assertEquals(401, get("me").statusCode());
    }

    @Test
    void registrationWithoutCsrfIsStillForbidden() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint("register"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(registration(UUID.randomUUID() + "@example.com")))
                .build();
        assertEquals(403, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void rememberMeRestoresSessionAndLogoutRevokesAuthentication() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        assertEquals(201, post("register", registration(email), "application/json").statusCode());
        assertEquals(200, post("login", "username=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=Integration123!&remember-me=true", "application/x-www-form-urlencoded").statusCode());
        assertTrue(cookies.getCookieStore().getCookies().stream()
                .anyMatch(cookie -> cookie.getName().equals("remember-me") && cookie.getMaxAge() > 0));

        for (var cookie : cookies.getCookieStore().getCookies()) {
            if (cookie.getName().equals("JSESSIONID")) cookies.getCookieStore().remove(endpoint("me"), cookie);
        }
        assertTrue(cookies.getCookieStore().getCookies().stream()
                .noneMatch(cookie -> cookie.getName().equals("JSESSIONID")));
        HttpResponse<String> profile = get("me");
        assertEquals(200, profile.statusCode());
        assertEquals(email, JsonPath.<String>read(profile.body(), "$.email"));

        assertEquals(204, post("logout", "", "application/x-www-form-urlencoded").statusCode());
        assertEquals(401, get("me").statusCode());
        assertTrue(cookies.getCookieStore().getCookies().stream()
                .noneMatch(cookie -> cookie.getName().equals("remember-me")));
    }

    private URI endpoint(String path) {
        return URI.create("http://localhost:" + port + "/api/auth/" + path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(endpoint(path)).timeout(Duration.ofSeconds(10))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String contentType) throws Exception {
        HttpResponse<String> csrf = get("csrf");
        assertEquals(200, csrf.statusCode());
        String header = JsonPath.read(csrf.body(), "$.headerName");
        String token = JsonPath.read(csrf.body(), "$.token");
        HttpRequest request = HttpRequest.newBuilder(endpoint(path))
                .timeout(Duration.ofSeconds(10))
                .header(header, token).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String registration(String email) {
        return """
                {"name":"Integration User","email":"%s","password":"Integration123!"}
                """.formatted(email);
    }
}
