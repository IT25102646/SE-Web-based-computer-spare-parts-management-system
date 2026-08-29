package com.comspare.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {

    boolean existsByProductCode(String productCode);

    // Powers UC-I07: Receive Low-Stock Alert
    @Query("SELECT p FROM Part p WHERE p.stockQuantity <= p.reorderLevel")
    List<Part> findLowStockParts();

    // Powers the search bar on the inventory list page
    @Query("SELECT p FROM Part p WHERE " +
           "LOWER(p.productCode) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Part> search(@Param("term") String term);
}
