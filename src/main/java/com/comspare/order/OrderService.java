package com.comspare.order;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartRepository;
import com.comspare.inventory.PartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    public static final List<String> STATUSES =
            List.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED");

    private static final String CANCELLED = "CANCELLED";

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final PartService partService;

    public OrderService(OrderRepository orderRepository,
                        PartRepository partRepository,
                        PartService partService) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.partService = partService;
    }

    // ===================== CREATE + UPDATE =====================

    /**
     * Creates a new order, or edits an existing one (when form.getId() is set).
     * Stock is reduced here (not by a DB trigger). On edit the old stock is put back first,
     * then the new items are checked and deducted. Cancelled orders hold no stock.
     */
    @Transactional
    public Order saveOrder(Order form) {

        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new IllegalArgumentException("Add at least one spare part to the order.");
        }
        if (!STATUSES.contains(form.getStatus())) {
            throw new IllegalArgumentException("Invalid order status.");
        }

        String number = form.getOrderNumber().trim();
        Optional<Order> clash = orderRepository.findByOrderNumber(number);
        if (clash.isPresent() && !clash.get().getId().equals(form.getId())) {
            throw new IllegalArgumentException("Order number '" + number + "' already exists.");
        }

        Order order;
        if (form.getId() == null) {
            order = new Order();
        } else {
            order = orderRepository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found."));
            if (!CANCELLED.equals(order.getStatus())) {
                restoreStock(order, "Order " + order.getOrderNumber() + " edited");
            }
            order.getItems().clear();
            orderRepository.flush(); // remove the old items before adding the new ones
        }

        boolean holdsStock = !CANCELLED.equals(form.getStatus());
        List<OrderItem> submitted = new ArrayList<>(form.getItems());
        double total = 0.0;

        for (OrderItem in : submitted) {
            if (in.getPart() == null || in.getPart().getId() == null) {
                throw new IllegalArgumentException("Please select a part for every item.");
            }
            if (in.getQuantity() == null || in.getQuantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }
            Part part = partRepository.findById(in.getPart().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected part not found."));

            if (holdsStock && !Boolean.TRUE.equals(part.getActive())) {
                throw new IllegalArgumentException(part.getName() + " is discontinued and cannot be ordered.");
            }
            if (holdsStock && part.getStockQuantity() < in.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for " + part.getName()
                        + ". Available stock: " + part.getStockQuantity());
            }

            OrderItem item = new OrderItem();
            item.setPart(part);
            item.setQuantity(in.getQuantity());
            item.setUnitPrice(part.getPrice());   // always the current catalogue price
            item.calculateSubtotal();
            total += item.getSubtotal();
            order.addItem(item);

            if (holdsStock) {
                partService.reduceStockForSale(part.getId(), in.getQuantity(), "Order " + number);
            }
        }

        order.setOrderNumber(number);
        order.setCustomerName(form.getCustomerName().trim());
        order.setCustomerEmail(form.getCustomerEmail().trim());
        order.setOrderDate(form.getOrderDate() != null ? form.getOrderDate() : LocalDateTime.now());
        order.setStatus(form.getStatus());
        order.setTotalAmount(Math.round(total * 100.0) / 100.0);

        return orderRepository.save(order);
    }

    // ===================== READ =====================

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmailIgnoreCaseOrderByOrderDateDesc(email.trim());
    }

    public String generateOrderNumber() {
        long next = orderRepository.count() + 1;
        String number;
        do {
            number = String.format("ORD%03d", next++);
        } while (orderRepository.existsByOrderNumber(number));
        return number;
    }

    // ===================== STATUS =====================

    @Transactional
    public Order updateOrderStatus(Long id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        if (!STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid order status.");
        }
        String old = order.getStatus();
        if (old.equals(newStatus)) {
            return order;
        }

        if (CANCELLED.equals(newStatus)) {
            restoreStock(order, "Order " + order.getOrderNumber() + " cancelled");
        } else if (CANCELLED.equals(old)) {
            // re-opening a cancelled order takes the stock again
            for (OrderItem item : order.getItems()) {
                partService.reduceStockForSale(item.getPart().getId(), item.getQuantity(),
                        "Order " + order.getOrderNumber() + " re-opened");
            }
        }
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ===================== DELETE =====================

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
        if (!CANCELLED.equals(order.getStatus())) {
            restoreStock(order, "Order " + order.getOrderNumber() + " deleted");
        }
        orderRepository.delete(order);
        orderRepository.flush(); // fails here if an invoice still points at this order
    }

    // ===================== helper =====================

    private void restoreStock(Order order, String note) {
        for (OrderItem item : order.getItems()) {
            partService.restoreStockFromOrder(item.getPart().getId(), item.getQuantity(), note);
        }
    }
}