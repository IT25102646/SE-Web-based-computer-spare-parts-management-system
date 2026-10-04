package com.comspare.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }

    /** Root URL -> send people to the right landing page (security decides the rest). */
    @GetMapping("/")
    public String root() {
        return "redirect:/users";
    }
}