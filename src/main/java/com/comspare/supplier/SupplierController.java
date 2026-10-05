package com.comspare.supplier;

import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "supplier/supplier-list";
    }

    @GetMapping("/new")
    public String showSupplierForm(Model model) {
        Supplier supplier = new Supplier();
        supplier.setStatus("ACTIVE");
        model.addAttribute("supplier", supplier);
        return "supplier/supplier-form";
    }

    @GetMapping("/edit/{id}")
    public String editSupplier(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.getSupplierById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
        model.addAttribute("supplier", supplier);
        return "supplier/supplier-form";
    }

    @PostMapping("/save")
    public String saveSupplier(@Valid @ModelAttribute("supplier") Supplier supplier,
                               BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "supplier/supplier-form";
        }
        try {
            supplierService.saveSupplier(supplier);
            ra.addFlashAttribute("successMessage", "Supplier saved.");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "supplier/supplier-form";
        }
    }

    // Delete = POST. The service refuses if the supplier has purchase orders.
    @PostMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Long id, RedirectAttributes ra) {
        try {
            supplierService.deleteSupplier(id);
            ra.addFlashAttribute("successMessage", "Supplier deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage() + " Set it to INACTIVE instead.");
        } catch (DataAccessException e) {
            ra.addFlashAttribute("errorMessage", "Cannot delete: this supplier is in use.");
        }
        return "redirect:/suppliers";
    }
}
