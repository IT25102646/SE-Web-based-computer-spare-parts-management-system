package com.comspare.supplier;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PurchaseOrderService {

    public static final List<String> EDITABLE_STATUSES = List.of("PENDING", "APPROVED");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final PartService partService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                PartService partService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.partService = partService;
    }

    // ===================== READ =====================

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    public String generateOrderNumber() {
        long next = purchaseOrderRepository.count() + 1;
        String number;
        do {
            number = String.format("PO%03d", next++);
        } while (purchaseOrderRepository.findByOrderNumber(number).isPresent());
        return number;
    }

    /** part id -> "CODE - Name", used to show part names on the form and view pages. */
    public Map<Long, String> getPartNames() {
        Map<Long, String> names = new LinkedHashMap<>();
        for (Part p : partService.getAllParts()) {
            names.put(p.getId(), p.getProductCode() + " - " + p.getName());
        }
        return names;
    }

    /** Same as getPartNames() but only parts that can still be ordered. */
    public Map<Long, String> getActivePartNames() {
        Map<Long, String> names = new LinkedHashMap<>();
        for (Part p : partService.getActiveParts()) {
            names.put(p.getId(), p.getProductCode() + " - " + p.getName());
        }
        return names;
    }

    public double calculateTotal(PurchaseOrder po) {
        double total = 0;
        for (PurchaseOrderItem i : po.getItems()) {
            total += i.getOrderedQuantity() * (i.getUnitCost() == null ? 0 : i.getUnitCost());
        }
        return Math.round(total * 100.0) / 100.0;
    }

    // ===================== CREATE + UPDATE =====================

    @Transactional
    public PurchaseOrder savePurchaseOrder(PurchaseOrder form) {

        if (form.getOrderNumber() == null || form.getOrderNumber().isBlank()) {
            throw new IllegalArgumentException("Order number is required.");
        }
        if (form.getSupplier() == null || form.getSupplier().getId() == null) {
            throw new IllegalArgumentException("Please select a supplier.");
        }
        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one spare part to the purchase order.");
        }

        String number = form.getOrderNumber().trim();
        Optional<PurchaseOrder> clash = purchaseOrderRepository.findByOrderNumber(number);
        if (clash.isPresent() && !clash.get().getId().equals(form.getId())) {
            throw new IllegalArgumentException("Purchase order number '" + number + "' already exists.");
        }

        Supplier supplier = supplierRepository.findById(form.getSupplier().getId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));

        LocalDate orderDate = form.getOrderDate() != null ? form.getOrderDate() : LocalDate.now();
        if (form.getExpectedDeliveryDate() != null && form.getExpectedDeliveryDate().isBefore(orderDate)) {
            throw new IllegalArgumentException("Expected delivery date cannot be before the order date.");
        }

        PurchaseOrder po;
        if (form.getId() == null) {
            po = new PurchaseOrder();
        } else {
            po = purchaseOrderRepository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase order not found."));
            if (!EDITABLE_STATUSES.contains(po.getStatus())) {
                throw new IllegalStateException("Received or cancelled purchase orders cannot be edited.");
            }
            po.getItems().clear();
            purchaseOrderRepository.flush();
        }

        List<PurchaseOrderItem> submitted = new ArrayList<>(form.getItems());
        for (PurchaseOrderItem in : submitted) {
            if (in.getPartId() == null || partService.getPartById(in.getPartId()).isEmpty()) {
                throw new IllegalArgumentException("Please select a valid part for every item.");
            }
            if (in.getOrderedQuantity() == null || in.getOrderedQuantity() < 1) {
                throw new IllegalArgumentException("Ordered quantity must be at least 1.");
            }
            if (in.getUnitCost() != null && in.getUnitCost() < 0) {
                throw new IllegalArgumentException("Unit cost cannot be negative.");
            }
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPartId(in.getPartId());
            item.setOrderedQuantity(in.getOrderedQuantity());
            item.setUnitCost(in.getUnitCost() == null ? 0.0 : in.getUnitCost());
            item.setReceivedQuantity(0);
            item.setPurchaseOrder(po);
            po.getItems().add(item);
        }

        po.setOrderNumber(number);
        po.setSupplier(supplier);
        po.setOrderDate(orderDate);
        po.setExpectedDeliveryDate(form.getExpectedDeliveryDate());
        po.setNotes(form.getNotes());
        po.setStatus(EDITABLE_STATUSES.contains(form.getStatus()) ? form.getStatus() : "PENDING");

        return purchaseOrderRepository.save(po);
    }

    // ===================== RECEIVE DELIVERY =====================

    /** Receives everything still outstanding and adds it to inventory stock. */
    @Transactional
    public void confirmDelivery(Long purchaseOrderId) {
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if ("RECEIVED".equalsIgnoreCase(po.getStatus())) {
            throw new IllegalStateException("This purchase order has already been received.");
        }
        if ("CANCELLED".equalsIgnoreCase(po.getStatus())) {
            throw new IllegalStateException("A cancelled purchase order cannot be received.");
        }
        if (po.getItems().isEmpty()) {
            throw new IllegalStateException("This purchase order has no items to receive.");
        }

        for (PurchaseOrderItem item : po.getItems()) {
            int already = item.getReceivedQuantity() == null ? 0 : item.getReceivedQuantity();
            int remaining = item.getOrderedQuantity() - already;
            if (remaining > 0) {
                partService.increaseStockFromDelivery(item.getPartId(), remaining,
                        "Received from purchase order " + po.getOrderNumber());
                item.setReceivedQuantity(item.getOrderedQuantity());
            }
        }
        po.setDeliveryDate(LocalDate.now());
        po.setStatus("RECEIVED");
        purchaseOrderRepository.save(po);
    }

    // ===================== CANCEL / DELETE =====================

    @Transactional
    public void cancelPurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        if ("RECEIVED".equalsIgnoreCase(po.getStatus())) {
            throw new IllegalStateException("A received purchase order cannot be cancelled.");
        }
        po.setStatus("CANCELLED");
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        if ("RECEIVED".equalsIgnoreCase(po.getStatus())) {
            throw new IllegalStateException(
                    "A received purchase order is part of the stock history and cannot be deleted.");
        }
        purchaseOrderRepository.delete(po);
    }
}