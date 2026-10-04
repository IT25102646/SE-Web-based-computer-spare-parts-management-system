package com.comspare.user;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Logs an action performed by the currently logged-in user. */
    public void log(String action, String target, String oldValue, String newValue) {
        repository.save(new AuditLog(currentActor(), action, target, oldValue, newValue));
    }

    /** Logs an action for a specific actor (used for login success/failure events). */
    public void logAs(String actor, String action, String target, String oldValue, String newValue) {
        repository.save(new AuditLog(actor, action, target, oldValue, newValue));
    }

    public List<AuditLog> findAll(String query) {
        if (query == null || query.isBlank()) {
            return repository.findAllByOrderByLoggedAtDesc();
        }
        return repository.search(query.trim());
    }

    public static String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return "SYSTEM";
        }
        return auth.getName();
    }
}