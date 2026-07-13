package com.ericbouchut.learndev.common.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
// Enables @PreAuthorize for service-level rules (e.g. course ownership).
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // The ERROR dispatch renders templates/error/*.html for a
                // response already authorized (or denied) on its way in.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/", "/privacy", "/auth/**", "/css/**", "/js/**", "/fonts/**").permitAll()
                .requestMatchers("/instructor/**").hasRole("INSTRUCTOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/courses/**").authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/auth/login")            // GET: show the login form
                .loginProcessingUrl("/auth/login")   // POST: Spring Security processes the login
                .defaultSuccessUrl("/dashboard", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
                .permitAll());
        // CSRF protection is ON by default; Thymeleaf adds the token to forms automatically.
        return http.build();
    }
}
