package com.comspare.supplier;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryStockService inventoryStockService;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            InventoryStockService inventoryStockService) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryStockService = inventoryStockService;
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    public PurchaseOrder savePurchaseOrder(
            PurchaseOrder purchaseOrder) {

        if (purchaseOrder.getSupplier() == null ||
                purchaseOrder.getSupplier().getId() == null) {

            throw new IllegalArgumentException(
                    "Supplier is required.");
        }

        if (purchaseOrder.getOrderNumber() == null ||
                purchaseOrder.getOrderNumber().isBlank()) {

            purchaseOrder.setOrderNumber(
                    "PO-" +
                            UUID.randomUUID()
                                    .toString()
                                    .substring(0, 8)
                                    .toUpperCase());
        }

        if (purchaseOrder.getOrderDate() == null) {
            purchaseOrder.setOrderDate(LocalDate.now());
        }

        if (purchaseOrder.getStatus() == null ||
                purchaseOrder.getStatus().isBlank()) {

            purchaseOrder.setStatus("PENDING");
        }

        if (purchaseOrder.getItems() == null ||
                purchaseOrder.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one purchase order item is required.");
        }

        for (PurchaseOrderItem item :
                purchaseOrder.getItems()) {

            if (item.getPartId() == null) {
                throw new IllegalArgumentException(
                        "Part ID is required.");
            }

            if (item.getOrderedQuantity() == null ||
                    item.getOrderedQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Ordered quantity must be greater than zero.");
            }

            if (item.getReceivedQuantity() == null) {
                item.setReceivedQuantity(0);
            }

            if (item.getReceivedQuantity() < 0) {
                throw new IllegalArgumentException(
                        "Received quantity cannot be negative.");
            }

            if (item.getReceivedQuantity() >
                    item.getOrderedQuantity()) {

                throw new IllegalArgumentException(
                        "Received quantity cannot exceed ordered quantity.");
            }

            item.setPurchaseOrder(purchaseOrder);
        }

        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void confirmDelivery(Long purchaseOrderId) {

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository.findById(purchaseOrderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase order not found."));

        if ("RECEIVED".equalsIgnoreCase(
                purchaseOrder.getStatus())) {

            throw new IllegalStateException(
                    "This purchase order has already been received.");
        }

        if ("CANCELLED".equalsIgnoreCase(
                purchaseOrder.getStatus())) {

            throw new IllegalStateException(
                    "Cancelled purchase orders cannot be received.");
        }

        if (purchaseOrder.getItems() == null ||
                purchaseOrder.getItems().isEmpty()) {

            throw new IllegalStateException(
                    "No items found in this purchase order.");
        }

        for (PurchaseOrderItem item :
                purchaseOrder.getItems()) {

            Integer received =
                    item.getReceivedQuantity();

            if (received == null || received == 0) {
                received = item.getOrderedQuantity();
                item.setReceivedQuantity(received);
            }

            if (received > item.getOrderedQuantity()) {
                throw new IllegalStateException(
                        "Received quantity cannot exceed ordered quantity.");
            }

            inventoryStockService.increaseStock(
                    item.getPartId(),
                    received
            );
        }

        purchaseOrder.setDeliveryDate(LocalDate.now());

        purchaseOrder.setStatus("RECEIVED");

        purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void cancelPurchaseOrder(Long id) {

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase order not found."));

        if ("RECEIVED".equalsIgnoreCase(
                purchaseOrder.getStatus())) {

            throw new IllegalStateException(
                    "A received purchase order cannot be cancelled.");
        }

        purchaseOrder.setStatus("CANCELLED");

        purchaseOrderRepository.save(purchaseOrder);
    }
}