package com.comspare.user;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Writes every login success / failure into the audit trail automatically. */
@Component
public class AuthEventListener {

    private final AuditLogService audit;

    public AuthEventListener(AuditLogService audit) {
        this.audit = audit;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        audit.logAs(event.getAuthentication().getName(), "LOGIN_SUCCESS", "Login", null, null);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        audit.logAs(String.valueOf(event.getAuthentication().getName()), "LOGIN_FAILED", "Login",
                null, event.getException().getClass().getSimpleName());
    }
}