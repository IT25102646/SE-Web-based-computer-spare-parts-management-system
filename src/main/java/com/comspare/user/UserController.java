package com.comspare.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** ADMIN only (enforced in SecurityConfig). */
@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "user/user-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", userService.getAllRoles());
        return "user/user-form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute User user, @RequestParam Long roleId,
                           Model model, RedirectAttributes ra) {
        try {
            User saved = userService.createUser(user, roleId);
            auditLogService.log("CREATE", "users #" + saved.getId(), null, "Created: " + describe(saved));
            ra.addFlashAttribute("successMessage", "User created.");
            return "redirect:/users";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", userService.getAllRoles());
            return "user/user-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getUserById(id));
        model.addAttribute("roles", userService.getAllRoles());
        return "user/user-form";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user,
                             @RequestParam Long roleId, Model model, RedirectAttributes ra) {
        String oldValue = describe(userService.getUserById(id));
        try {
            User updated = userService.updateUser(id, user, roleId);
            auditLogService.log("UPDATE", "users #" + id, oldValue, describe(updated));
            ra.addFlashAttribute("successMessage", "User updated.");
            return "redirect:/users";
        } catch (IllegalArgumentException | IllegalStateException e) {
            user.setId(id);
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", userService.getAllRoles());
            return "user/user-form";
        }
    }

    // "Delete" = deactivate: the account can no longer log in, but the record stays
    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            User user = userService.deactivateUser(id, auth.getName());
            auditLogService.log("DEACTIVATE", "users #" + id, "active", "deactivated: " + user.getEmail());
            ra.addFlashAttribute("successMessage", "User deactivated. They can no longer log in.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/reactivate/{id}")
    public String reactivate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User user = userService.reactivateUser(id);
            auditLogService.log("REACTIVATE", "users #" + id, "deactivated", "active: " + user.getEmail());
            ra.addFlashAttribute("successMessage", "User reactivated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }

    @GetMapping("/audit")
    public String auditLogs(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("logs", auditLogService.findAll(q));
        model.addAttribute("q", q);
        return "user/audit-list";
    }

    private String describe(User u) {
        return "Name: " + u.getName() + ", Email: " + u.getEmail() + ", Role: " + u.getRole().getRoleName();
    }
}
