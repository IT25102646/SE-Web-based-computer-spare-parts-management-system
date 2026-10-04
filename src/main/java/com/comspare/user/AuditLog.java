package com.comspare.user;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * One row = one sensitive action: who, what, on which record, before/after, when.
 * @Immutable tells Hibernate to never issue UPDATE/DELETE for this entity.
 */
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performed_by", nullable = false, length = 150, updatable = false)
    private String performedBy;

    @Column(nullable = false, length = 60, updatable = false)
    private String action;

    @Column(name = "target_record", length = 255, updatable = false)
    private String targetRecord;

    @Column(name = "old_value", length = 500, updatable = false)
    private String oldValue;

    @Column(name = "new_value", length = 500, updatable = false)
    private String newValue;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    public AuditLog() {}

    public AuditLog(String performedBy, String action, String targetRecord,
                    String oldValue, String newValue) {
        this.performedBy = performedBy;
        this.action = action;
        this.targetRecord = targetRecord;
        this.oldValue = trim(oldValue);
        this.newValue = trim(newValue);
        this.loggedAt = LocalDateTime.now();
    }

    private static String trim(String s) {
        return (s != null && s.length() > 500) ? s.substring(0, 500) : s;
    }

    public Long getId() { return id; }
    public String getPerformedBy() { return performedBy; }
    public String getAction() { return action; }
    public String getTargetRecord() { return targetRecord; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
}