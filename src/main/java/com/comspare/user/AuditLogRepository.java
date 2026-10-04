package com.comspare.user;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
package com.comspare.user;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Extends the bare Repository marker (NOT JpaRepository) so that only the
 * methods declared here exist: save + read. There is no delete or update,
 * which keeps the audit trail append-only at code level.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog log);

    List<AuditLog> findAllByOrderByLoggedAtDesc();

    @Query("""
           select a from AuditLog a
           where lower(a.performedBy) like lower(concat('%', :q, '%'))
              or lower(a.action) like lower(concat('%', :q, '%'))
              or lower(a.targetRecord) like lower(concat('%', :q, '%'))
           order by a.loggedAt desc
           """)
    List<AuditLog> search(@Param("q") String q);
}