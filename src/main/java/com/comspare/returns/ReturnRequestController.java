package com.comspare.returns;

import com.comspare.inventory.PartService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/returns")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;
    private final PartService partService;

    public ReturnRequestController(
            ReturnRequestService returnRequestService,
            PartService partService) {

        this.returnRequestService = returnRequestService;
        this.partService = partService;
    }

    // ================= READ / LIST =================

    @GetMapping
    public String listReturns(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String claimType,
            Model model) {

        model.addAttribute(
                "returns",
                returnRequestService.searchReturns(
                        search,
                        status,
                        claimType));

        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("claimType", claimType);
        model.addAttribute("pageTitle", "Returns & Warranty Claims");

        return "returns/return-list";
    }

    // ================= CREATE FORM =================

    @GetMapping("/new")
    public String showCreateForm(Model model) {

        model.addAttribute(
                "returnRequest",
                new ReturnRequest());

        model.addAttribute(
                "parts",
                partService.getAllParts());

        model.addAttribute(
                "pageTitle",
                "Create Return / Warranty Claim");

        return "returns/return-form";
    }

    // ================= CREATE =================

    @PostMapping("/save")
    public String saveReturn(
            @Valid @ModelAttribute("returnRequest")
            ReturnRequest returnRequest,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "parts",
                    partService.getAllParts());

            model.addAttribute(
                    "pageTitle",
                    "Create Return / Warranty Claim");

            return "returns/return-form";
        }

        try {

            returnRequestService.createReturnRequest(
                    returnRequest);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Return request created successfully.");

            return "redirect:/returns";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "parts",
                    partService.getAllParts());

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            model.addAttribute(
                    "pageTitle",
                    "Create Return / Warranty Claim");

            return "returns/return-form";
        }
    }

    // ================= READ / DETAILS =================

    @GetMapping("/view/{id}")
    public String viewReturn(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "returnRequest",
                returnRequestService.getReturnById(id));

        model.addAttribute(
                "pageTitle",
                "Return Claim Details");

        return "returns/return-details";
    }

    // ================= UPDATE FORM =================

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "returnRequest",
                returnRequestService.getReturnById(id));

        model.addAttribute(
                "parts",
                partService.getAllParts());

        model.addAttribute(
                "pageTitle",
                "Edit Return Claim");

        return "returns/return-form";
    }

    // ================= UPDATE =================

    @PostMapping("/update/{id}")
    public String updateReturn(
            @PathVariable Long id,

            @Valid @ModelAttribute("returnRequest")
            ReturnRequest returnRequest,

            BindingResult bindingResult,

            Model model,

            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "parts",
                    partService.getAllParts());

            model.addAttribute(
                    "pageTitle",
                    "Edit Return Claim");

            return "returns/return-form";
        }

        try {

            returnRequestService.updateReturnRequest(
                    id,
                    returnRequest);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Return request updated successfully.");

            return "redirect:/returns";

        } catch (IllegalArgumentException |
                 IllegalStateException e) {

            model.addAttribute(
                    "parts",
                    partService.getAllParts());

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            model.addAttribute(
                    "pageTitle",
                    "Edit Return Claim");

            return "returns/return-form";
        }
    }

    // ================= DELETE =================

    @PostMapping("/delete/{id}")
    public String deleteReturn(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            returnRequestService.deleteReturnRequest(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Return request deleted successfully.");

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/returns";
    }

    // ================= APPROVE =================

    @PostMapping("/approve/{id}")
    public String approveReturn(
            @PathVariable Long id,

            @RequestParam(required = false)
            String decisionNotes,

            RedirectAttributes redirectAttributes) {

        try {

            returnRequestService.approveReturn(
                    id,
                    decisionNotes);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Return claim approved successfully.");

        } catch (IllegalStateException |
                 IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/returns/view/" + id;
    }

    // ================= REJECT =================

    @PostMapping("/reject/{id}")
    public String rejectReturn(
            @PathVariable Long id,

            @RequestParam(required = false)
            String decisionNotes,

            RedirectAttributes redirectAttributes) {

        try {

            returnRequestService.rejectReturn(
                    id,
                    decisionNotes);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Return claim rejected.");

        } catch (IllegalStateException |
                 IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/returns/view/" + id;
    }

    // ================= CANCEL =================

    @PostMapping("/cancel/{id}")
    public String cancelReturn(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            returnRequestService.cancelReturn(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Return claim cancelled.");

        } catch (IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/returns";
    }
}
