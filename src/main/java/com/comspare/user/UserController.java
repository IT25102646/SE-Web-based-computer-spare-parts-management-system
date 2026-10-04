package com.comspare.user;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    // ================= READ (list) =================
    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("activePage", "users");
        return "user/user-list";
    }

    // ================= CREATE =================
    @GetMapping("/users/new")
    public String newForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        return formView(model, false);
    }

    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult result, Model model, RedirectAttributes flash) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            result.rejectValue("password", "required", "Password is required for a new user");
        } else if (!UserService.isStrongPassword(form.getPassword())) {
            result.rejectValue("password", "weak", "Use at least 8 characters with a letter and a number");
        }
        if (result.hasErrors()) {
            return formView(model, false);
        }
        try {
            User created = userService.create(form);
            flash.addFlashAttribute("success", "Created account for " + created.getName());
            return "redirect:/users";
        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return formView(model, false);
        }
    }

    // ================= UPDATE =================
    @GetMapping("/users/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.get(id);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setRoleId(user.getRole().getId());
        model.addAttribute("userForm", form);
        return formView(model, true);
    }

    @PostMapping("/users/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult result, Model model,
                         Authentication auth, RedirectAttributes flash) {
        form.setId(id);
        if (form.getPassword() != null && !form.getPassword().isBlank()
                && !UserService.isStrongPassword(form.getPassword())) {
            result.rejectValue("password", "weak", "Use at least 8 characters with a letter and a number");
        }
        if (result.hasErrors()) {
            return formView(model, true);
        }
        try {
            userService.update(id, form, auth.getName());
            flash.addFlashAttribute("success", "Saved changes for " + form.getName());
            return "redirect:/users";
        } catch (IllegalArgumentException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return formView(model, true);
        } catch (IllegalStateException e) {
            result.rejectValue("roleId", "rule", e.getMessage());
            return formView(model, true);
        }
    }

    // ================= DELETE (soft) / RESTORE =================
    @PostMapping("/users/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        try {
            userService.deactivate(id, auth.getName());
            flash.addFlashAttribute("success", "Account deactivated. Their history is kept.");
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes flash) {
        userService.activate(id);
        flash.addFlashAttribute("success", "Account reactivated.");
        return "redirect:/users";
    }

    // ================= AUDIT TRAIL (read-only) =================
    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("logs", auditLogService.findAll(q));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("activePage", "audit");
        return "user/audit-list";
    }

    // ================= MY PROFILE (any logged-in user) =================
    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        model.addAttribute("me", userService.getByEmail(auth.getName()));
        model.addAttribute("activePage", "profile");
        return "user/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth, RedirectAttributes flash) {
        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "The new passwords do not match");
        } else if (!UserService.isStrongPassword(newPassword)) {
            flash.addFlashAttribute("error", "Use at least 8 characters with a letter and a number");
        } else {
            try {
                userService.changeOwnPassword(auth.getName(), currentPassword, newPassword);
                flash.addFlashAttribute("success", "Password updated");
            } catch (IllegalArgumentException e) {
                flash.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/profile";
    }

    // ---------- helper ----------
    private String formView(Model model, boolean editing) {
        model.addAttribute("roles", userService.findRoles());
        model.addAttribute("editing", editing);
        model.addAttribute("activePage", "users");
        return "user/user-form";
    }
}