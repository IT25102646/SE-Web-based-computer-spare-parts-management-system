package com.comspare.supplier;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    public PurchaseOrder savePurchaseOrder(
            PurchaseOrder purchaseOrder) {

        if (purchaseOrder.getOrderDate() == null) {
            purchaseOrder.setOrderDate(LocalDate.now());
        }

        if (purchaseOrder.getStatus() == null ||
                purchaseOrder.getStatus().isBlank()) {

            purchaseOrder.setStatus("PENDING");
        }

        if (purchaseOrder.getItems() != null) {

            for (PurchaseOrderItem item :
                    purchaseOrder.getItems()) {

                item.setPurchaseOrder(purchaseOrder);

                if (item.getReceivedQuantity() == null) {
                    item.setReceivedQuantity(0);
                }
            }
        }

        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void confirmDelivery(Long purchaseOrderId) {

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository
                        .findById(purchaseOrderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase order not found"));

        if ("RECEIVED".equalsIgnoreCase(
                purchaseOrder.getStatus())) {

            throw new IllegalStateException(
                    "This purchase order has already been received.");
        }

        if (purchaseOrder.getItems() != null) {

            for (PurchaseOrderItem item :
                    purchaseOrder.getItems()) {

                if (item.getReceivedQuantity() == null) {

                    item.setReceivedQuantity(
                            item.getOrderedQuantity()
                    );
                }
            }
        }

        purchaseOrder.setDeliveryDate(LocalDate.now());

        purchaseOrder.setStatus("RECEIVED");

        purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void cancelPurchaseOrder(Long id) {

        PurchaseOrder purchaseOrder =
                purchaseOrderRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase order not found"));

        purchaseOrder.setStatus("CANCELLED");

        purchaseOrderRepository.save(purchaseOrder);
    }
}