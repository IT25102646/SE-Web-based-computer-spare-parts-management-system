package com.comspare.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs at startup. Makes sure the six roles exist (using the SAME names as ComSpareDB.sql, so no
 * duplicates are created) and - only if the users table is empty - creates demo accounts.
 *
 *   Admin login : admin@comspare.lk / Admin@1234
 *   Demo logins : <role>@comspare.lk / Demo@1234
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final List<String> ROLES = List.of(
            "ADMIN", "OPERATIONS_MANAGER", "INVENTORY_SUPERVISOR",
            "STORE_KEEPER", "CUSTOMER_SERVICE_EXECUTIVE", "FINANCE_OFFICER");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           PasswordEncoder encoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        for (String name : ROLES) {
            if (roleRepository.findByRoleName(name).isEmpty()) {
                roleRepository.save(new Role(name));
            }
        }
        if (userRepository.count() > 0) return;

        create("System Admin", "admin@comspare.lk", "Admin@1234", "ADMIN");
        create("Nimal Perera",   "manager@comspare.lk",     "Demo@1234", "OPERATIONS_MANAGER");
        create("Sunil Fernando", "supervisor@comspare.lk",  "Demo@1234", "INVENTORY_SUPERVISOR");
        create("Kasun Silva",    "storekeeper@comspare.lk", "Demo@1234", "STORE_KEEPER");
        create("Tharushi Jayasinghe", "service@comspare.lk","Demo@1234", "CUSTOMER_SERVICE_EXECUTIVE");
        create("Ramesh Kumar",   "finance@comspare.lk",     "Demo@1234", "FINANCE_OFFICER");
    }

    private void create(String name, String email, String rawPassword, String roleName) {
        Role role = roleRepository.findByRoleName(roleName).orElseThrow();
        userRepository.save(new User(name, email, encoder.encode(rawPassword), role));
    }
}
