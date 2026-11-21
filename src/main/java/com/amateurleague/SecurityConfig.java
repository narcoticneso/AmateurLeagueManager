package com.amateurleague;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = { "/", "/index.html", "/favicon.svg", "/assets/**", "/api/state", "/api/session", "/api/register", "/api/verify", "/v3/api-docs", "/error" };

    @Bean
    UserDetailsService users(JdbcTemplate db) {
        return username ->
            db
                .query(
                    "SELECT email, password_hash FROM users WHERE email = ?",
                    (rs, row) ->
                        User.withUsername(rs.getString("email"))
                            .password("{bcrypt}" + rs.getString("password_hash"))
                            .build(),
                    username.trim().toLowerCase(Locale.ROOT)
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, SecurityContextRepository repository) throws Exception {
        return http
            .securityContext(context -> context.securityContextRepository(repository))
            .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
            .formLogin(login ->
                login
                    .loginProcessingUrl("/api/login")
                    .successHandler((request, response, auth) -> response.setStatus(204))
                    .failureHandler((request, response, error) -> response.sendError(401, "Invalid credentials"))
                    .permitAll()
            )
            .logout(logout -> logout.logoutUrl("/api/logout").logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .build();
    }
}
