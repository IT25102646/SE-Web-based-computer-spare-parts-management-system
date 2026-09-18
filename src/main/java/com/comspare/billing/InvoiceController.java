package com.comspare.billing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // Invoice list
    @GetMapping
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "billing/invoice-list";
    }

    // view Invoice
    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", invoiceService.getPaymentsForInvoice(id));
        model.addAttribute("balance", invoiceService.getBalance(invoice));
        model.addAttribute("methods", PaymentMethod.values());
        return "billing/invoice-detail";
    }

    // new Invoice Form
    @GetMapping("/new")
    public String newInvoiceForm() {
        return "billing/invoice-form";
    }

    // createInvoice
    @PostMapping("/create")
    public String createInvoice(@RequestParam Long orderId,
                                @RequestParam String customerName,
                                @RequestParam BigDecimal totalAmount) {
        Invoice invoice = invoiceService.generateInvoice(orderId, customerName, totalAmount);
        return "redirect:/invoices/" + invoice.getId();
    }

    // record Payment
    @PostMapping("/{id}/pay")
    public String recordPayment(@PathVariable Long id,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod method,
                                @RequestParam(required = false) String referenceNo) {
        invoiceService.recordPayment(id, amount, method, referenceNo);
        return "redirect:/invoices/" + id;
    }

    // Payment history
    @GetMapping("/payments")
    public String paymentHistory(Model model) {
        model.addAttribute("payments", invoiceService.getAllPayments());
        return "billing/payment-history";
    }
    // Delete a payment and recalculate the invoice
    @PostMapping("/payments/{paymentId}/delete")
    public String deletePayment(@PathVariable Long paymentId) {
        Long invoiceId = invoiceService.deletePayment(paymentId);
        return "redirect:/invoices/" + invoiceId;
    }
    // Show the receipt for a single payment
    @GetMapping("/payments/{paymentId}/receipt")
    public String viewReceipt(@PathVariable Long paymentId, Model model) {
        Payment payment = invoiceService.getPaymentById(paymentId);
        model.addAttribute("payment", payment);
        model.addAttribute("invoice", payment.getInvoice());
        return "billing/receipt";
    }
}