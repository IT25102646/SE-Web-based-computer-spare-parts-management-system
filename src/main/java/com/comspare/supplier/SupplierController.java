package com.comspare.supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(
            SupplierService supplierService) {

        this.supplierService = supplierService;
    }

    @GetMapping
    public String listSuppliers(Model model) {

        model.addAttribute(
                "suppliers",
                supplierService.getAllSuppliers()
        );

        return "supplier/supplier-list";
    }

    @GetMapping("/new")
    public String showSupplierForm(Model model) {

        model.addAttribute(
                "supplier",
                new Supplier()
        );

        return "supplier/supplier-form";
    }

    @GetMapping("/edit/{id}")
    public String editSupplier(
            @PathVariable Long id,
            Model model) {

        Supplier supplier =
                supplierService.getSupplierById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Supplier not found"));

        model.addAttribute(
                "supplier",
                supplier
        );

        return "supplier/supplier-form";
    }

    @PostMapping("/save")
    public String saveSupplier(
            @ModelAttribute Supplier supplier) {

        supplierService.saveSupplier(supplier);

        return "redirect:/suppliers";
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(
            @PathVariable Long id) {

        supplierService.deleteSupplier(id);

        return "redirect:/suppliers";
    }
}