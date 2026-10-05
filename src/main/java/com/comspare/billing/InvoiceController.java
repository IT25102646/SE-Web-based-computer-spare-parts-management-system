package com.comspare.billing;

import com.comspare.order.Order;
import com.comspare.order.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Access (ADMIN, FINANCE_OFFICER) is enforced in SecurityConfig. */
@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrderService orderService;

    public InvoiceController(InvoiceService invoiceService, OrderService orderService) {
        this.invoiceService = invoiceService;
        this.orderService = orderService;
    }

    @GetMapping
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "billing/invoice-list";
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", invoiceService.getPaymentsForInvoice(id));
        model.addAttribute("balance", invoiceService.getBalance(invoice));
        model.addAttribute("methods", PaymentMethod.values());
        return "billing/invoice-detail";
    }

    // Orders that are not cancelled and do not have an invoice yet
    @GetMapping("/new")
    public String newInvoiceForm(Model model) {
        model.addAttribute("orders", orderService.getAllOrders().stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .filter(o -> invoiceService.findByOrderId(o.getId()).isEmpty())
                .toList());
        return "billing/invoice-form";
    }

    // Customer and total come from the order itself, so they cannot be mistyped
    @PostMapping("/create")
    public String createInvoice(@RequestParam Long orderId, RedirectAttributes ra) {
        try {
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found."));
            if ("CANCELLED".equals(order.getStatus())) {
                throw new IllegalArgumentException("Cannot invoice a cancelled order.");
            }
            Invoice invoice = invoiceService.generateInvoice(
                    orderId,
                    order.getCustomerName(),
                    BigDecimal.valueOf(order.getTotalAmount()).setScale(2, RoundingMode.HALF_UP));
            ra.addFlashAttribute("successMessage", "Invoice " + invoice.getInvoiceNumber() + " is ready.");
            return "redirect:/invoices/" + invoice.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/invoices/new";
        }
    }

    // Edit invoice
    @GetMapping("/{id}/edit")
    public String editInvoice(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoiceById(id));
        return "billing/invoice-edit";
    }

    @PostMapping("/{id}/update")
    public String updateInvoice(@PathVariable Long id,
                                @RequestParam String customerName,
                                @RequestParam BigDecimal totalAmount,
                                RedirectAttributes ra) {
        try {
            invoiceService.updateInvoice(id, customerName.trim(), totalAmount.setScale(2, RoundingMode.HALF_UP));
            ra.addFlashAttribute("successMessage", "Invoice updated.");
            return "redirect:/invoices/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/invoices/" + id + "/edit";
        }
    }

    // Delete invoice (with its payments)
    @PostMapping("/{id}/delete")
    public String deleteInvoice(@PathVariable Long id, RedirectAttributes ra) {
        invoiceService.deleteInvoice(id);
        ra.addFlashAttribute("successMessage", "Invoice deleted.");
        return "redirect:/invoices";
    }

    // Record payment
    @PostMapping("/{id}/pay")
    public String recordPayment(@PathVariable Long id,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod method,
                                @RequestParam(required = false) String referenceNo,
                                RedirectAttributes ra) {
        try {
            invoiceService.recordPayment(id, amount, method, referenceNo);
            ra.addFlashAttribute("successMessage", "Payment recorded.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @GetMapping("/payments")
    public String paymentHistory(Model model) {
        model.addAttribute("payments", invoiceService.getAllPayments());
        return "billing/payment-history";
    }

    @PostMapping("/payments/{paymentId}/delete")
    public String deletePayment(@PathVariable Long paymentId, RedirectAttributes ra) {
        Long invoiceId = invoiceService.deletePayment(paymentId);
        ra.addFlashAttribute("successMessage", "Payment removed and invoice recalculated.");
        return "redirect:/invoices/" + invoiceId;
    }

    @GetMapping("/payments/{paymentId}/receipt")
    public String viewReceipt(@PathVariable Long paymentId, Model model) {
        Payment payment = invoiceService.getPaymentById(paymentId);
        model.addAttribute("payment", payment);
        model.addAttribute("invoice", payment.getInvoice());
        return "billing/receipt";
    }
}
