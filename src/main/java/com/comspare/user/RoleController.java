package com.comspare.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** ADMIN only (enforced in SecurityConfig). */
@Controller
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;
    private final AuditLogService auditLogService;

    public RoleController(RoleService roleService, AuditLogService auditLogService) {
        this.roleService = roleService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("userCounts", roleService.getUserCounts());
        model.addAttribute("builtIn", RoleService.BUILT_IN);
        model.addAttribute("builtInList", RoleService.BUILT_IN_ORDER);
        model.addAttribute("access", RoleService.ACCESS);
        return "user/role-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("role", new Role());
        return "user/role-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam String roleName, Model model, RedirectAttributes ra) {
        try {
            Role saved = roleService.createRole(roleName);
            auditLogService.log("CREATE", "roles #" + saved.getId(), null, "Created role: " + saved.getRoleName());
            ra.addFlashAttribute("successMessage", "Role created.");
            return "redirect:/roles";
        } catch (IllegalArgumentException | IllegalStateException e) {
            Role role = new Role();
            role.setRoleName(roleName);
            model.addAttribute("role", role);
            model.addAttribute("errorMessage", e.getMessage());
            return "user/role-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Role role = roleService.getRoleById(id);
        if (roleService.isBuiltIn(role)) {
            ra.addFlashAttribute("errorMessage", "Built-in roles cannot be renamed.");
            return "redirect:/roles";
        }
        model.addAttribute("role", role);
        return "user/role-form";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id, @RequestParam String roleName,
                         Model model, RedirectAttributes ra) {
        String old = roleService.getRoleById(id).getRoleName();
        try {
            Role saved = roleService.renameRole(id, roleName);
            auditLogService.log("UPDATE", "roles #" + id, old, saved.getRoleName());
            ra.addFlashAttribute("successMessage", "Role renamed.");
            return "redirect:/roles";
        } catch (IllegalArgumentException | IllegalStateException e) {
            Role role = new Role();
            role.setId(id);
            role.setRoleName(roleName);
            model.addAttribute("role", role);
            model.addAttribute("errorMessage", e.getMessage());
            return "user/role-form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Role removed = roleService.deleteRole(id);
            auditLogService.log("DELETE", "roles #" + id, "Deleted role: " + removed.getRoleName(), null);
            ra.addFlashAttribute("successMessage", "Role deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/roles";
    }
}
