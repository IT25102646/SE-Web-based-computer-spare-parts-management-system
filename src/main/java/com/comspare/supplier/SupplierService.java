package com.comspare.supplier;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierService(
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository) {

        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    public Supplier saveSupplier(Supplier supplier) {

        if (supplier.getSupplierName() == null ||
                supplier.getSupplierName().isBlank()) {

            throw new IllegalArgumentException(
                    "Supplier name is required.");
        }

        if (supplier.getStatus() == null ||
                supplier.getStatus().isBlank()) {

            supplier.setStatus("ACTIVE");
        }

        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {

        if (!supplierRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Supplier not found.");
        }

        if (purchaseOrderRepository.existsBySupplierId(id)) {
            throw new IllegalStateException(
                    "Cannot delete a supplier with existing purchase orders.");
        }

        supplierRepository.deleteById(id);
    }
}