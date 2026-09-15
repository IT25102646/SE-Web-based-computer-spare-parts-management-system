package com.comspare.supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    private final SupplierService supplierService;

    public PurchaseOrderController(
            PurchaseOrderService purchaseOrderService,
            SupplierService supplierService) {

        this.purchaseOrderService = purchaseOrderService;

        this.supplierService = supplierService;
    }

    @GetMapping
    public String listPurchaseOrders(Model model) {

        model.addAttribute(
                "purchaseOrders",
                purchaseOrderService.getAllPurchaseOrders()
        );

        return "supplier/purchase-order-list";
    }

    @GetMapping("/new")
    public String showPurchaseOrderForm(Model model) {

        PurchaseOrder purchaseOrder =
                new PurchaseOrder();

        purchaseOrder.setStatus("PENDING");

        model.addAttribute(
                "purchaseOrder",
                purchaseOrder
        );

        model.addAttribute(
                "suppliers",
                supplierService.getAllSuppliers()
        );

        return "supplier/purchase-order-form";
    }

    @GetMapping("/edit/{id}")
    public String editPurchaseOrder(
            @PathVariable Long id,
            Model model) {

        PurchaseOrder purchaseOrder =
                purchaseOrderService
                        .getPurchaseOrderById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Purchase order not found"));

        model.addAttribute(
                "purchaseOrder",
                purchaseOrder
        );

        model.addAttribute(
                "suppliers",
                supplierService.getAllSuppliers()
        );

        return "supplier/purchase-order-form";
    }

    @PostMapping("/save")
    public String savePurchaseOrder(
            @ModelAttribute PurchaseOrder purchaseOrder) {

        purchaseOrderService.savePurchaseOrder(
                purchaseOrder
        );

        return "redirect:/purchase-orders";
    }

    @PostMapping("/confirm/{id}")
    public String confirmDelivery(
            @PathVariable Long id) {

        purchaseOrderService.confirmDelivery(id);

        return "redirect:/purchase-orders";
    }

    @PostMapping("/cancel/{id}")
    public String cancelPurchaseOrder(
            @PathVariable Long id) {

        purchaseOrderService.cancelPurchaseOrder(id);

        return "redirect:/purchase-orders";
    }
}