package com.comspare.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoleService {

    /** The six built-in roles: protected, cannot be renamed or deleted. */
    public static final List<String> BUILT_IN_ORDER = List.of(
            "ADMIN", "OPERATIONS_MANAGER", "INVENTORY_SUPERVISOR",
            "STORE_KEEPER", "CUSTOMER_SERVICE_EXECUTIVE", "FINANCE_OFFICER");

    public static final Set<String> BUILT_IN = new LinkedHashSet<>(BUILT_IN_ORDER);

    /** Access matrix (mirrors the rules in SecurityConfig). */
    public static final Map<String, List<String>> ACCESS = new LinkedHashMap<>();
    static {
        ACCESS.put("Inventory", List.of("ADMIN", "INVENTORY_SUPERVISOR", "STORE_KEEPER", "OPERATIONS_MANAGER"));
        ACCESS.put("Orders", List.of("ADMIN", "CUSTOMER_SERVICE_EXECUTIVE", "OPERATIONS_MANAGER"));
        ACCESS.put("Suppliers & Purchase Orders", List.of("ADMIN", "INVENTORY_SUPERVISOR", "OPERATIONS_MANAGER"));
        ACCESS.put("Billing", List.of("ADMIN", "FINANCE_OFFICER"));
        ACCESS.put("Returns", List.of("ADMIN", "CUSTOMER_SERVICE_EXECUTIVE", "INVENTORY_SUPERVISOR"));
        ACCESS.put("Reports", List.of("ADMIN", "OPERATIONS_MANAGER", "FINANCE_OFFICER", "INVENTORY_SUPERVISOR"));
        ACCESS.put("Users & Roles", List.of("ADMIN"));
    }

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Role not found"));
    }

    public Map<Long, Long> getUserCounts() {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Role r : roleRepository.findAll()) {
            counts.put(r.getId(), userRepository.countByRoleId(r.getId()));
        }
        return counts;
    }

    public boolean isBuiltIn(Role role) {
        return BUILT_IN.contains(role.getRoleName().toUpperCase());
    }

    @Transactional
    public Role createRole(String rawName) {
        String name = normalize(rawName);
        if (roleRepository.existsByRoleNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A role named '" + name + "' already exists.");
        }
        return roleRepository.save(new Role(name));
    }

    @Transactional
    public Role renameRole(Long id, String rawName) {
        Role role = getRoleById(id);
        if (isBuiltIn(role)) {
            throw new IllegalStateException("Built-in roles cannot be renamed.");
        }
        String name = normalize(rawName);
        if (!name.equalsIgnoreCase(role.getRoleName()) && roleRepository.existsByRoleNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A role named '" + name + "' already exists.");
        }
        role.setRoleName(name);
        return roleRepository.save(role);
    }

    @Transactional
    public Role deleteRole(Long id) {
        Role role = getRoleById(id);
        if (isBuiltIn(role)) {
            throw new IllegalStateException("Built-in roles cannot be deleted.");
        }
        long users = userRepository.countByRoleId(id);
        if (users > 0) {
            throw new IllegalStateException("This role is assigned to " + users
                    + " user(s). Move them to another role first.");
        }
        roleRepository.delete(role);
        return role;
    }

    /** "warehouse auditor" -> WAREHOUSE_AUDITOR */
    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Role name is required.");
        }
        String name = raw.trim().toUpperCase().replaceAll("[\\s-]+", "_");
        if (!name.matches("[A-Z][A-Z0-9_]{2,49}")) {
            throw new IllegalArgumentException(
                    "Role name must be 3-50 characters: letters, numbers and underscores, starting with a letter.");
        }
        return name;
    }
}
