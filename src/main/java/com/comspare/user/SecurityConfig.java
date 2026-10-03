package com.comspare.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Where non-admin users land after login. CHANGE to your inventory list URL. */
    private static final String HOME_URL = "/parts";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // one-way salted hash
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/access-denied", "/css/**", "/js/**", "/images/**").permitAll()

                        // ---- User & Role Management: Admin only (enforced on the SERVER) ----
                        .requestMatchers("/users/**", "/audit/**").hasRole("ADMIN")

                        // ---- OPTIONAL: apply the role matrix to the other modules ----
                        // Adjust the URL patterns to match your teammates' controllers, e.g.:
                        // .requestMatchers("/parts/**", "/stock-adjustments/**")
                        //     .hasAnyRole("ADMIN", "INVENTORY_SUPERVISOR", "STORE_KEEPER",
                        //                 "OPERATIONS_MANAGER", "CUSTOMER_SERVICE_EXECUTIVE", "FINANCE_OFFICER")
                        // .requestMatchers("/reports/finance/**").hasAnyRole("ADMIN", "FINANCE_OFFICER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            boolean admin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                            response.sendRedirect(request.getContextPath() + (admin ? "/users" : HOME_URL));
                        })
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")                 // POST (CSRF-protected)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"));

        return http.build();
    }
}