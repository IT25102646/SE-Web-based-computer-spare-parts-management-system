package com.comspare.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Any logged-in user: view own profile and change own password. */
@Controller
public class ProfileController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public ProfileController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        model.addAttribute("me", userService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
        return "user/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth, RedirectAttributes ra) {
        try {
            userService.changePassword(auth.getName(), currentPassword, newPassword, confirmPassword);
            auditLogService.log("PASSWORD_CHANGE", "users", null, "Password changed by " + auth.getName());
            ra.addFlashAttribute("successMessage", "Password updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}
