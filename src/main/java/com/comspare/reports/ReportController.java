package com.comspare.reports;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report flow:  browser -> /reports/{name} -> ReportController -> report service (SQL on ComSpareDB)
 *               -> ReportFormatter -> Thymeleaf page   (or PdfExportService for /pdf)
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    public record Card(String group, String name, String title, String description) { }

    private record Report(String title, String description, List<Map<String, Object>> rows,
                          boolean dates, boolean category, List<String> statuses) { }

    private static final List<String> NONE = List.of();
    private static final List<String> STOCK_STATUS = List.of("LOW", "OUT", "AVAILABLE");
    private static final List<String> PO_STATUS = List.of("PENDING", "APPROVED", "RECEIVED", "CANCELLED");
    private static final List<String> SALE_STATUS = List.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED");
    private static final List<String> RETURN_STATUS =
            List.of("PENDING", "APPROVED", "REJECTED", "PROCESSING", "COMPLETED", "CANCELLED");
    private static final List<String> INVOICE_STATUS = List.of("PENDING", "PARTIAL", "PAID");

    private static final String G1 = "Inventory & Purchasing";
    private static final String G2 = "Stock Prediction & Analytics";
    private static final String G3 = "Sales, Returns, Billing & Loss";

    private static final List<Card> CARDS = List.of(
            new Card(G1, "stock", "Stock Report", "Current stock quantities, reorder levels, prices and stock values."),
            new Card(G1, "low-stock", "Low Stock Report", "Parts at or below their reorder level."),
            new Card(G1, "out-of-stock", "Out of Stock Report", "Parts that currently have zero stock."),
            new Card(G1, "movement", "Stock Movement", "Received, sold, returned, adjusted and damaged movements."),
            new Card(G1, "valuation", "Inventory Valuation", "Inventory value from stock quantity and price."),
            new Card(G1, "purchases", "Purchase Report", "Purchase orders, suppliers, quantities and costs."),
            new Card(G1, "purchase-expense", "Purchase Expense", "Spending on purchase orders and received stock."),
            new Card(G1, "supplier-performance", "Supplier Performance", "Supplier orders, deliveries and fulfilment rates."),
            new Card(G2, "reorder-prediction", "Reorder Prediction", "Suggested reorder quantities, with reorder-level update."),
            new Card(G3, "sales", "Sales Report", "Sales orders, customers, parts and sales values."),
            new Card(G3, "popular-items", "Popular Items", "Parts ranked by quantity sold."),
            new Card(G3, "monthly-summary", "Monthly Summary", "Monthly order counts, items sold and sales totals."),
            new Card(G3, "inventory-status", "Inventory Status", "In stock, low stock or out of stock for every part."),
            new Card(G3, "returned", "Returned Items", "Customer returns, reasons, claims and statuses."),
            new Card(G3, "damaged", "Damaged Items", "Stock recorded as damaged and its estimated value."),
            new Card(G3, "loss", "Loss Report", "Estimated stock loss from damaged quantities."),
            new Card(G3, "invoices", "Invoices & Payments", "Invoices, amounts paid and balances due.")
    );

    private final InventoryReportService inventory;
    private final SalesLossReportService sales;
    private final PdfExportService pdf;

    public ReportController(InventoryReportService inventory, SalesLossReportService sales, PdfExportService pdf) {
        this.inventory = inventory;
        this.sales = sales;
        this.pdf = pdf;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("groups", CARDS.stream()
                .collect(Collectors.groupingBy(Card::group, LinkedHashMap::new, Collectors.toList())));
        return "reports/report-dashboard";
    }

    @GetMapping("/{name}")
    public String show(@PathVariable String name,
                       @RequestParam(required = false) String fromDate,
                       @RequestParam(required = false) String toDate,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String status,
                       Model model) {

        Report r = build(name, fromDate, toDate, category, status);
        boolean reorder = "reorder-prediction".equals(name);

        model.addAttribute("reportName", name);
        model.addAttribute("reportTitle", r.title());
        model.addAttribute("reportDescription", r.description());
        model.addAttribute("rows", ReportFormatter.format(r.rows(), !reorder));
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        model.addAttribute("showDates", r.dates());
        model.addAttribute("showCategory", r.category());
        model.addAttribute("statuses", r.statuses());

        return reorder ? "reports/reorder-prediction" : "reports/report-view";
    }

    @GetMapping("/{name}/pdf")
    public void exportPdf(@PathVariable String name,
                          @RequestParam(required = false) String fromDate,
                          @RequestParam(required = false) String toDate,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String status,
                          HttpServletResponse response) throws Exception {

        Report r = build(name, fromDate, toDate, category, status);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "-report.pdf\"");
        pdf.export(r.title(), ReportFormatter.format(r.rows(), true), response.getOutputStream());
    }

    private Report build(String name, String from, String to, String cat, String status) {
        return switch (name) {
            case "stock" -> new Report("Stock Report", desc(name), inventory.getStockReport(cat, status), false, true, STOCK_STATUS);
            case "low-stock" -> new Report("Low Stock Report", desc(name), inventory.getLowStockReport(cat), false, true, NONE);
            case "out-of-stock" -> new Report("Out of Stock Report", desc(name), inventory.getOutOfStockReport(cat), false, true, NONE);
            case "movement" -> new Report("Stock Movement Report", desc(name), inventory.getMovementReport(from, to, cat), true, true, NONE);
            case "valuation" -> new Report("Inventory Valuation Report", desc(name), inventory.getValuationReport(cat), false, true, NONE);
            case "purchases" -> new Report("Purchase Report", desc(name), inventory.getPurchaseReport(from, to, status), true, false, PO_STATUS);
            case "purchase-expense" -> new Report("Purchase Expense Report", desc(name), inventory.getPurchaseExpenseReport(from, to), true, false, NONE);
            case "supplier-performance" -> new Report("Supplier Performance Report", desc(name), inventory.getSupplierPerformanceReport(from, to), true, false, NONE);
            case "reorder-prediction" -> new Report("Reorder Prediction Report", desc(name), inventory.getReorderPredictions(cat), false, true, NONE);
            case "sales" -> new Report("Sales Report", desc(name), sales.getSalesReport(from, to, status), true, false, SALE_STATUS);
            case "popular-items" -> new Report("Popular Items Report", desc(name), sales.getPopularItemsReport(from, to), true, false, NONE);
            case "monthly-summary" -> new Report("Monthly Sales Summary", desc(name), sales.getMonthlySummaryReport(from, to), true, false, NONE);
            case "inventory-status" -> new Report("Inventory Status Report", desc(name), sales.getInventoryStatusReport(cat), false, true, NONE);
            case "returned" -> new Report("Returned Items Report", desc(name), sales.getReturnedReport(from, to, status), true, false, RETURN_STATUS);
            case "damaged" -> new Report("Damaged Items Report", desc(name), sales.getDamagedReport(from, to, cat), true, true, NONE);
            case "loss" -> new Report("Loss Report", desc(name), sales.getLossReport(from, to), true, false, NONE);
            case "invoices" -> new Report("Invoices & Payments Report", desc(name), sales.getInvoiceReport(from, to, status), true, false, INVOICE_STATUS);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + name);
        };
    }

    private String desc(String name) {
        return CARDS.stream().filter(c -> c.name().equals(name)).map(Card::description).findFirst().orElse("");
    }
}