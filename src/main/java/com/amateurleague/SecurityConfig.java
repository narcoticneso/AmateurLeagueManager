package com.amateurleague;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = { "/", "/index.html", "/favicon.svg", "/assets/**", "/api/state", "/api/session", "/v3/api-docs", "/error" };

    @Bean
    UserDetailsService users(Environment env) {
        var users = new InMemoryUserDetailsManager();
        var encoder = new BCryptPasswordEncoder();
        for (String name : new String[] { "rudra", "ryan", "lucas", "nebojsa", "kiara" }) {
            String password = env.getProperty("ALM_PASSWORD_" + name.toUpperCase(Locale.ROOT), "neighborhood-demo");
            users.createUser(
                User.withUsername(name)
                    .password("{bcrypt}" + encoder.encode(password))
                    .roles("DEVELOPER")
                    .build()
            );
        }
        return users;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
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
