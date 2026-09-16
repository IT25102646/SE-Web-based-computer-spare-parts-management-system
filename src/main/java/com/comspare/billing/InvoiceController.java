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

    // Invoice ලැයිස්තුව
    @GetMapping
    public String listInvoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "billing/invoice-list";
    }

    // එක invoice එකක විස්තර
    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", invoiceService.getPaymentsForInvoice(id));
        model.addAttribute("balance", invoiceService.getBalance(invoice));
        model.addAttribute("methods", PaymentMethod.values());
        return "billing/invoice-detail";
    }

    // අලුත් invoice එකක් හදන form එක
    @GetMapping("/new")
    public String newInvoiceForm() {
        return "billing/invoice-form";
    }

    // Invoice එක හදනවා
    @PostMapping("/create")
    public String createInvoice(@RequestParam Long orderId,
                                @RequestParam String customerName,
                                @RequestParam BigDecimal totalAmount) {
        Invoice invoice = invoiceService.generateInvoice(orderId, customerName, totalAmount);
        return "redirect:/invoices/" + invoice.getId();
    }

    // ගෙවීමක් record කරනවා
    @PostMapping("/{id}/pay")
    public String recordPayment(@PathVariable Long id,
                                @RequestParam BigDecimal amount,
                                @RequestParam PaymentMethod method,
                                @RequestParam(required = false) String referenceNo) {
        invoiceService.recordPayment(id, amount, method, referenceNo);
        return "redirect:/invoices/" + id;
    }

    // ගෙවීම් ඉතිහාසය
    @GetMapping("/payments")
    public String paymentHistory(Model model) {
        model.addAttribute("payments", invoiceService.getAllPayments());
        return "billing/payment-history";
    }
}