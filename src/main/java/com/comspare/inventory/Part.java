package com.comspare.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parts")
@Getter
@Setter
@NoArgsConstructor
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product code is required")
    @Column(name = "product_code", unique = true, nullable = false, length = 30)
    private String productCode;

    @NotBlank(message = "Part name is required")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    private String brand;

    private String model;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Double price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @NotNull(message = "Reorder level is required")
    @PositiveOrZero(message = "Reorder level cannot be negative")
    @Column(name = "reorder_level")
    private Integer reorderLevel;

    private String location;

    @Column(length = 1000)
    private String specifications;

    @Column(name = "compatible_with", length = 500)
    private String compatibleWith;

    public boolean isLowStock() {
        return stockQuantity != null && reorderLevel != null && stockQuantity <= reorderLevel;
    }

    public boolean isOutOfStock() {
        return stockQuantity != null && stockQuantity == 0;
    }
}
