package com.comspare.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReorderUpdateController {

    private final JdbcTemplate jdbcTemplate;

    public ReorderUpdateController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/reports/reorder-prediction/update")
    public String updateReorderLevel(@RequestParam String productCode,
                                     @RequestParam int newReorderLevel,
                                     RedirectAttributes redirectAttributes) {
        if (newReorderLevel < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reorder level cannot be negative.");
            return "redirect:/reports/reorder-prediction";
        }
        int rows = jdbcTemplate.update(
                "UPDATE parts SET reorder_level = ? WHERE product_code = ?", newReorderLevel, productCode);

        if (rows > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reorder level for " + productCode + " updated to " + newReorderLevel + ".");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not find part " + productCode + " to update.");
        }
        return "redirect:/reports/reorder-prediction";
    }
}