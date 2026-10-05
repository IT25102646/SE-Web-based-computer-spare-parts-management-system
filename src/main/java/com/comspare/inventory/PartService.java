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

    // ===================== READ =====================

    /** Every part, including discontinued ones (for name look-ups on old records). */
    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    /** Only parts that are still sold / orderable. */
    public List<Part> getActiveParts() {
        return partRepository.findByActiveTrueOrderByNameAsc();
    }

    public Optional<Part> getPartById(Long id) {
        return partRepository.findById(id);
    }

    public List<Part> searchParts(String term, boolean discontinued) {
        Boolean active = !discontinued;
        if (term == null || term.isBlank()) {
            return partRepository.findByActiveOrderByNameAsc(active);
        }
        return partRepository.search(term.trim(), active);
    }

    public List<Part> getLowStockParts() {
        return partRepository.findLowStockParts();
    }

    public long countLowStockParts() {
        return getLowStockParts().size();
    }

    public List<PartHistory> getPartHistory(Long partId) {
        return partHistoryRepository.findByPartIdOrderByEventDateDesc(partId);
    }

    // ===================== CREATE =====================

    @Transactional
    public Part addPart(Part part, String reason) {
        requireReason(reason);
        part.setProductCode(part.getProductCode().trim());
        if (partRepository.existsByProductCode(part.getProductCode())) {
            throw new IllegalArgumentException(
                    "A part with product code '" + part.getProductCode() + "' already exists (it may be discontinued).");
        }
        part.setActive(true);
        Part saved = partRepository.save(part);
        log(saved, "CREATED", saved.getStockQuantity(),
                "Part added (initial stock " + saved.getStockQuantity() + "). Reason: " + reason.trim());
        return saved;
    }

    // ===================== UPDATE =====================

    /** Product code and stock quantity are NOT changed here (stock only changes via adjustStock). */
    @Transactional
    public Part updatePart(Long id, Part updated, String reason) {
        requireReason(reason);
        Part existing = requirePart(id);

        StringBuilder changes = new StringBuilder();
        diff(changes, "name", existing.getName(), updated.getName());
        diff(changes, "category", existing.getCategory(), updated.getCategory());
        diff(changes, "brand", existing.getBrand(), updated.getBrand());
        diff(changes, "model", existing.getModel(), updated.getModel());
        diff(changes, "price", existing.getPrice(), updated.getPrice());
        diff(changes, "reorder level", existing.getReorderLevel(), updated.getReorderLevel());
        diff(changes, "location", existing.getLocation(), updated.getLocation());
        diff(changes, "specifications", existing.getSpecifications(), updated.getSpecifications());
        diff(changes, "compatible with", existing.getCompatibleWith(), updated.getCompatibleWith());

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setBrand(updated.getBrand());
        existing.setModel(updated.getModel());
        existing.setPrice(updated.getPrice());
        existing.setReorderLevel(updated.getReorderLevel());
        existing.setLocation(updated.getLocation());
        existing.setSpecifications(updated.getSpecifications());
        existing.setCompatibleWith(updated.getCompatibleWith());
        Part saved = partRepository.save(existing);

        log(saved, "UPDATED", null, "Reason: " + reason.trim()
                + (changes.length() == 0 ? " | no field values changed" : " | " + changes));
        return saved;
    }

    // ===================== DELETE (= DISCONTINUE) =====================

    /**
     * "Deleting" a part discontinues it: it disappears from the inventory list and from order /
     * purchase-order dropdowns, but the row stays so past orders, invoices, returns and the part
     * history remain valid. Remaining stock must be written off as a loss (damaged) first.
     */
    @Transactional
    public void discontinuePart(Long id, String reason, boolean writeOff, String by) {
        requireReason(reason);
        Part part = requirePart(id);
        if (!Boolean.TRUE.equals(part.getActive())) {
            throw new IllegalStateException("This part is already discontinued.");
        }

        int stock = part.getStockQuantity();
        if (stock > 0) {
            if (!writeOff) {
                throw new IllegalArgumentException("This part still has " + stock
                        + " in stock. Tick \"write off as a loss\", or adjust the stock to 0 first.");
            }
            StockAdjustment adjustment = new StockAdjustment();
            adjustment.setPart(part);
            adjustment.setChangeAmount(-stock);
            adjustment.setReason("Discontinued - written off");
            adjustment.setAdjustedBy(cut(by, 100));
            stockAdjustmentRepository.save(adjustment);

            log(part, "DAMAGED", stock, "Written off on discontinuation. Reason: " + reason.trim());
            part.setStockQuantity(0);
        }

        part.setActive(false);
        partRepository.save(part);
        log(part, "DISCONTINUED", stock, "Part discontinued. Reason: " + reason.trim());
    }

    @Transactional
    public void restorePart(Long id) {
        Part part = requirePart(id);
        if (Boolean.TRUE.equals(part.getActive())) {
            throw new IllegalStateException("This part is already active.");
        }
        part.setActive(true);
        partRepository.save(part);
        log(part, "RESTORED", null, "Part restored to the active inventory");
    }

    // ===================== MANUAL STOCK ADJUSTMENT =====================

    @Transactional
    public Part adjustStock(Long partId, int changeAmount, String reason, String adjustedBy) {
        if (changeAmount == 0) {
            throw new IllegalArgumentException("Change amount cannot be zero.");
        }
        Part part = requirePart(partId);

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
        adjustment.setReason(cut(reason, 50));
        adjustment.setAdjustedBy(cut(adjustedBy, 100));
        stockAdjustmentRepository.save(adjustment);

        // "Damaged" removals feed the Damaged / Loss reports
        String type = (changeAmount < 0 && "Damaged".equalsIgnoreCase(reason)) ? "DAMAGED" : "ADJUSTED";
        log(part, type, Math.abs(changeAmount), reason + " (" + (changeAmount > 0 ? "+" : "") + changeAmount + ")");
        return part;
    }

    // =========================================================================
    // CROSS-MODULE METHODS - used by the Orders, Supplier and Returns modules
    // =========================================================================

    /** Orders: stock leaves when an order is placed. */
    @Transactional
    public void reduceStockForSale(Long partId, int quantity, String note) {
        Part part = requirePart(partId);
        if (part.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Not enough stock for " + part.getName()
                    + ". Available stock: " + part.getStockQuantity());
        }
        part.setStockQuantity(part.getStockQuantity() - quantity);
        partRepository.save(part);
        log(part, "SOLD", quantity, note);
    }

    /** Orders: cancelled / edited / deleted order puts stock back. */
    @Transactional
    public void restoreStockFromOrder(Long partId, int quantity, String note) {
        Part part = requirePart(partId);
        part.setStockQuantity(part.getStockQuantity() + quantity);
        partRepository.save(part);
        log(part, "RETURNED", quantity, note);
    }

    /** Purchase orders: a delivery was received from the supplier. */
    @Transactional
    public void increaseStockFromDelivery(Long partId, int quantity, String note) {
        Part part = requirePart(partId);
        part.setStockQuantity(part.getStockQuantity() + quantity);
        partRepository.save(part);
        log(part, "RECEIVED", quantity, note);
    }

    /** Returns: a good item came back and can be sold again. */
    @Transactional
    public void restockFromReturn(Long partId, int quantity, String note) {
        Part part = requirePart(partId);
        part.setStockQuantity(part.getStockQuantity() + quantity);
        partRepository.save(part);
        log(part, "RETURNED", quantity, note);
    }

    /**
     * Returns: a defective item came back. It was already taken off the shelf when it was
     * sold, so stock does not change - the unit is only recorded as DAMAGED (loss report).
     */
    @Transactional
    public void markAsDamagedFromReturn(Long partId, int quantity, String reason) {
        Part part = requirePart(partId);
        log(part, "DAMAGED", quantity, "Returned defective: " + reason);
    }

    /** Kept for compatibility with older callers. */
    @Transactional
    public void logHistoryEvent(Long partId, String eventType, Integer quantity, String notes) {
        log(requirePart(partId), eventType, quantity, notes);
    }

    // ===================== helpers =====================

    private Part requirePart(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Part not found with id: " + id));
    }

    private void log(Part part, String eventType, Integer quantity, String notes) {
        PartHistory entry = new PartHistory();
        entry.setPart(part);
        entry.setEventType(eventType);
        entry.setQuantity(quantity);
        entry.setNotes(cut(notes, 500));
        partHistoryRepository.save(entry);
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required.");
        }
    }

    private static void diff(StringBuilder sb, String label, Object oldV, Object newV) {
        String o = oldV == null ? "" : oldV.toString().trim();
        String n = newV == null ? "" : newV.toString().trim();
        if (!o.equals(n)) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(label).append(": ").append(o.isEmpty() ? "-" : o).append(" -> ").append(n.isEmpty() ? "-" : n);
        }
    }

    private static String cut(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }
}