package com.comspare.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error").permitAll()

                        .requestMatchers("/users/**", "/roles/**").hasRole("ADMIN")
                        .requestMatchers("/invoices/**").hasAnyRole("ADMIN", "FINANCE_OFFICER")
                        .requestMatchers("/orders/**").hasAnyRole("ADMIN", "CUSTOMER_SERVICE_EXECUTIVE", "OPERATIONS_MANAGER")
                        .requestMatchers("/returns/**").hasAnyRole("ADMIN", "CUSTOMER_SERVICE_EXECUTIVE", "INVENTORY_SUPERVISOR")
                        .requestMatchers("/suppliers/**", "/purchase-orders/**", "/supplier-performance/**")
                        .hasAnyRole("ADMIN", "INVENTORY_SUPERVISOR", "OPERATIONS_MANAGER")
                        .requestMatchers("/parts/**")
                        .hasAnyRole("ADMIN", "INVENTORY_SUPERVISOR", "STORE_KEEPER", "OPERATIONS_MANAGER")
                        .requestMatchers("/reports/**")
                        .hasAnyRole("ADMIN", "OPERATIONS_MANAGER", "FINANCE_OFFICER", "INVENTORY_SUPERVISOR")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)   // home page: every role can open it
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")           // POST only (the navbar uses a form)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}

