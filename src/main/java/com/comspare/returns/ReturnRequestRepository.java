package com.comspare.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestRepository
        extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<ReturnRequest> findByClaimTypeOrderByCreatedAtDesc(String claimType);

    List<ReturnRequest> findByCustomerNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String customerName);
}
