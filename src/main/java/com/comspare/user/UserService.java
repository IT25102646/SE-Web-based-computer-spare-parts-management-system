package com.comspare.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ---------- create ----------
    @Transactional
    public User createUser(User user, Long roleId) {
        requireNameAndEmail(user);
        checkPassword(user.getPasswordHash());
        user.setEmail(user.getEmail().trim().toLowerCase());
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        user.setRole(roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found")));
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setActive(true);
        return userRepository.save(user);
    }

    // ---------- update ----------
    @Transactional
    public User updateUser(Long id, User updated, Long roleId) {
        User existing = getUserById(id);
        requireNameAndEmail(updated);

        String email = updated.getEmail().trim().toLowerCase();
        Optional<User> clash = userRepository.findByEmail(email);
        if (clash.isPresent() && !clash.get().getId().equals(id)) {
            throw new IllegalArgumentException("Email already exists.");
        }

        Role newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        boolean demotingAdmin = isAdmin(existing) && existing.isActive()
                && !"ADMIN".equalsIgnoreCase(newRole.getRoleName());
        if (demotingAdmin && userRepository.countByRoleRoleNameIgnoreCaseAndActiveTrue("ADMIN") <= 1) {
            throw new IllegalStateException("The last active ADMIN cannot be changed to another role.");
        }

        existing.setName(updated.getName().trim());
        existing.setEmail(email);
        existing.setRole(newRole);

        String password = updated.getPasswordHash();
        if (password != null && !password.isBlank()) {
            checkPassword(password);
            existing.setPasswordHash(passwordEncoder.encode(password));
        }
        return userRepository.save(existing);
    }

    // ---------- delete = deactivate (soft delete) ----------
    @Transactional
    public User deactivateUser(Long id, String currentUserEmail) {
        User user = getUserById(id);
        if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new IllegalStateException("You cannot deactivate your own account.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("This account is already deactivated.");
        }
        if (isAdmin(user) && userRepository.countByRoleRoleNameIgnoreCaseAndActiveTrue("ADMIN") <= 1) {
            throw new IllegalStateException("The last active ADMIN cannot be deactivated.");
        }
        user.setActive(false);
        return userRepository.save(user);
    }

    @Transactional
    public User reactivateUser(Long id) {
        User user = getUserById(id);
        user.setActive(true);
        return userRepository.save(user);
    }

    // ---------- own password ----------
    @Transactional
    public void changePassword(String email, String current, String next, String confirm) {
        User user = findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (current == null || !passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (next == null || !next.equals(confirm)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
        checkPassword(next);
        user.setPasswordHash(passwordEncoder.encode(next));
        userRepository.save(user);
    }

    // ---------- helpers ----------
    private boolean isAdmin(User u) {
        return "ADMIN".equalsIgnoreCase(u.getRole().getRoleName());
    }

    private void checkPassword(String p) {
        if (p == null || p.length() < 8 || !p.matches(".*[A-Za-z].*") || !p.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must be at least 8 characters with a letter and a number.");
        }
    }

    private void requireNameAndEmail(User u) {
        if (u.getName() == null || u.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (u.getEmail() == null || u.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
    }
}

