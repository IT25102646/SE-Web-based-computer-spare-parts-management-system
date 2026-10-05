package com.comspare.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {

    boolean existsByProductCode(String productCode);

    List<Part> findByActiveOrderByNameAsc(Boolean active);

    List<Part> findByActiveTrueOrderByNameAsc();

    // Low-stock alert: active parts only
    @Query("SELECT p FROM Part p WHERE p.active = true AND p.stockQuantity <= p.reorderLevel")
    List<Part> findLowStockParts();

    // Search bar on the inventory list (active or discontinued view)
    @Query("SELECT p FROM Part p WHERE p.active = :active AND (" +
            "LOWER(p.productCode) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :term, '%'))) ORDER BY p.name")
    List<Part> search(@Param("term") String term, @Param("active") Boolean active);
}
