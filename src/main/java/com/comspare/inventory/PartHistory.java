package com.comspare.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Full timeline of everything that happens to a part: received, sold,
// returned, adjusted, or marked damaged. Powers UC-I06 "View Part History".
// Other modules (Orders, Suppliers, Returns) write into this table too —
// see the cross-module methods in PartService for how they call in.
@Entity
@Table(name = "part_history")
@Getter
@Setter
@NoArgsConstructor
public class PartHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    // Allowed values: RECEIVED, SOLD, RETURNED, ADJUSTED, DAMAGED
    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    private Integer quantity;

    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        this.eventDate = LocalDateTime.now();
    }
}
