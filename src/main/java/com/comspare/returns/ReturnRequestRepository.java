package com.comspare.returns;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @Query("""
        SELECT r FROM ReturnRequest r
        JOIN r.part p
        WHERE LOWER(r.customerName) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(r.customerContact) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
        ORDER BY r.createdAt DESC
        """)
    List<ReturnRequest> search(@Param("term") String term);

    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
}
