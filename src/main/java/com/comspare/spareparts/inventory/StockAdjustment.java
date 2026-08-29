package com.comspare.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Records a manual stock correction: damaged, lost, or a counting error.
// This is the audit record behind UC-I05 "Adjust Stock" — every correction
// a staff member makes is permanently logged here with a reason.
@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @NotNull(message = "Change amount is required")
    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount; // positive = added, negative = removed

    @NotBlank(message = "A reason is required for every stock adjustment")
    @Column(nullable = false, length = 50)
    private String reason; // Damaged, Lost, Correction, Other

    @Column(name = "adjusted_by", length = 100)
    private String adjustedBy;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDateTime adjustmentDate;

    @PrePersist
    protected void onCreate() {
        this.adjustmentDate = LocalDateTime.now();
    }
}
