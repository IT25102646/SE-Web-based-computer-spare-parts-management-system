package com.comspare.reports;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    private final InventoryReportService inventoryReportService;
    private final SalesLossReportService salesLossReportService;
    private final PdfExportService pdfExportService;

    public ReportController(
            InventoryReportService inventoryReportService,
            SalesLossReportService salesLossReportService,
            PdfExportService pdfExportService) {

        this.inventoryReportService = inventoryReportService;
        this.salesLossReportService = salesLossReportService;
        this.pdfExportService = pdfExportService;
    }

    // =========================================================
    // REPORT DASHBOARD
    // =========================================================

    @GetMapping("/reports")
    public String dashboard() {
        return "reports/report-dashboard";
    }

    // =========================================================
    // 1. STOCK
    // =========================================================

    @GetMapping("/reports/stock")
    public String stock(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getStockReport(category, status);

        return view(
                model,
                "Stock Report",
                "Current stock levels for all spare parts.",
                rows,
                category,
                null,
                status,
                "stock"
        );
    }

    // =========================================================
    // 2. LOW STOCK
    // =========================================================

    @GetMapping("/reports/low-stock")
    public String lowStock(
            @RequestParam(required = false) String category,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getLowStockReport(category);

        return view(
                model,
                "Low Stock Report",
                "Parts whose current stock is at or below the reorder level.",
                rows,
                category,
                null,
                null,
                "low-stock"
        );
    }

    // =========================================================
    // 3. OUT OF STOCK
    // =========================================================

    @GetMapping("/reports/out-of-stock")
    public String outOfStock(
            @RequestParam(required = false) String category,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getOutOfStockReport(category);

        return view(
                model,
                "Out of Stock Report",
                "Parts that currently have zero stock.",
                rows,
                category,
                null,
                null,
                "out-of-stock"
        );
    }

    // =========================================================
    // 4. STOCK MOVEMENT
    // =========================================================

    @GetMapping("/reports/movement")
    public String movement(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String category,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getMovementReport(
                        fromDate,
                        toDate,
                        category
                );

        return view(
                model,
                "Stock Movement Report",
                "Recorded stock movements from part history.",
                rows,
                category,
                fromDate,
                toDate,
                "movement"
        );
    }

    // =========================================================
    // 5. INVENTORY VALUATION
    // =========================================================

    @GetMapping("/reports/valuation")
    public String valuation(
            @RequestParam(required = false) String category,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getValuationReport(category);

        return view(
                model,
                "Inventory Valuation Report",
                "Current inventory value calculated from stock quantity and part price.",
                rows,
                category,
                null,
                null,
                "valuation"
        );
    }

    // =========================================================
    // 6. PURCHASES
    // =========================================================

    @GetMapping("/reports/purchases")
    public String purchases(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String status,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getPurchaseReport(
                        fromDate,
                        toDate,
                        status
                );

        return view(
                model,
                "Purchase Report",
                "Purchase orders, suppliers, quantities and purchase values.",
                rows,
                null,
                fromDate,
                toDate,
                "purchases"
        );
    }

    // =========================================================
    // 7. PURCHASE EXPENSE
    // =========================================================

    @GetMapping("/reports/purchase-expense")
    public String purchaseExpense(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getPurchaseExpenseReport(
                        fromDate,
                        toDate
                );

        return view(
                model,
                "Purchase Expense Report",
                "Purchase expenditure calculated from ordered quantities and unit costs.",
                rows,
                null,
                fromDate,
                toDate,
                "purchase-expense"
        );
    }

    // =========================================================
    // 8. SUPPLIER PERFORMANCE
    // =========================================================

    @GetMapping("/reports/supplier-performance")
    public String supplierPerformance(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {

        List<Map<String, Object>> rows =
                inventoryReportService.getSupplierPerformanceReport(
                        fromDate,
                        toDate
                );

        return view(
                model,
                "Supplier Performance Report",
                "Supplier order, delivery and fulfillment information.",
                rows,
                null,
                fromDate,
                toDate,
                "supplier-performance"
        );
    }

    // =========================================================
    // 9. SALES
    // =========================================================

    @GetMapping("/reports/sales")
    public String sales(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String status,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getSalesReport(
                        fromDate,
                        toDate,
                        status
                );

        return view(
                model,
                "Sales Report",
                "Sales recorded from non-cancelled customer orders.",
                rows,
                null,
                fromDate,
                toDate,
                "sales"
        );
    }

    // =========================================================
    // 10. POPULAR ITEMS
    // =========================================================

    @GetMapping("/reports/popular-items")
    public String popularItems(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getPopularItemsReport(
                        fromDate,
                        toDate
                );

        return view(
                model,
                "Popular Items Report",
                "Parts ranked by total quantity sold.",
                rows,
                null,
                fromDate,
                toDate,
                "popular-items"
        );
    }

    // =========================================================
    // 11. DAMAGED
    // =========================================================

    @GetMapping("/reports/damaged")
    public String damaged(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String category,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getDamagedReport(
                        fromDate,
                        toDate,
                        category
                );

        return view(
                model,
                "Damaged Items Report",
                "Damaged stock recorded in part history.",
                rows,
                category,
                fromDate,
                toDate,
                "damaged"
        );
    }

    // =========================================================
    // 12. RETURNED
    // =========================================================

    @GetMapping("/reports/returned")
    public String returned(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String status,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getReturnedReport(
                        fromDate,
                        toDate,
                        status
                );

        return view(
                model,
                "Returned Items Report",
                "Customer return requests and their current status.",
                rows,
                null,
                fromDate,
                toDate,
                "returned"
        );
    }

    // =========================================================
    // 13. LOSS
    // =========================================================

    @GetMapping("/reports/loss")
    public String loss(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getLossReport(
                        fromDate,
                        toDate
                );

        return view(
                model,
                "Loss Report",
                "Estimated stock loss based on recorded damaged items.",
                rows,
                null,
                fromDate,
                toDate,
                "loss"
        );
    }

    // =========================================================
    // 14. MONTHLY SUMMARY
    // =========================================================

    @GetMapping("/reports/monthly-summary")
    public String monthlySummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getMonthlySummaryReport(
                        fromDate,
                        toDate
                );

        return view(
                model,
                "Monthly Sales Summary",
                "Monthly order, item and sales totals.",
                rows,
                null,
                fromDate,
                toDate,
                "monthly-summary"
        );
    }

    // =========================================================
    // 15. INVENTORY STATUS
    // =========================================================

    @GetMapping("/reports/inventory-status")
    public String inventoryStatus(
            @RequestParam(required = false) String category,
            Model model) {

        List<Map<String, Object>> rows =
                salesLossReportService.getInventoryStatusReport(category);

        return view(
                model,
                "Inventory Status Report",
                "Current inventory classification by stock availability.",
                rows,
                category,
                null,
                null,
                "inventory-status"
        );
    }

    // =========================================================
    // PDF EXPORT
    // =========================================================

    @GetMapping("/reports/{reportName}/pdf")
    public void exportPdf(
            @PathVariable String reportName,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            jakarta.servlet.http.HttpServletResponse response)
            throws Exception {

        List<Map<String, Object>> rows;
        String title;

        switch (reportName) {

            case "stock" -> {
                rows = inventoryReportService.getStockReport(
                        category, status);
                title = "Stock Report";
            }

            case "low-stock" -> {
                rows = inventoryReportService.getLowStockReport(
                        category);
                title = "Low Stock Report";
            }

            case "out-of-stock" -> {
                rows = inventoryReportService.getOutOfStockReport(
                        category);
                title = "Out of Stock Report";
            }

            case "movement" -> {
                rows = inventoryReportService.getMovementReport(
                        fromDate, toDate, category);
                title = "Stock Movement Report";
            }

            case "valuation" -> {
                rows = inventoryReportService.getValuationReport(
                        category);
                title = "Inventory Valuation Report";
            }

            case "purchases" -> {
                rows = inventoryReportService.getPurchaseReport(
                        fromDate, toDate, status);
                title = "Purchase Report";
            }

            case "purchase-expense" -> {
                rows = inventoryReportService.getPurchaseExpenseReport(
                        fromDate, toDate);
                title = "Purchase Expense Report";
            }

            case "supplier-performance" -> {
                rows = inventoryReportService.getSupplierPerformanceReport(
                        fromDate, toDate);
                title = "Supplier Performance Report";
            }

            case "sales" -> {
                rows = salesLossReportService.getSalesReport(
                        fromDate, toDate, status);
                title = "Sales Report";
            }

            case "popular-items" -> {
                rows = salesLossReportService.getPopularItemsReport(
                        fromDate, toDate);
                title = "Popular Items Report";
            }

            case "damaged" -> {
                rows = salesLossReportService.getDamagedReport(
                        fromDate, toDate, category);
                title = "Damaged Items Report";
            }

            case "returned" -> {
                rows = salesLossReportService.getReturnedReport(
                        fromDate, toDate, status);
                title = "Returned Items Report";
            }

            case "loss" -> {
                rows = salesLossReportService.getLossReport(
                        fromDate, toDate);
                title = "Loss Report";
            }

            case "monthly-summary" -> {
                rows = salesLossReportService.getMonthlySummaryReport(
                        fromDate, toDate);
                title = "Monthly Sales Summary";
            }

            case "inventory-status" -> {
                rows = salesLossReportService.getInventoryStatusReport(
                        category);
                title = "Inventory Status Report";
            }

            default -> {
                response.sendError(
                        jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND,
                        "Unknown report: " + reportName
                );
                return;
            }
        }

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + reportName + "-report.pdf\""
        );

        pdfExportService.export(
                title,
                rows,
                response.getOutputStream()
        );
    }

    // =========================================================
    // COMMON VIEW METHOD
    // =========================================================

    private String view(
            Model model,
            String title,
            String description,
            List<Map<String, Object>> rows,
            String category,
            String fromDate,
            String toDate,
            String reportName) {

        model.addAttribute("reportTitle", title);
        model.addAttribute("reportDescription", description);
        model.addAttribute("rows", rows);
        model.addAttribute("category", category);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("reportName", reportName);

        return "reports/report-view";
    }
}