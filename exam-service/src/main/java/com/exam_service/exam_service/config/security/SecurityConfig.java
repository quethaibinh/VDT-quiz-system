package com.exam_service.exam_service.config.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
/**
 * Cau hinh trusted-header authentication va phan quyen cho exam-service.
 */
public class SecurityConfig {

    private final ExamHeaderAuthenticationFilter examHeaderAuthenticationFilter;
    private final InternalApiKeyAuthenticationFilter internalApiKeyAuthenticationFilter;

    public SecurityConfig(
            ExamHeaderAuthenticationFilter examHeaderAuthenticationFilter,
            InternalApiKeyAuthenticationFilter internalApiKeyAuthenticationFilter
    ) {
        this.examHeaderAuthenticationFilter = examHeaderAuthenticationFilter;
        this.internalApiKeyAuthenticationFilter = internalApiKeyAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(customizer -> customizer.disable())
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/v1/api/exam-service/public/**").permitAll()
                        .requestMatchers("/v1/internal/**").permitAll()
                        .requestMatchers("/v1/api/exam-service/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/v1/api/exam-service/student/**").hasRole("STUDENT")
                        .requestMatchers("/v1/api/admin/exam-service/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalApiKeyAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .addFilterBefore(examHeaderAuthenticationFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new AccessDeniedHandlerImpl();
    }

    @Bean
    public FilterRegistrationBean<ExamHeaderAuthenticationFilter> questionHeaderFilterRegistration(
            ExamHeaderAuthenticationFilter filter
    ) {
        FilterRegistrationBean<ExamHeaderAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        // Tranh servlet container chay filter them lan thu hai ngoai Spring Security.
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<InternalApiKeyAuthenticationFilter> internalApiKeyFilterRegistration(
            InternalApiKeyAuthenticationFilter filter
    ) {
        FilterRegistrationBean<InternalApiKeyAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

}
