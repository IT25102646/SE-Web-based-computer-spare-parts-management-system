package com.comspare.billing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    // ---------- CREATE ----------
    @Transactional
    public Invoice generateInvoice(Long orderId, String customerName, BigDecimal totalAmount) {
        // one invoice per order: return the existing one if there is already one
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

    // ---------- UPDATE invoice (customer name / total) ----------
    @Transactional
    public Invoice updateInvoice(Long id, String customerName, BigDecimal totalAmount) {
        Invoice invoice = getInvoiceById(id);
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative.");
        }
        if (totalAmount.compareTo(invoice.getAmountPaid()) < 0) {
            throw new IllegalArgumentException(
                    "Total cannot be lower than the amount already paid (Rs. " + invoice.getAmountPaid() + ").");
        }
        invoice.setCustomerName(customerName);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaymentStatus(decideStatus(invoice.getAmountPaid(), totalAmount));
        return invoiceRepository.save(invoice);
    }

    // ---------- DELETE invoice (and its payments) ----------
    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        paymentRepository.deleteAll(paymentRepository.findByInvoiceId(id));
        invoiceRepository.delete(invoice);
    }

    // ---------- Payments ----------
    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount,
                                 PaymentMethod method, String referenceNo) {

        Invoice invoice = getInvoiceById(invoiceId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        BigDecimal balance = getBalance(invoice);
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Payment is more than the balance due (Rs. " + balance + ").");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setReferenceNo(referenceNo == null || referenceNo.isBlank() ? null : referenceNo.trim());
        paymentRepository.save(payment);

        BigDecimal newPaid = invoice.getAmountPaid().add(amount);
        invoice.setAmountPaid(newPaid);
        invoice.setPaymentStatus(decideStatus(newPaid, invoice.getTotalAmount()));
        invoiceRepository.save(invoice);
        return payment;
    }

    @Transactional
    public Long deletePayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        Invoice invoice = payment.getInvoice();
        paymentRepository.delete(payment);
        paymentRepository.flush();

        BigDecimal newPaid = paymentRepository.findByInvoiceId(invoice.getId())
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setAmountPaid(newPaid);
        invoice.setPaymentStatus(decideStatus(newPaid, invoice.getTotalAmount()));
        invoiceRepository.save(invoice);
        return invoice.getId();
    }

    private PaymentStatus decideStatus(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PARTIAL;
        }
        return PaymentStatus.PENDING;
    }

    /** Never reuses a number, even after invoices were deleted or seeded as INV001. */
    private String generateInvoiceNumber() {
        long next = invoiceRepository.count() + 1;
        String number;
        do {
            number = String.format("INV-%05d", next++);
        } while (invoiceRepository.existsByInvoiceNumber(number));
        return number;
    }

    // ---------- READ ----------
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    public Optional<Invoice> findByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsForInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    public BigDecimal getBalance(Invoice invoice) {
        return invoice.getTotalAmount().subtract(invoice.getAmountPaid());
    }
}
