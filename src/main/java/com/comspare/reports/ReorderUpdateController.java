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
    public String updateReorderLevel(
            @RequestParam String productCode,
            @RequestParam int newReorderLevel,
            RedirectAttributes redirectAttributes) {

        String sql =
                "UPDATE parts SET reorder_level = ? " +
                        "WHERE product_code = ?";

        int rowsAffected = jdbcTemplate.update(
                sql,
                newReorderLevel,
                productCode
        );

        if (rowsAffected > 0) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Reorder level for " + productCode
                            + " updated to " + newReorderLevel + "."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not find part " + productCode + " to update."
            );
        }

        return "redirect:/reports/reorder-prediction";
    }
}