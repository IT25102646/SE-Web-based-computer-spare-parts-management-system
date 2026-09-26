package com.comspare.order;

import com.comspare.inventory.Part;
import com.comspare.inventory.PartRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PartRepository partRepository;

    public OrderController(OrderService orderService,
                           PartRepository partRepository) {
        this.orderService = orderService;
        this.partRepository = partRepository;
    }

    // View all orders
    @GetMapping
    public String getAllOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "order/orders";
    }

    // Show create order form
    @GetMapping("/new")
    public String showCreateForm(Model model) {

        Order order = new Order();

        order.setOrderDate(java.time.LocalDateTime.now());
        order.setStatus("PENDING");

        model.addAttribute("order", order);

        // Get available parts from inventory
        List<Part> parts = partRepository.findAll();
        model.addAttribute("parts", parts);

        return "order/order-form";
    }

    // Save order
    @PostMapping("/save")
    public String saveOrder(
            @Valid @ModelAttribute("order") Order order,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {

            // Reload parts if validation fails
            List<Part> parts = partRepository.findAll();
            model.addAttribute("parts", parts);

            return "order/order-form";
        }

        try {

            orderService.saveOrder(order);

        } catch (IllegalArgumentException e) {

            model.addAttribute("errorMessage", e.getMessage());

            List<Part> parts = partRepository.findAll();
            model.addAttribute("parts", parts);

            return "order/order-form";
        }

        return "redirect:/orders";
    }

    // Edit order
    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid order ID: " + id));

        model.addAttribute("order", order);

        // Load parts for the form
        List<Part> parts = partRepository.findAll();
        model.addAttribute("parts", parts);

        return "order/order-form";
    }

    // Update order status
    @PostMapping("/status/{id}")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam("status") String status) {

        orderService.updateOrderStatus(id, status);

        return "redirect:/orders";
    }

    // Delete order
    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id) {

        orderService.deleteOrder(id);

        return "redirect:/orders";
    }
}
