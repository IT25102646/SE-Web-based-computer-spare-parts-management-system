package com.comspare.supplier;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryStockService {

    private final PartRepository partRepository;

    public InventoryStockService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public void increaseStock(Long partId, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Received quantity must be greater than zero.");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Part not found: " + partId));

        Integer currentStock = part.getStockQuantity();

        if (currentStock == null) {
            currentStock = 0;
        }

        part.setStockQuantity(currentStock + quantity);

        partRepository.save(part);
    }
}
