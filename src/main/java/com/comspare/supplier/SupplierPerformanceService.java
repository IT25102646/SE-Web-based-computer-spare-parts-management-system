package com.comspare.supplier;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierPerformanceService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierPerformanceService(
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository) {

        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public List<SupplierPerformance> getPerformance() {

        List<SupplierPerformance> result =
                new ArrayList<>();

        List<Supplier> suppliers =
                supplierRepository.findAll();

        List<PurchaseOrder> orders =
                purchaseOrderRepository.findAll();

        for (Supplier supplier : suppliers) {

            long total = 0;
            long onTime = 0;
            long late = 0;

            for (PurchaseOrder order : orders) {

                if (order.getSupplier() == null ||
                        !supplier.getId()
                                .equals(order.getSupplier().getId())) {
                    continue;
                }

                if (!"RECEIVED".equalsIgnoreCase(
                        order.getStatus())) {
                    continue;
                }

                if (order.getDeliveryDate() == null ||
                        order.getExpectedDeliveryDate() == null) {
                    continue;
                }

                total++;

                if (!order.getDeliveryDate()
                        .isAfter(
                                order.getExpectedDeliveryDate())) {

                    onTime++;

                } else {

                    late++;
                }
            }

            double reliability = 0;

            if (total > 0) {
                reliability =
                        ((double) onTime / total) * 100;
            }

            result.add(
                    new SupplierPerformance(
                            supplier.getId(),
                            supplier.getSupplierName(),
                            total,
                            onTime,
                            late,
                            reliability
                    )
            );
        }

        return result;
    }
}
