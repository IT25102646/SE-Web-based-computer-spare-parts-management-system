package com.comspare.returns;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final PartService partService;

    public ReturnRequestService(ReturnRequestRepository returnRequestRepository,
                                PartService partService) {
        this.returnRequestRepository = returnRequestRepository;
        this.partService = partService;
    }

    public List<ReturnRequest> getAllReturns() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ReturnRequest> searchReturns(String term) {
        if (term == null || term.isBlank()) {
            return getAllReturns();
        }
        return returnRequestRepository.search(term.trim());
    }

    public ReturnRequest getById(Long id) {
        return returnRequestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Return request not found with id: " + id));
    }

    public List<Part> getParts() {
        return partService.getAllParts();
    }

    @Transactional
    public ReturnRequest create(ReturnRequest request, Long partId) {
        Part part = partService.getPartById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Selected spare part was not found."));

        validateQuantity(request.getQuantity());
        request.setPart(part);
        request.setStatus(ReturnRequest.ReturnStatus.PENDING);
        request.setInventoryProcessed(false);
        return returnRequestRepository.save(request);
    }

    @Transactional
    public ReturnRequest update(Long id, ReturnRequest incoming, Long partId) {
        ReturnRequest existing = getById(id);

        if (existing.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new IllegalStateException("Only pending return requests can be edited.");
        }

        Part part = partService.getPartById(partId)
            .orElseThrow(() -> new IllegalArgumentException("Selected spare part was not found."));

        validateQuantity(incoming.getQuantity());

        existing.setCustomerName(incoming.getCustomerName());
        existing.setCustomerContact(incoming.getCustomerContact());
        existing.setPart(part);
        existing.setQuantity(incoming.getQuantity());
        existing.setReturnReason(incoming.getReturnReason());
        existing.setClaimType(incoming.getClaimType());
        existing.setResolution(incoming.getResolution());
        return returnRequestRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        ReturnRequest request = getById(id);
        if (request.getStatus() == ReturnRequest.ReturnStatus.APPROVED) {
            throw new IllegalStateException("Approved return requests cannot be deleted.");
        }
        returnRequestRepository.delete(request);
    }

    @Transactional
    public ReturnRequest approve(Long id, String notes) {
        ReturnRequest request = getById(id);

        if (request.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be approved.");
        }

        if (request.getResolution() == null) {
            throw new IllegalStateException("Select a resolution before approving the request.");
        }

        // The existing inventory module provides the agreed cross-module method.
        // For defective returns, remove the returned quantity from usable stock
        // and record the DAMAGED event in part history.
        if (isDefectiveReason(request.getReturnReason())) {
            if (request.getPart().getStockQuantity() < request.getQuantity()) {
                throw new IllegalStateException(
                    "Insufficient stock to process this defective return. Available: "
                        + request.getPart().getStockQuantity()
                        + ", requested: " + request.getQuantity());
            }
            partService.markAsDamagedFromReturn(
                request.getPart().getId(),
                request.getQuantity(),
                request.getReturnReason());
            request.setInventoryProcessed(true);
        }

        request.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        request.setDecisionNotes(cleanNotes(notes));
        return returnRequestRepository.save(request);
    }

    @Transactional
    public ReturnRequest reject(Long id, String notes) {
        ReturnRequest request = getById(id);
        if (request.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected.");
        }
        request.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        request.setDecisionNotes(cleanNotes(notes));
        return returnRequestRepository.save(request);
    }

    @Transactional
    public ReturnRequest cancel(Long id) {
        ReturnRequest request = getById(id);
        if (request.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be cancelled.");
        }
        request.setStatus(ReturnRequest.ReturnStatus.CANCELLED);
        return returnRequestRepository.save(request);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
    }

    private boolean isDefectiveReason(String reason) {
        if (reason == null) return false;
        String value = reason.toLowerCase();
        return value.contains("defective") || value.contains("damaged") || value.contains("faulty");
    }

    private String cleanNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
