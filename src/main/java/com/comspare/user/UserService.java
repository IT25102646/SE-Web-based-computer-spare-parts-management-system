package com.comspare.user;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService audit;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, AuditLogService audit) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    // ---------- READ ----------
    public List<User> findAll() {
        return userRepository.findAll(Sort.by("name"));
    }

    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<Role> findRoles() {
        return roleRepository.findAll(Sort.by("id"));
    }

    // ---------- CREATE ----------
    @Transactional
    public User create(UserForm form) {
        String email = normalise(form.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        Role role = findRole(form.getRoleId());
        User user = new User(form.getName().trim(), email,
                passwordEncoder.encode(form.getPassword()), role);
        userRepository.save(user);

        audit.log("USER_CREATED", describe(user), null,
                "name=" + user.getName() + ", role=" + role.getRoleName());
        return user;
    }

    // ---------- UPDATE ----------
    @Transactional
    public User update(Long id, UserForm form, String actingEmail) {
        User user = get(id);
        String email = normalise(form.getEmail());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("Another user already has this email");
        }
        Role newRole = findRole(form.getRoleId());
        boolean roleChanged = !user.getRole().getId().equals(newRole.getId());

        if (roleChanged) {
            if (user.getEmail().equalsIgnoreCase(actingEmail)) {
                throw new IllegalStateException("You cannot change your own role");
            }
            if (isAdmin(user) && user.isActive() && userRepository.countActiveAdmins() <= 1) {
                throw new IllegalStateException("You cannot remove the role of the last active Admin");
            }
        }

        String before = "name=" + user.getName() + ", email=" + user.getEmail()
                + ", role=" + user.getRole().getRoleName();

        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setRole(newRole);

        boolean passwordReset = form.getPassword() != null && !form.getPassword().isBlank();
        if (passwordReset) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(user);

        String after = "name=" + user.getName() + ", email=" + user.getEmail()
                + ", role=" + newRole.getRoleName();
        audit.log(roleChanged ? "ROLE_CHANGED" : "USER_UPDATED", describe(user), before, after);
        if (passwordReset) {
            audit.log("PASSWORD_RESET", describe(user), null, "password changed by admin");
        }
        return user;
    }

    // ---------- DELETE (soft) ----------
    @Transactional
    public void deactivate(Long id, String actingEmail) {
        User user = get(id);
        if (user.getEmail().equalsIgnoreCase(actingEmail)) {
            throw new IllegalStateException("You cannot deactivate your own account");
        }
        if (isAdmin(user) && user.isActive() && userRepository.countActiveAdmins() <= 1) {
            throw new IllegalStateException("You cannot deactivate the last active Admin");
        }
        user.setActive(false);
        userRepository.save(user);
        audit.log("USER_DEACTIVATED", describe(user), "ACTIVE", "INACTIVE");
    }

    @Transactional
    public void activate(Long id) {
        User user = get(id);
        user.setActive(true);
        userRepository.save(user);
        audit.log("USER_REACTIVATED", describe(user), "INACTIVE", "ACTIVE");
    }

    // ---------- OWN PROFILE ----------
    @Transactional
    public void changeOwnPassword(String email, String currentPassword, String newPassword) {
        User user = getByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        audit.log("PASSWORD_CHANGED", describe(user), null, "password changed by owner");
    }

    // ---------- helpers ----------
    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Selected role does not exist"));
    }

    private boolean isAdmin(User u) {
        return "admin".equalsIgnoreCase(u.getRole().getRoleName());
    }

    private String normalise(String email) {
        return email.trim().toLowerCase();
    }

    private String describe(User u) {
        return "User #" + u.getId() + " (" + u.getEmail() + ")";
    }

    /** At least 8 chars, with a letter and a digit. */
    public static boolean isStrongPassword(String p) {
        return p != null && p.length() >= 8
                && p.matches(".*[A-Za-z].*") && p.matches(".*\\d.*");
    }
}