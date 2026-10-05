package com.comspare.returns;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ================= CREATE =================

    @Transactional
    public ReturnRequest createReturnRequest(ReturnRequest request) {
        validate(request);
        request.setPart(loadPart(request));
        request.setStatus("PENDING");
        request.setInventoryProcessed(false);
        return returnRequestRepository.save(request);
    }

    // ================= READ =================

    public List<ReturnRequest> getAllReturns() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public ReturnRequest getReturnById(Long id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found with id: " + id));
    }

    public List<ReturnRequest> searchReturns(String search, String status, String claimType) {
        String s = search == null ? "" : search.trim().toLowerCase();
        return returnRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> s.isEmpty()
                        || r.getCustomerName().toLowerCase().contains(s)
                        || r.getCustomerContact().toLowerCase().contains(s))
                .filter(r -> status == null || status.isBlank() || status.equals(r.getStatus()))
                .filter(r -> claimType == null || claimType.isBlank() || claimType.equals(r.getClaimType()))
                .toList();
    }

    // ================= UPDATE (only while PENDING) =================

    @Transactional
    public ReturnRequest updateReturnRequest(Long id, ReturnRequest updated) {
        ReturnRequest existing = getReturnById(id);
        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalStateException("Only pending return requests can be edited.");
        }
        validate(updated);

        existing.setCustomerName(updated.getCustomerName());
        existing.setCustomerContact(updated.getCustomerContact());
        existing.setPart(loadPart(updated));
        existing.setQuantity(updated.getQuantity());
        existing.setReturnReason(updated.getReturnReason());
        existing.setClaimType(updated.getClaimType());
        existing.setResolution(updated.getResolution());
        return returnRequestRepository.save(existing);
    }

    // ================= DELETE (only while PENDING) =================

    @Transactional
    public void deleteReturnRequest(Long id) {
        ReturnRequest existing = getReturnById(id);
        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalStateException("Only pending return requests can be deleted.");
        }
        returnRequestRepository.delete(existing);
    }

    // ================= APPROVE =================

    /**
     * Approving a claim updates inventory once:
     *  - defective item / warranty claim -> logged as DAMAGED (loss report), stock unchanged
     *  - good item returned              -> added back to stock, logged as RETURNED
     */
    @Transactional
    public ReturnRequest approveReturn(Long id, String decisionNotes, String resolution) {
        ReturnRequest request = getReturnById(id);
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only pending claims can be approved.");
        }

        request.setStatus("APPROVED");
        request.setDecisionNotes(decisionNotes);
        if (resolution != null && !resolution.isBlank()) {
            request.setResolution(resolution);
        }

        Long partId = request.getPart().getId();
        String reason = request.getReturnReason();

        if ("WARRANTY".equals(request.getClaimType()) || isDefectiveReason(reason)) {
            partService.markAsDamagedFromReturn(partId, request.getQuantity(), reason);
        } else {
            partService.restockFromReturn(partId, request.getQuantity(),
                    "Return #" + request.getId() + " approved: " + reason);
        }
        request.setInventoryProcessed(true);
        return returnRequestRepository.save(request);
    }

    // ================= REJECT =================

    @Transactional
    public ReturnRequest rejectReturn(Long id, String decisionNotes) {
        ReturnRequest request = getReturnById(id);
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only pending claims can be rejected.");
        }
        request.setStatus("REJECTED");
        request.setDecisionNotes(decisionNotes);
        return returnRequestRepository.save(request);
    }

    // ================= COMPLETE =================

    @Transactional
    public ReturnRequest completeReturn(Long id) {
        ReturnRequest request = getReturnById(id);
        if (!"APPROVED".equals(request.getStatus())) {
            throw new IllegalStateException("Only approved claims can be marked as completed.");
        }
        request.setStatus("COMPLETED");
        return returnRequestRepository.save(request);
    }

    // ================= CANCEL =================

    @Transactional
    public ReturnRequest cancelReturn(Long id) {
        ReturnRequest request = getReturnById(id);
        if (!"PENDING".equals(request.getStatus()) && !"PROCESSING".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending or processing claims can be cancelled (inventory is updated once a claim is approved).");
        }
        request.setStatus("CANCELLED");
        return returnRequestRepository.save(request);
    }

    // ================= helpers =================

    private void validate(ReturnRequest r) {
        if (r.getQuantity() == null || r.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (r.getPart() == null || r.getPart().getId() == null) {
            throw new IllegalArgumentException("A spare part must be selected.");
        }
    }

    private Part loadPart(ReturnRequest r) {
        return partService.getPartById(r.getPart().getId())
                .orElseThrow(() -> new IllegalArgumentException("Selected part was not found."));
    }

    private boolean isDefectiveReason(String reason) {
        if (reason == null) return false;
        String v = reason.toLowerCase();
        return v.contains("defect") || v.contains("damage") || v.contains("broken")
                || v.contains("not working") || v.contains("fault");
    }
}
