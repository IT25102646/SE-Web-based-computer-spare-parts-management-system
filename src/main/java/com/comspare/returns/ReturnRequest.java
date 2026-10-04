package com.comspare.returns;

import com.comspare.inventory.Part;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "Please select a spare part")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank(message = "Return reason is required")
    @Column(name = "return_reason", nullable = false, length = 500)
    private String returnReason;

    @NotBlank(message = "Claim type is required")
    @Column(name = "claim_type", nullable = false, length = 20)
    private String claimType = "RETURN";

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 20)
    private String resolution;

    @Column(name = "decision_notes", length = 500)
    private String decisionNotes;

    @Column(name = "inventory_processed", nullable = false)
    private Boolean inventoryProcessed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = "PENDING";
        }

        if (inventoryProcessed == null) {
            inventoryProcessed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
