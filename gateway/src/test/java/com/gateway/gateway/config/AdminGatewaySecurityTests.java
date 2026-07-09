package com.gateway.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "gateway.trusted-secret=test-gateway-secret",
        "AUTH_SERVICE_URL=http://127.0.0.1:1",
        "QUESTION_SERVICE_URL=http://127.0.0.1:1",
        "EXAM_SERVICE_URL=http://127.0.0.1:1",
        "EXAMRUNTIME_SERVICE_URL=http://127.0.0.1:1"
})
class AdminGatewaySecurityTests {

    private static final String SECRET = "01234567890123456789012345678901";

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .baseUrl("http://localhost")
                .build();
    }

    @Test
    void adminRoutesRejectUnauthenticatedAndTeacherRequests() {
        for (String path : adminPaths()) {
            client.get().uri(path)
                    .exchange()
                    .expectStatus().isUnauthorized();

            client.get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("TEACHER"))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Test
    void adminTokenPassesGatewayAuthorization() {
        for (String path : adminPaths()) {
            client.get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))
                    .exchange()
                    .expectStatus()
                    .value(status -> assertThat(status).isNotIn(401, 403));
        }
    }

    @Test
    void corsPreflightAllowsPatchForAdminApis() {
        client.options()
                .uri("/v1/api/admin/auth-service/users/user-id/status")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        value -> assertThat(value).contains("PATCH")
                );
    }

    @Test
    void studentRoutesAllowedForAuthenticatedUsers() {
        // Kiem tra route student cho phep request co JWT hop le di qua gateway
        client.get().uri("/v1/api/examruntime-service/student/exams/123/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("STUDENT"))
                .exchange()
                .expectStatus()
                .value(status -> assertThat(status).isNotIn(401, 403));
    }

    @Test
    void studentRoutesRejectUnauthenticatedRequests() {
        // Kiem tra route student chan request khong co JWT o gateway
        client.get().uri("/v1/api/examruntime-service/student/exams/123/join")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void monitorWebSocketRouteAcceptsAccessTokenQueryToken() {
        client.get().uri("/v1/api/examruntime-service/ws?access_token=" + token("STUDENT"))
                .exchange()
                .expectStatus()
                .value(status -> assertThat(status).isNotIn(401, 403));
    }

    @Test
    void accessTokenQueryIsIgnoredOutsideMonitorWebSocketRoute() {
        client.get().uri("/v1/api/examruntime-service/student/exams/123/join?access_token=" + token("STUDENT"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void publicRuntimeRouteAllowsUnauthenticatedRequests() {
        // Kiem tra route public cua examruntime cho phep request khong co JWT di qua
        client.get().uri("/v1/api/examruntime-service/public/status")
                .exchange()
                .expectStatus()
                .value(status -> assertThat(status).isNotIn(401, 403));
    }

    private String[] adminPaths() {
        return new String[]{
                "/v1/api/admin/auth-service/users",
                "/v1/api/admin/question-service/subjects",
                "/v1/api/admin/exam-service/health"
        };
    }

    @SuppressWarnings("deprecation")
    private String token(String role) {
        return Jwts.builder()
                .setSubject(role.toLowerCase())
                .claim("userRole", role)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(
                        Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }
}
