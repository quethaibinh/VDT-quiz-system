package com.gateway.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

@Configuration
@EnableWebFluxSecurity
/**
 * Cau hinh xac thuc JWT va phan quyen tai cua ngo vao he thong.
 */
public class SecurityConfig {

    private final JwtAuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public SecurityConfig(
            JwtAuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    /**
     * Tao chuoi loc bao mat stateless cho tat ca request di qua gateway.
     */
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Gateway dung JWT stateless nen khong can CSRF nhu form login.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        // Browser CORS preflight must complete before JWT authorization.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Login va register la hai endpoint cong khai.
                        .pathMatchers("/v1/api/auth-service/login").permitAll()
                        .pathMatchers("/v1/api/auth-service/register").permitAll()
                        .pathMatchers("/v1/api/question-service/public/**").permitAll()
                        // Cho phep api public cua examruntime di qua khong can token
                        .pathMatchers("/v1/api/examruntime-service/public/**").permitAll()
                        .pathMatchers("/v1/api/admin/**").hasRole("ADMIN")
                        // Cac request con lai phai co JWT hop le.
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .build();
    }
}
