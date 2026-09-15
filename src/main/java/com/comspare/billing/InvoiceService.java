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

    // ---------- 1. Order එකකින් invoice එකක් හදනවා ----------
    @Transactional
    public Invoice generateInvoice(Long orderId, String customerName, BigDecimal totalAmount) {

        // එකම order එකට දැනටමත් invoice එකක් තියෙනවා නම්, ඒකම දෙනවා
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

    // ---------- 2. ගෙවීමක් record කරනවා ----------
    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount,
                                 PaymentMethod method, String referenceNo) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        // ගෙවීම save කරනවා
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setReferenceNo(referenceNo);
        paymentRepository.save(payment);

        // Invoice එකේ ගෙවපු මුදල update කරනවා
        BigDecimal newPaid = invoice.getAmountPaid().add(amount);
        invoice.setAmountPaid(newPaid);
        invoice.setPaymentStatus(decideStatus(newPaid, invoice.getTotalAmount()));
        invoiceRepository.save(invoice);

        return payment;
    }

    // ---------- 3. තත්ත්වය තීරණය කරනවා ----------
    private PaymentStatus decideStatus(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PARTIAL;
        } else {
            return PaymentStatus.PENDING;
        }
    }

    // ---------- 4. Invoice අංකය හදනවා ----------
    private String generateInvoiceNumber() {
        long count = invoiceRepository.count() + 1;
        return String.format("INV-%05d", count);
    }

    // ---------- 5. බලන්න ඕන දේවල් ----------
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
}