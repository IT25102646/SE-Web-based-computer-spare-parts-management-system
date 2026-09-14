package com.comspare.returns;

import com.comspare.inventory.Part;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @NotBlank(message = "Customer contact is required")
    @Column(name = "customer_contact", nullable = false, length = 100)
    private String customerContact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank(message = "Return reason is required")
    @Column(name = "return_reason", nullable = false, length = 500)
    private String returnReason;

    @NotNull(message = "Claim type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 20)
    private ClaimType claimType = ClaimType.RETURN;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnStatus status = ReturnStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Resolution resolution;

    @Column(name = "decision_notes", length = 500)
    private String decisionNotes;

    @Column(name = "inventory_processed", nullable = false)
    private boolean inventoryProcessed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ClaimType {
        RETURN, WARRANTY
    }

    public enum ReturnStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    public enum Resolution {
        REFUND, REPLACEMENT, REPAIR, STORE_CREDIT
    }
}
