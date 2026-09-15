package com.comspare.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService,
                          AuditLogService auditLogService) {

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
    public String saveUser(@ModelAttribute User user,
                           @RequestParam Long roleId) {

        User savedUser = userService.createUser(user, roleId);

        auditLogService.log(
                null,
                "CREATE",
                "users",
                savedUser.getId(),
                null,
                "Created user: " + savedUser.getEmail()
        );

        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model) {

        model.addAttribute("user", userService.getUserById(id));
        model.addAttribute("roles", userService.getAllRoles());

        return "user/user-form";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             @RequestParam Long roleId) {

        User existingUser = userService.getUserById(id);

        String oldValue =
                "Name: " + existingUser.getName()
                        + ", Email: " + existingUser.getEmail()
                        + ", Role: " + existingUser.getRole().getRoleName();

        User updatedUser =
                userService.updateUser(id, user, roleId);

        String newValue =
                "Name: " + updatedUser.getName()
                        + ", Email: " + updatedUser.getEmail()
                        + ", Role: " + updatedUser.getRole().getRoleName();

        auditLogService.log(
                null,
                "UPDATE",
                "users",
                id,
                oldValue,
                newValue
        );

        return "redirect:/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {

        User user = userService.getUserById(id);

        String oldValue =
                "Deleted user: " + user.getEmail();

        auditLogService.log(
                null,
                "DELETE",
                "users",
                id,
                oldValue,
                null
        );

        userService.deleteUser(id);

        return "redirect:/users";
    }

    @GetMapping("/audit")
    public String auditLogs(Model model) {

        model.addAttribute(
                "logs",
                auditLogService.getAllLogs()
        );

        return "user/audit-list";
    }
}