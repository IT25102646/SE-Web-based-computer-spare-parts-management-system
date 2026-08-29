package com.comspare.inventory;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    // UC-I04: View Stock (list + search)
    @GetMapping
    public String listParts(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("parts", partService.searchParts(search));
        model.addAttribute("lowStockCount", partService.countLowStockParts());
        model.addAttribute("searchTerm", search);
        model.addAttribute("pageTitle", "Inventory");
        return "inventory/part-list";
    }

    // UC-I01: Add Spare Part
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("part", new Part());
        model.addAttribute("pageTitle", "Add spare part");
        return "inventory/part-form";
    }

    @PostMapping("/save")
    public String savePart(@Valid @ModelAttribute("part") Part part,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Add spare part");
            return "inventory/part-form";
        }
        try {
            Part saved = partService.addPart(part);
            redirectAttributes.addFlashAttribute("successMessage",
                "Part '" + saved.getName() + "' added successfully.");
            if (saved.isLowStock()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                    "Note: this part is already at or below its reorder level.");
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Add spare part");
            return "inventory/part-form";
        }
        return "redirect:/parts";
    }

    // UC-I02: Update Spare Part
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Part part = partService.getPartById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
        model.addAttribute("part", part);
        model.addAttribute("pageTitle", "Edit spare part");
        return "inventory/part-form";
    }

    @PostMapping("/update/{id}")
    public String updatePart(@PathVariable Long id,
                              @Valid @ModelAttribute("part") Part part,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit spare part");
            return "inventory/part-form";
        }
        partService.updatePart(id, part);
        redirectAttributes.addFlashAttribute("successMessage", "Part updated successfully.");
        return "redirect:/parts";
    }

    // UC-I03: Delete Spare Part
    @PostMapping("/delete/{id}")
    public String deletePart(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        partService.deletePart(id);
        redirectAttributes.addFlashAttribute("successMessage", "Part deleted successfully.");
        return "redirect:/parts";
    }

    // UC-I05: Adjust Stock
    @GetMapping("/adjust/{id}")
    public String showAdjustForm(@PathVariable Long id, Model model) {
        Part part = partService.getPartById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
        model.addAttribute("part", part);
        model.addAttribute("pageTitle", "Adjust stock");
        return "inventory/stock-adjustment-form";
    }

    @PostMapping("/adjust/{id}")
    public String submitAdjustment(@PathVariable Long id,
                                    @RequestParam Integer changeAmount,
                                    @RequestParam String reason,
                                    @RequestParam(required = false) String adjustedBy,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            partService.adjustStock(id, changeAmount, reason,
                (adjustedBy == null || adjustedBy.isBlank()) ? "Unspecified staff" : adjustedBy);
            redirectAttributes.addFlashAttribute("successMessage", "Stock adjustment recorded.");
            return "redirect:/parts";
        } catch (IllegalArgumentException e) {
            Part part = partService.getPartById(id).orElseThrow();
            model.addAttribute("part", part);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Adjust stock");
            return "inventory/stock-adjustment-form";
        }
    }

    // UC-I06: View Part History
    @GetMapping("/history/{id}")
    public String viewHistory(@PathVariable Long id, Model model) {
        Part part = partService.getPartById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
        model.addAttribute("part", part);
        model.addAttribute("historyEntries", partService.getPartHistory(id));
        model.addAttribute("pageTitle", "Part history — " + part.getName());
        return "inventory/part-history";
    }
}
