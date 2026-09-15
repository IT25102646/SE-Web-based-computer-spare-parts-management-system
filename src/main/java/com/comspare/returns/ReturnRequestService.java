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

    public ReturnRequestService(
            ReturnRequestRepository returnRequestRepository,
            PartService partService) {

        this.returnRequestRepository = returnRequestRepository;
        this.partService = partService;
    }

    // ================= CREATE =================

    @Transactional
    public ReturnRequest createReturnRequest(ReturnRequest request) {

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        if (request.getPart() == null || request.getPart().getId() == null) {
            throw new IllegalArgumentException(
                    "A spare part must be selected.");
        }

        Part part = partService.getPartById(request.getPart().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Selected part was not found."));

        request.setPart(part);
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
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Return request not found with id: " + id));
    }

    public List<ReturnRequest> searchReturns(
            String search,
            String status,
            String claimType) {

        List<ReturnRequest> returns =
                returnRequestRepository.findAllByOrderByCreatedAtDesc();

        return returns.stream()
                .filter(r ->
                        search == null ||
                        search.isBlank() ||
                        r.getCustomerName()
                                .toLowerCase()
                                .contains(search.toLowerCase()) ||
                        r.getCustomerContact()
                                .toLowerCase()
                                .contains(search.toLowerCase()))
                .filter(r ->
                        status == null ||
                        status.isBlank() ||
                        status.equals(r.getStatus()))
                .filter(r ->
                        claimType == null ||
                        claimType.isBlank() ||
                        claimType.equals(r.getClaimType()))
                .toList();
    }

    // ================= UPDATE =================

    @Transactional
    public ReturnRequest updateReturnRequest(
            Long id,
            ReturnRequest updated) {

        ReturnRequest existing = getReturnById(id);

        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalStateException(
                    "Only pending return requests can be edited.");
        }

        if (updated.getQuantity() == null ||
                updated.getQuantity() <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        Part part = partService.getPartById(
                        updated.getPart().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected part was not found."));

        existing.setCustomerName(updated.getCustomerName());
        existing.setCustomerContact(updated.getCustomerContact());
        existing.setPart(part);
        existing.setQuantity(updated.getQuantity());
        existing.setReturnReason(updated.getReturnReason());
        existing.setClaimType(updated.getClaimType());
        existing.setResolution(updated.getResolution());

        return returnRequestRepository.save(existing);
    }

    // ================= DELETE =================

    @Transactional
    public void deleteReturnRequest(Long id) {

        ReturnRequest existing = getReturnById(id);

        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalStateException(
                    "Only pending return requests can be deleted.");
        }

        returnRequestRepository.delete(existing);
    }

    // ================= APPROVE =================

    @Transactional
    public ReturnRequest approveReturn(
            Long id,
            String decisionNotes) {

        ReturnRequest request = getReturnById(id);

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending claims can be approved.");
        }

        request.setStatus("APPROVED");
        request.setDecisionNotes(decisionNotes);

        /*
         * Integration with the existing Inventory module.
         *
         * If the returned item is defective/damaged,
         * update inventory through PartService.
         */
        String reason = request.getReturnReason();

        if (isDefectiveReason(reason)) {

            partService.markAsDamagedFromReturn(
                    request.getPart().getId(),
                    request.getQuantity(),
                    reason
            );

            request.setInventoryProcessed(true);
        }

        return returnRequestRepository.save(request);
    }

    // ================= REJECT =================

    @Transactional
    public ReturnRequest rejectReturn(
            Long id,
            String decisionNotes) {

        ReturnRequest request = getReturnById(id);

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending claims can be rejected.");
        }

        request.setStatus("REJECTED");
        request.setDecisionNotes(decisionNotes);

        return returnRequestRepository.save(request);
    }

    // ================= CANCEL =================

    @Transactional
    public ReturnRequest cancelReturn(Long id) {

        ReturnRequest request = getReturnById(id);

        if ("COMPLETED".equals(request.getStatus()) ||
                "REJECTED".equals(request.getStatus())) {

            throw new IllegalStateException(
                    "This claim cannot be cancelled.");
        }

        request.setStatus("CANCELLED");

        return returnRequestRepository.save(request);
    }

    // ================= HELPER =================

    private boolean isDefectiveReason(String reason) {

        if (reason == null) {
            return false;
        }

        String value = reason.toLowerCase();

        return value.contains("defect")
                || value.contains("damage")
                || value.contains("broken")
                || value.contains("not working")
                || value.contains("fault");
    }
}
