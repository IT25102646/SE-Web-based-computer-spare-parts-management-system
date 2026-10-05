package com.comspare.supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                   SupplierService supplierService) {
        this.purchaseOrderService = purchaseOrderService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String listPurchaseOrders(Model model) {
        model.addAttribute("purchaseOrders", purchaseOrderService.getAllPurchaseOrders());
        return "supplier/purchase-order-list";
    }

    @GetMapping("/view/{id}")
    public String viewPurchaseOrder(@PathVariable Long id, Model model) {
        PurchaseOrder po = purchaseOrderService.getPurchaseOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        model.addAttribute("purchaseOrder", po);
        model.addAttribute("partNames", purchaseOrderService.getPartNames());
        model.addAttribute("total", purchaseOrderService.calculateTotal(po));
        return "supplier/purchase-order-view";
    }

    @GetMapping("/new")
    public String showPurchaseOrderForm(Model model) {
        PurchaseOrder po = new PurchaseOrder();
        po.setOrderNumber(purchaseOrderService.generateOrderNumber());
        po.setOrderDate(LocalDate.now());
        po.setStatus("PENDING");
        return form(po, model);
    }

    @GetMapping("/edit/{id}")
    public String editPurchaseOrder(@PathVariable Long id, Model model, RedirectAttributes ra) {
        PurchaseOrder po = purchaseOrderService.getPurchaseOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));
        if (!PurchaseOrderService.EDITABLE_STATUSES.contains(po.getStatus())) {
            ra.addFlashAttribute("errorMessage", "Received or cancelled purchase orders cannot be edited.");
            return "redirect:/purchase-orders";
        }
        return form(po, model);
    }

    @PostMapping("/save")
    public String savePurchaseOrder(@ModelAttribute PurchaseOrder purchaseOrder,
                                    Model model, RedirectAttributes ra) {
        try {
            purchaseOrderService.savePurchaseOrder(purchaseOrder);
            ra.addFlashAttribute("successMessage", "Purchase order saved.");
            return "redirect:/purchase-orders";
        } catch (IllegalArgumentException | IllegalStateException e) {
            purchaseOrder.getItems().removeIf(i -> i.getPartId() == null);
            model.addAttribute("errorMessage", e.getMessage());
            return form(purchaseOrder, model);
        }
    }

    @PostMapping("/confirm/{id}")
    public String confirmDelivery(@PathVariable Long id, RedirectAttributes ra) {
        try {
            purchaseOrderService.confirmDelivery(id);
            ra.addFlashAttribute("successMessage", "Delivery confirmed and stock updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchase-orders";
    }

    @PostMapping("/cancel/{id}")
    public String cancelPurchaseOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            purchaseOrderService.cancelPurchaseOrder(id);
            ra.addFlashAttribute("successMessage", "Purchase order cancelled.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchase-orders";
    }

    @PostMapping("/delete/{id}")
    public String deletePurchaseOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            purchaseOrderService.deletePurchaseOrder(id);
            ra.addFlashAttribute("successMessage", "Purchase order deleted.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchase-orders";
    }

    private String form(PurchaseOrder po, Model model) {
        model.addAttribute("purchaseOrder", po);
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("partNames", purchaseOrderService.getPartNames());
        model.addAttribute("partOptions", purchaseOrderService.getActivePartNames());
        return "supplier/purchase-order-form";
    }
}
