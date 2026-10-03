package com.comspare.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs at startup. Creates the six roles, and - only if the users table is empty -
 * an Admin plus one demo account per role so you can demo RBAC immediately.
 *
 *   Admin login : admin@comspare.lk / Admin@1234
 *   Demo logins : <role>@comspare.lk / Demo@1234
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final List<String> ROLES = List.of(
            "Admin", "Operations Manager", "Inventory Supervisor",
            "Store Keeper", "Customer Service Executive", "Finance Officer");

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

        create("System Admin", "admin@comspare.lk", "Admin@1234", "Admin");
        create("Nimal Perera",   "manager@comspare.lk",    "Demo@1234", "Operations Manager");
        create("Sunil Fernando", "supervisor@comspare.lk", "Demo@1234", "Inventory Supervisor");
        create("Kasun Silva",    "storekeeper@comspare.lk","Demo@1234", "Store Keeper");
        create("Tharushi Jayasinghe", "service@comspare.lk","Demo@1234", "Customer Service Executive");
        create("Ramesh Kumar",   "finance@comspare.lk",    "Demo@1234", "Finance Officer");
    }

    private void create(String name, String email, String rawPassword, String roleName) {
        Role role = roleRepository.findByRoleName(roleName).orElseThrow();
        userRepository.save(new User(name, email, encoder.encode(rawPassword), role));
    }
}