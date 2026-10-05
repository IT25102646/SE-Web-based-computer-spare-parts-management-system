package com.comspare.order;

import com.comspare.inventory.PartRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PartRepository partRepository;

    public OrderController(OrderService orderService, PartRepository partRepository) {
        this.orderService = orderService;
        this.partRepository = partRepository;
    }

    @GetMapping
    public String getAllOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("statuses", OrderService.STATUSES);
        return "order/orders";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Order order = new Order();
        order.setOrderNumber(orderService.generateOrderNumber());
        order.setOrderDate(LocalDateTime.now().withSecond(0).withNano(0));
        order.setStatus("PENDING");
        return form(order, model);
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid order ID: " + id));
        return form(order, model);
    }

    @PostMapping("/save")
    public String saveOrder(@Valid @ModelAttribute("order") Order order,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            hydrate(order);
            return form(order, model);
        }
        try {
            Order saved = orderService.saveOrder(order);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order " + saved.getOrderNumber() + " saved.");
            return "redirect:/orders";
        } catch (IllegalArgumentException | IllegalStateException e) {
            hydrate(order);
            model.addAttribute("errorMessage", e.getMessage());
            return form(order, model);
        }
    }

    @PostMapping("/status/{id}")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders";
    }

    @PostMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Order deleted.");
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete: an invoice exists for this order. Delete the invoice first, or cancel the order instead.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders";
    }

    // Customer order history (search by email)
    @GetMapping("/history")
    public String history(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("customerEmail", email);
        model.addAttribute("orders",
                (email == null || email.isBlank()) ? java.util.List.of() : orderService.getOrdersByEmail(email));
        return "order/order-history";
    }

    // ---------- helpers ----------

    private String form(Order order, Model model) {
        model.addAttribute("order", order);
        model.addAttribute("parts", partRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("statuses", OrderService.STATUSES);
        return "order/order-form";
    }

    /** After a failed save, re-load the part details so the item rows can be shown again. */
    private void hydrate(Order order) {
        if (order.getItems() == null) return;
        order.getItems().removeIf(i -> i.getPart() == null || i.getPart().getId() == null);
        for (OrderItem i : order.getItems()) {
            partRepository.findById(i.getPart().getId()).ifPresent(p -> {
                i.setPart(p);
                i.setUnitPrice(p.getPrice());
                i.calculateSubtotal();
            });
        }
    }
}
