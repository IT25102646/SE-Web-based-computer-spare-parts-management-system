package com.comspare.order;

import com.comspare.inventory.Part;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @NotNull(message = "Subtotal is required")
    @Column(nullable = false)
    private Double subtotal;

    public void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            subtotal = unitPrice * quantity;
        }
    }
}

