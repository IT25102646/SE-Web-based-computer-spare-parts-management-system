package com.comspare.supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/supplier-performance")
public class SupplierPerformanceController {

    private final SupplierPerformanceService performanceService;

    public SupplierPerformanceController(
            SupplierPerformanceService performanceService) {

        this.performanceService = performanceService;
    }

    @GetMapping
    public String performance(Model model) {

        model.addAttribute(
                "performance",
                performanceService.getPerformance()
        );

        return "supplier/supplier-performance";
    }
}