package com.sep490.backendclubmanagement.security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;


@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    // System Role Constants (with ROLE_ prefix as stored in authorities)
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_STUDENT = "ROLE_STUDENT";

    // Authority strings for @PreAuthorize and @Secured annotations
    // Use hasRole() which automatically adds ROLE_ prefix, or hasAuthority() with full name
    public static final String AUTHORITY_ADMIN = "hasRole('ADMIN')";
    public static final String AUTHORITY_STAFF = "hasRole('STAFF')";
    public static final String AUTHORITY_STUDENT = "hasRole('STUDENT')";

    public static final String AUTHORITY_ADMIN_OR_STAFF = "hasAnyRole('ADMIN', 'STAFF')";
    public static final String AUTHORITY_ALL_ROLES = "hasAnyRole('ADMIN', 'STAFF', 'STUDENT')";

    // Only truly public endpoints (no authentication required)
    private final String[] PUBLIC_URL = {
            "/api/auth/google",
            "/api/auth/refreshToken",
            "/api/v1/public/**",
            "/api/homepage",
            "/test/**",
            "/posts/**",
            "/api/my-club/**",
            "/api/management/**",
            "/api/posts/**",
            "/api/comments/**",
            "/api/posts/**",
            "/api/clubs/**",
            "/api/events/get-all-by-filter",
            "/api/events/get-all-event-types",
            "/api/events/{id}",
            "/api/events/get-all-club",
            "/api/news/get-all-by-filter",
            "/api/news/{id}",
            "/api/public/clubs/**",
            "/api/pay-os/webhook",
            "/api/pay-os/webhook",
            "/ws/**",
            "/api/test/**",
            "/api/admin-departments/**",
            "/api/profile/**",
            "/api/recruitments/clubs/{id}/open",
            "api/news/clubs/{clubId}/published",
            "/api/events/clubs/{clubId}/published",
            "/api/clubInfo/{id}/teams/dto",
            "/api/clubInfo/{id}",
            "/api/clubInfo/code/{clubCode}",
            "/api/clubInfo",

    };

    private final String[] SWAGGER_URL = {
            "/v2/api-docs",
            "/v3/api-docs",
            "v3/api-docs/**",
            "/swagger-ui/**",
            "/configuration/ui",
            "/swagger-resources/**",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(SWAGGER_URL).permitAll()
                        .requestMatchers(PUBLIC_URL).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/banner").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
