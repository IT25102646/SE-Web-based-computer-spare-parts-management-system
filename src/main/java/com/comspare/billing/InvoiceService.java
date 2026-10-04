package com.comspare.billing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    // ---------- 1. Creating an invoice from an order ----------
    @Transactional
    public Invoice generateInvoice(Long orderId, String customerName, BigDecimal totalAmount) {

        // If an invoice already exists for the same order, return that invoice.
        return invoiceRepository.findByOrderId(orderId).orElseGet(() -> {

            Invoice invoice = new Invoice();
            invoice.setOrderId(orderId);
            invoice.setCustomerName(customerName);
            invoice.setTotalAmount(totalAmount);
            invoice.setAmountPaid(BigDecimal.ZERO);
            invoice.setPaymentStatus(PaymentStatus.PENDING);
            invoice.setInvoiceNumber(generateInvoiceNumber());

            return invoiceRepository.save(invoice);
        });
    }

    // ---------- 2. Recording a payment ----------
    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount,
                                 PaymentMethod method, String referenceNo) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        // Saving the payment
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setReferenceNo(referenceNo);
        paymentRepository.save(payment);

        // Updating the paid amount on the invoice
        BigDecimal newPaid = invoice.getAmountPaid().add(amount);
        invoice.setAmountPaid(newPaid);
        invoice.setPaymentStatus(decideStatus(newPaid, invoice.getTotalAmount()));
        invoiceRepository.save(invoice);

        return payment;
    }

    // ---------- 3. Determining the status ----------
    private PaymentStatus decideStatus(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PARTIAL;
        } else {
            return PaymentStatus.PENDING;
        }
    }

    // ---------- 4. Generating the invoice number ----------
    private String generateInvoiceNumber() {
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%05d", count);
    }

    // ---------- 5. Things to check ----------
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    public List<Payment> getPaymentsForInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    public BigDecimal getBalance(Invoice invoice) {
        return invoice.getTotalAmount().subtract(invoice.getAmountPaid());
    }
    // ---------- Remove a payment that was recorded by mistake ----------
    @Transactional
    public Long deletePayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        Invoice invoice = payment.getInvoice();

        // Delete the payment record
        paymentRepository.delete(payment);

        // Recalculate the total from the remaining payments
        BigDecimal newPaid = paymentRepository.findByInvoiceId(invoice.getId())
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update the invoice with the new paid amount and status
        invoice.setAmountPaid(newPaid);
        invoice.setPaymentStatus(decideStatus(newPaid, invoice.getTotalAmount()));
        invoiceRepository.save(invoice);

        return invoice.getId();
    }

    // ---------- Get a single payment (used for the receipt) ----------
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }
}