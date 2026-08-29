package com.comspare.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final PartHistoryRepository partHistoryRepository;

    public PartService(PartRepository partRepository,
                        StockAdjustmentRepository stockAdjustmentRepository,
                        PartHistoryRepository partHistoryRepository) {
        this.partRepository = partRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.partHistoryRepository = partHistoryRepository;
    }

    // ===================== UC-I04: View Stock =====================

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public Optional<Part> getPartById(Long id) {
        return partRepository.findById(id);
    }

    public List<Part> searchParts(String term) {
        if (term == null || term.isBlank()) {
            return getAllParts();
        }
        return partRepository.search(term);
    }

    // ===================== UC-I01: Add Spare Part =====================

    @Transactional
    public Part addPart(Part part) {
        if (partRepository.existsByProductCode(part.getProductCode())) {
            throw new IllegalArgumentException(
                "A part with product code '" + part.getProductCode() + "' already exists.");
        }
        Part saved = partRepository.save(part);
        logHistoryEvent(saved.getId(), "RECEIVED", saved.getStockQuantity(), "Initial stock on creation");
        return saved;
    }

    // ===================== UC-I02: Update Spare Part =====================

    @Transactional
    public Part updatePart(Long id, Part updatedPart) {
        Part existing = partRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + id));

        existing.setName(updatedPart.getName());
        existing.setCategory(updatedPart.getCategory());
        existing.setBrand(updatedPart.getBrand());
        existing.setModel(updatedPart.getModel());
        existing.setPrice(updatedPart.getPrice());
        existing.setStockQuantity(updatedPart.getStockQuantity());
        existing.setReorderLevel(updatedPart.getReorderLevel());
        existing.setLocation(updatedPart.getLocation());
        existing.setSpecifications(updatedPart.getSpecifications());
        existing.setCompatibleWith(updatedPart.getCompatibleWith());
        // Product code intentionally not editable after creation

        return partRepository.save(existing);
    }

    // ===================== UC-I03: Delete Spare Part =====================

    @Transactional
    public void deletePart(Long id) {
        partRepository.deleteById(id);
    }

    // ===================== UC-I07: Receive Low-Stock Alert =====================

    public List<Part> getLowStockParts() {
        return partRepository.findLowStockParts();
    }

    public long countLowStockParts() {
        return getLowStockParts().size();
    }

    // ===================== UC-I05: Adjust Stock (manual, staff-driven) =====================

    @Transactional
    public Part adjustStock(Long partId, int changeAmount, String reason, String adjustedBy) {
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + partId));

        int newQuantity = part.getStockQuantity() + changeAmount;
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                "This adjustment would result in negative stock (" + newQuantity + ").");
        }

        part.setStockQuantity(newQuantity);
        partRepository.save(part);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setPart(part);
        adjustment.setChangeAmount(changeAmount);
        adjustment.setReason(reason);
        adjustment.setAdjustedBy(adjustedBy);
        stockAdjustmentRepository.save(adjustment);

        logHistoryEvent(partId, "ADJUSTED", changeAmount, reason);

        return part;
    }

    // ===================== UC-I06: View Part History =====================

    public List<PartHistory> getPartHistory(Long partId) {
        return partHistoryRepository.findByPartIdOrderByEventDateDesc(partId);
    }

    // Internal helper — writes one row to the part_history timeline.
    // Public so cross-module calls below can reuse it directly if needed.
    public void logHistoryEvent(Long partId, String eventType, Integer quantity, String notes) {
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + partId));
        PartHistory entry = new PartHistory();
        entry.setPart(part);
        entry.setEventType(eventType);
        entry.setQuantity(quantity);
        entry.setNotes(notes);
        partHistoryRepository.save(entry);
    }

    // =========================================================================
    // CROSS-MODULE METHODS — called by OTHER teammates' services, not by
    // PartController. See the integration instructions below for who calls
    // what, and why these exist instead of using adjustStock() directly.
    // =========================================================================

    // Called by Wickramasinghe K.N's OrderService when an order is confirmed.
    @Transactional
    public void reduceStockForSale(Long partId, int quantity) {
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + partId));

        if (part.getStockQuantity() < quantity) {
            throw new IllegalStateException(
                "Insufficient stock for part '" + part.getName() + "'. Available: "
                + part.getStockQuantity() + ", requested: " + quantity);
        }

        part.setStockQuantity(part.getStockQuantity() - quantity);
        partRepository.save(part);
        logHistoryEvent(partId, "SOLD", quantity, "Sold via customer order");
    }

    // Called by Jayawardana H.M.S.V's PurchaseOrderService when a delivery
    // is marked "Received".
    @Transactional
    public void increaseStockFromDelivery(Long partId, int quantity) {
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + partId));

        part.setStockQuantity(part.getStockQuantity() + quantity);
        partRepository.save(part);
        logHistoryEvent(partId, "RECEIVED", quantity, "Received from supplier delivery");
    }

    // Called by Manaweera H.T's ReturnRequestService when a return is approved
    // and the item is confirmed defective.
    @Transactional
    public void markAsDamagedFromReturn(Long partId, int quantity, String reason) {
        Part part = partRepository.findById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + partId));

        int newQuantity = Math.max(0, part.getStockQuantity() - quantity);
        part.setStockQuantity(newQuantity);
        partRepository.save(part);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setPart(part);
        adjustment.setChangeAmount(-quantity);
        adjustment.setReason("Damaged (return approved): " + reason);
        adjustment.setAdjustedBy("System — Returns module");
        stockAdjustmentRepository.save(adjustment);

        logHistoryEvent(partId, "DAMAGED", quantity, reason);
    }
}
