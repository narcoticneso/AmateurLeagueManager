package com.amateurleague;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/",
        "/index.html",
        "/favicon.svg",
        "/assets/**",
        "/api/state",
        "/api/session",
        "/api/register",
        "/api/verify",
        "/v3/api-docs",
        "/error"
    };

    @Bean
    UserDetailsService users(Environment env, JdbcTemplate db) {
        var developers = new InMemoryUserDetailsManager();
        var encoder = new BCryptPasswordEncoder();
        for (String name : new String[] { "rudra", "ryan", "lucas", "nebojsa", "kiara" }) {
            String password = env.getProperty("ALM_PASSWORD_" + name.toUpperCase(java.util.Locale.ROOT), "neighborhood-demo");
            developers.createUser(User.withUsername(name)
                .password("{bcrypt}" + encoder.encode(password))
                .roles("COMMISSIONER")
                .build());
        }
        return username -> {
            try {
                return developers.loadUserByUsername(username);
            } catch (UsernameNotFoundException missingDeveloper) {
                return db.queryForObject(
                    "SELECT email, password_hash, account_role FROM users WHERE email = ?",
                    (rs, row) -> User.withUsername(rs.getString("email"))
                        .password("{bcrypt}" + rs.getString("password_hash"))
                        .roles(rs.getString("account_role"))
                        .build(),
                    username.trim().toLowerCase(java.util.Locale.ROOT));
            }
        };
    }
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        SecurityContextRepository repository
    ) throws Exception {
        return http
            .securityContext(context ->
                context.securityContextRepository(repository)
            )
            .authorizeHttpRequests(auth ->
                auth.requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .formLogin(login ->
                login
                    .loginProcessingUrl("/api/login")
                    .successHandler((request, response, auth) -> {
                        request.getSession().setAttribute("view",
                            auth.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_COMMISSIONER"))
                                ? "COMMISSIONER" : "PLAYER");
                        response.setStatus(204);
                    })
                    .failureHandler((request, response, error) -> response.sendError(401, "Invalid credentials"))
                    .permitAll()
            )
            .logout(logout ->
                logout
                    .logoutUrl("/api/logout")
                    .logoutSuccessHandler((request, response, auth) -> response.setStatus(204))
            )
            .exceptionHandling(errors ->
                errors.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            )
            .build();
    }
}
