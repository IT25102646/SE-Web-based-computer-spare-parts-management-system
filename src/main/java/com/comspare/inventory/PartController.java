package com.comspare.inventory;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    // ---------- list (active, or the discontinued view) ----------
    @GetMapping
    public String listParts(@RequestParam(required = false) String search,
                            @RequestParam(defaultValue = "false") boolean discontinued,
                            Model model) {
        model.addAttribute("parts", partService.searchParts(search, discontinued));
        model.addAttribute("lowStockCount", partService.countLowStockParts());
        model.addAttribute("searchTerm", search);
        model.addAttribute("discontinued", discontinued);
        model.addAttribute("pageTitle", "Inventory");
        return "inventory/part-list";
    }

    // ---------- add ----------
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("part", new Part());
        model.addAttribute("pageTitle", "Add spare part");
        return "inventory/part-form";
    }

    @PostMapping("/save")
    public String savePart(@Valid @ModelAttribute("part") Part part,
                           BindingResult bindingResult,
                           @RequestParam(defaultValue = "") String reason,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return form(model, "Add spare part", reason, null);
        }
        try {
            Part saved = partService.addPart(part, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Part '" + saved.getName() + "' added successfully.");
            if (saved.isLowStock()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Note: this part is already at or below its reorder level.");
            }
            return "redirect:/parts";
        } catch (IllegalArgumentException e) {
            return form(model, "Add spare part", reason, e.getMessage());
        }
    }

    // ---------- edit ----------
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
                             @RequestParam(defaultValue = "") String reason,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        part.setId(id); // keeps the form in "edit" mode if something fails
        if (bindingResult.hasErrors()) {
            return form(model, "Edit spare part", reason, null);
        }
        try {
            partService.updatePart(id, part, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Part updated successfully.");
            return "redirect:/parts";
        } catch (IllegalArgumentException e) {
            return form(model, "Edit spare part", reason, e.getMessage());
        }
    }

    // ---------- delete = discontinue (needs a reason) ----------
    @GetMapping("/delete/{id}")
    public String showDeleteForm(@PathVariable Long id, Model model) {
        Part part = partService.getPartById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
        model.addAttribute("part", part);
        model.addAttribute("pageTitle", "Delete spare part");
        return "inventory/part-delete-form";
    }

    @PostMapping("/delete/{id}")
    public String deletePart(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String reason,
                             @RequestParam(defaultValue = "false") boolean writeOff,
                             Principal principal,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        try {
            partService.discontinuePart(id, reason, writeOff, principal != null ? principal.getName() : null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Part deleted (discontinued). You can find it under \"Show discontinued\".");
            return "redirect:/parts";
        } catch (IllegalArgumentException | IllegalStateException e) {
            Part part = partService.getPartById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
            model.addAttribute("part", part);
            model.addAttribute("reason", reason);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Delete spare part");
            return "inventory/part-delete-form";
        }
    }

    @PostMapping("/restore/{id}")
    public String restorePart(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            partService.restorePart(id);
            redirectAttributes.addFlashAttribute("successMessage", "Part restored to the active inventory.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parts";
    }

    // ---------- adjust stock ----------
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
                                   Principal principal,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        String who = (adjustedBy == null || adjustedBy.isBlank())
                ? (principal != null ? principal.getName() : "Unspecified staff")
                : adjustedBy;
        try {
            partService.adjustStock(id, changeAmount, reason, who);
            redirectAttributes.addFlashAttribute("successMessage", "Stock adjustment recorded.");
            return "redirect:/parts";
        } catch (IllegalArgumentException e) {
            Part part = partService.getPartById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
            model.addAttribute("part", part);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Adjust stock");
            return "inventory/stock-adjustment-form";
        }
    }

    // ---------- history ----------
    @GetMapping("/history/{id}")
    public String viewHistory(@PathVariable Long id, Model model) {
        Part part = partService.getPartById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid part id: " + id));
        model.addAttribute("part", part);
        model.addAttribute("historyEntries", partService.getPartHistory(id));
        model.addAttribute("pageTitle", "Part history - " + part.getName());
        return "inventory/part-history";
    }

    private String form(Model model, String title, String reason, String error) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("reason", reason);
        if (error != null) model.addAttribute("errorMessage", error);
        return "inventory/part-form";
    }
}
