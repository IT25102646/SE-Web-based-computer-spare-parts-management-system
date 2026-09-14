package com.comspare.returns;

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

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("returns", returnRequestService.searchReturns(search));
        model.addAttribute("searchTerm", search);
        model.addAttribute("pageTitle", "Returns & Warranty Claims");
        return "returns/return-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("returnRequest", new ReturnRequest());
        model.addAttribute("parts", returnRequestService.getParts());
        model.addAttribute("pageTitle", "New Return / Warranty Claim");
        return "returns/return-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("returnRequest") ReturnRequest request,
                       BindingResult bindingResult,
                       @RequestParam Long partId,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("parts", returnRequestService.getParts());
            model.addAttribute("selectedPartId", partId);
            model.addAttribute("pageTitle", "New Return / Warranty Claim");
            return "returns/return-form";
        }
        try {
            ReturnRequest saved = returnRequestService.create(request, partId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Return request #" + saved.getId() + " created successfully.");
            return "redirect:/returns";
        } catch (IllegalArgumentException e) {
            model.addAttribute("parts", returnRequestService.getParts());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "New Return / Warranty Claim");
            return "returns/return-form";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        ReturnRequest request = returnRequestService.getById(id);
        model.addAttribute("returnRequest", request);
        model.addAttribute("parts", returnRequestService.getParts());
        model.addAttribute("selectedPartId", request.getPart().getId());
        model.addAttribute("pageTitle", "Edit Return Request #" + id);
        return "returns/return-form";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("returnRequest") ReturnRequest request,
                         BindingResult bindingResult,
                         @RequestParam Long partId,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("parts", returnRequestService.getParts());
            model.addAttribute("selectedPartId", partId);
            model.addAttribute("pageTitle", "Edit Return Request #" + id);
            return "returns/return-form";
        }
        try {
            returnRequestService.update(id, request, partId);
            redirectAttributes.addFlashAttribute("successMessage", "Return request updated successfully.");
            return "redirect:/returns";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("parts", returnRequestService.getParts());
            model.addAttribute("selectedPartId", partId);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Edit Return Request #" + id);
            return "returns/return-form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("returnRequest", returnRequestService.getById(id));
        model.addAttribute("pageTitle", "Return Request #" + id);
        return "returns/return-details";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String notes,
                          RedirectAttributes redirectAttributes) {
        try {
            returnRequestService.approve(id, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Return request approved successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/returns/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes redirectAttributes) {
        try {
            returnRequestService.reject(id, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Return request rejected.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/returns/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            returnRequestService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Return request cancelled.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/returns/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            returnRequestService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Return request deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/returns";
    }
}
