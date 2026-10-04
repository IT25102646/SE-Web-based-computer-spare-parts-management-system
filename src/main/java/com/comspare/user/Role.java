package com.comspare.user;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 100)
    private String roleName;

    public Role() {}

    public Role(String roleName) {
        this.roleName = roleName;
    }

    /** "Store Keeper" -> "ROLE_STORE_KEEPER" (the name Spring Security checks) */
    public String toAuthority() {
        return "ROLE_" + roleName.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}