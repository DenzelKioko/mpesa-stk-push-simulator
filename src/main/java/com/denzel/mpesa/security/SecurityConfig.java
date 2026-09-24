package com.denzel.mpesa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * The real Daraja API uses OAuth 2.0 — you POST your consumer key:secret
 * (Base64-encoded) to get a bearer token, then use that for STK calls.
 *
 * This simulator simplifies to HTTP Basic Auth with those same credentials,
 * which lets you demonstrate understanding of the auth pattern without
 * implementing a full OAuth server.
 *
 * In a production extension, replace this with a /oauth/v1/generate endpoint
 * that returns a bearer token and add BearerTokenAuthenticationFilter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${mpesa.auth.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.auth.consumer-secret}")
    private String consumerSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // H2 console open for dev visibility
                .requestMatchers("/h2-console/**").permitAll()
                // Health check open (handy for demos)
                .requestMatchers("/api/mpesa/health").permitAll()
                // Everything else requires Basic Auth
                .anyRequest().authenticated()
            )
            // Allow H2 console frames
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()))
            .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        // consumer key = username, consumer secret = password
        var user = User.builder()
                .username(consumerKey)
                .password(encoder.encode(consumerSecret))
                .roles("API_USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
