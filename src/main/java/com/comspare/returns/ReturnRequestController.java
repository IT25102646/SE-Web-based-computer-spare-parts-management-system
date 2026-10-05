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

    public ReturnRequestController(ReturnRequestService returnRequestService, PartService partService) {
        this.returnRequestService = returnRequestService;
        this.partService = partService;
    }

    @GetMapping
    public String listReturns(@RequestParam(required = false) String search,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String claimType,
                              Model model) {
        model.addAttribute("returns", returnRequestService.searchReturns(search, status, claimType));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("claimType", claimType);
        model.addAttribute("pageTitle", "Returns & Warranty Claims");
        return "returns/return-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        return form(new ReturnRequest(), "Create Return / Warranty Claim", model);
    }

    @PostMapping("/save")
    public String saveReturn(@Valid @ModelAttribute("returnRequest") ReturnRequest returnRequest,
                             BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            return form(returnRequest, "Create Return / Warranty Claim", model);
        }
        try {
            returnRequestService.createReturnRequest(returnRequest);
            ra.addFlashAttribute("successMessage", "Return request created successfully.");
            return "redirect:/returns";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return form(returnRequest, "Create Return / Warranty Claim", model);
        }
    }

    @GetMapping("/view/{id}")
    public String viewReturn(@PathVariable Long id, Model model) {
        model.addAttribute("returnRequest", returnRequestService.getReturnById(id));
        model.addAttribute("pageTitle", "Return Claim Details");
        return "returns/return-details";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        return form(returnRequestService.getReturnById(id), "Edit Return Claim", model);
    }

    @PostMapping("/update/{id}")
    public String updateReturn(@PathVariable Long id,
                               @Valid @ModelAttribute("returnRequest") ReturnRequest returnRequest,
                               BindingResult bindingResult, Model model, RedirectAttributes ra) {
        returnRequest.setId(id); // keeps the form in "edit" mode if there is an error
        if (bindingResult.hasErrors()) {
            return form(returnRequest, "Edit Return Claim", model);
        }
        try {
            returnRequestService.updateReturnRequest(id, returnRequest);
            ra.addFlashAttribute("successMessage", "Return request updated successfully.");
            return "redirect:/returns";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return form(returnRequest, "Edit Return Claim", model);
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteReturn(@PathVariable Long id, RedirectAttributes ra) {
        try {
            returnRequestService.deleteReturnRequest(id);
            ra.addFlashAttribute("successMessage", "Return request deleted successfully.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/returns";
    }

    @PostMapping("/approve/{id}")
    public String approveReturn(@PathVariable Long id,
                                @RequestParam(required = false) String decisionNotes,
                                @RequestParam(required = false) String resolution,
                                RedirectAttributes ra) {
        return action(id, ra, () -> returnRequestService.approveReturn(id, decisionNotes, resolution),
                "Return claim approved and inventory updated.");
    }

    @PostMapping("/reject/{id}")
    public String rejectReturn(@PathVariable Long id,
                               @RequestParam(required = false) String decisionNotes,
                               RedirectAttributes ra) {
        return action(id, ra, () -> returnRequestService.rejectReturn(id, decisionNotes),
                "Return claim rejected.");
    }

    @PostMapping("/complete/{id}")
    public String completeReturn(@PathVariable Long id, RedirectAttributes ra) {
        return action(id, ra, () -> returnRequestService.completeReturn(id),
                "Return claim marked as completed.");
    }

    @PostMapping("/cancel/{id}")
    public String cancelReturn(@PathVariable Long id, RedirectAttributes ra) {
        return action(id, ra, () -> returnRequestService.cancelReturn(id),
                "Return claim cancelled.");
    }

    private String action(Long id, RedirectAttributes ra, Runnable work, String okMessage) {
        try {
            work.run();
            ra.addFlashAttribute("successMessage", okMessage);
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/returns/view/" + id;
    }

    private String form(ReturnRequest request, String title, Model model) {
        model.addAttribute("returnRequest", request);
        model.addAttribute("parts", partService.getAllParts());
        model.addAttribute("pageTitle", title);
        return "returns/return-form";
    }
}