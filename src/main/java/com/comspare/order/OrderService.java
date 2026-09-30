package com.comspare.order;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;

    public OrderService(OrderRepository orderRepository,
                        PartRepository partRepository) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
    }

    // Create / Update Order
    @Transactional
    public Order saveOrder(Order order) {

        double total = 0.0;

        // Process each order item
        for (OrderItem item : order.getItems()) {

            if (item.getPart() == null || item.getPart().getId() == null) {
                throw new IllegalArgumentException("Please select a part.");
            }

            Part part = partRepository.findById(item.getPart().getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected part not found."));

            int quantity = item.getQuantity();

            // Check quantity
            if (quantity <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be at least 1.");
            }

            // Check stock availability
            if (part.getStockQuantity() < quantity) {
                throw new IllegalArgumentException(
                        "Not enough stock for " + part.getName()
                                + ". Available stock: "
                                + part.getStockQuantity());
            }

            // Use current part price
            item.setPart(part);
            item.setUnitPrice(part.getPrice());

            // Calculate item subtotal
            item.calculateSubtotal();

            // Add item subtotal to order total
            total += item.getSubtotal();

            // Reduce stock
            part.setStockQuantity(
                    part.getStockQuantity() - quantity
            );

            // Connect item to order
            item.setOrder(order);

            // Save updated stock
            partRepository.save(part);
        }

        // Set total order amount
        order.setTotalAmount(total);

        // Save order
        return orderRepository.save(order);
    }

    // Get all Orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Get Order by ID
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    // Get Order by Order Number
    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    // Update Order Status
    @Transactional
    public Order updateOrderStatus(Long id, String status) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found with ID: " + id));

        order.setStatus(status);

        return orderRepository.save(order);
    }

    // Delete Order
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}
