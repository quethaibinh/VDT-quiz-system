package com.gateway.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    // Dùng constructor injection để Spring tự truyền các bean cần thiết vào.
    public SecurityConfig(
            JwtAuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Gateway dùng JWT nên không cần CSRF/session như form login.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        // Cho phép login/register đi thẳng tới auth-service.
                        .pathMatchers("/v1/api/auth-service/login").permitAll()
                        .pathMatchers("/v1/api/auth-service/register").permitAll()
                        .pathMatchers("/v1/api/question-service/public/**").permitAll()
                        .pathMatchers("/v1/api/admin/**").hasRole("ADMIN")
                        // Các request còn lại bắt buộc phải đăng nhập.
                        .anyExchange().authenticated()
                )
                .build();
    }
}
