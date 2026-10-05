package com.comspare.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByLoggedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
            "LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(a.action) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(a.targetRecord) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY a.loggedAt DESC")
    List<AuditLog> search(@Param("q") String q);
}
