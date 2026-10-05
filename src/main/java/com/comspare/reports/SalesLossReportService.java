package com.comspare.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Sales, returns, loss and billing reports.
 * Live SQL queries on orders, order_items, parts, part_history, return_requests, invoices.
 */
@Service
public class SalesLossReportService {

    private final JdbcTemplate jdbc;

    public SalesLossReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // SALES (cancelled orders are not sales)
    public List<Map<String, Object>> getSalesReport(String fromDate, String toDate, String status) {
        ReportQuery q = new ReportQuery("""
                SELECT o.order_number AS OrderNumber, o.customer_name AS Customer, o.order_date AS OrderDate,
                       o.status AS OrderStatus, p.product_code AS ProductCode, p.name AS PartName,
                       p.category AS Category, oi.quantity AS Quantity, oi.unit_price AS UnitPrice,
                       oi.subtotal AS SalesValue
                FROM orders o
                INNER JOIN order_items oi ON o.id = oi.order_id
                INNER JOIN parts p ON oi.part_id = p.id
                WHERE o.status <> 'CANCELLED'
                """);
        q.and("o.order_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("o.order_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        q.and("o.status = ?", status);
        return q.raw("ORDER BY o.order_date DESC").run(jdbc);
    }

    // POPULAR ITEMS
    public List<Map<String, Object>> getPopularItemsReport(String fromDate, String toDate) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       SUM(oi.quantity) AS TotalQuantitySold, SUM(oi.subtotal) AS TotalSalesValue
                FROM orders o
                INNER JOIN order_items oi ON o.id = oi.order_id
                INNER JOIN parts p ON oi.part_id = p.id
                WHERE o.status <> 'CANCELLED'
                """);
        q.and("o.order_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("o.order_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        return q.raw("GROUP BY p.product_code, p.name, p.category ORDER BY SUM(oi.quantity) DESC").run(jdbc);
    }

    // DAMAGED ITEMS
    public List<Map<String, Object>> getDamagedReport(String fromDate, String toDate, String category) {
        ReportQuery q = new ReportQuery("""
                SELECT ph.id AS RecordID, p.product_code AS ProductCode, p.name AS PartName,
                       p.category AS Category, ph.quantity AS DamagedQuantity, p.price AS UnitPrice,
                       (ph.quantity * p.price) AS EstimatedLoss, ph.event_date AS DamageDate, ph.notes AS Notes
                FROM part_history ph
                INNER JOIN parts p ON ph.part_id = p.id
                WHERE ph.event_type = 'DAMAGED'
                """);
        q.and("ph.event_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("ph.event_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        q.and("p.category = ?", category);
        return q.raw("ORDER BY ph.event_date DESC").run(jdbc);
    }

    // RETURNED ITEMS
    public List<Map<String, Object>> getReturnedReport(String fromDate, String toDate, String status) {
        ReportQuery q = new ReportQuery("""
                SELECT rr.id AS ReturnID, rr.customer_name AS Customer, rr.customer_contact AS Contact,
                       p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       rr.quantity AS Quantity, rr.return_reason AS ReturnReason, rr.claim_type AS ClaimType,
                       rr.status AS Status, rr.resolution AS Resolution,
                       rr.inventory_processed AS InventoryProcessed,
                       rr.created_at AS CreatedAt, rr.updated_at AS UpdatedAt
                FROM return_requests rr
                INNER JOIN parts p ON rr.part_id = p.id
                WHERE 1 = 1
                """);
        q.and("rr.created_at >= CAST(? AS DATETIME2)", fromDate);
        q.and("rr.created_at < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        q.and("rr.status = ?", status);
        return q.raw("ORDER BY rr.created_at DESC").run(jdbc);
    }

    // LOSS (recorded DAMAGED stock - there is no separate loss table)
    public List<Map<String, Object>> getLossReport(String fromDate, String toDate) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       SUM(ph.quantity) AS TotalLostQuantity, p.price AS UnitPrice,
                       (SUM(ph.quantity) * p.price) AS EstimatedLoss
                FROM part_history ph
                INNER JOIN parts p ON ph.part_id = p.id
                WHERE ph.event_type = 'DAMAGED'
                """);
        q.and("ph.event_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("ph.event_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        return q.raw("GROUP BY p.product_code, p.name, p.category, p.price ORDER BY (SUM(ph.quantity) * p.price) DESC")
                .run(jdbc);
    }

    // MONTHLY SUMMARY
    public List<Map<String, Object>> getMonthlySummaryReport(String fromDate, String toDate) {
        ReportQuery q = new ReportQuery("""
                SELECT YEAR(o.order_date) AS SaleYear, MONTH(o.order_date) AS SaleMonth,
                       DATENAME(MONTH, o.order_date) AS MonthName,
                       COUNT(DISTINCT o.id) AS TotalOrders, SUM(oi.quantity) AS TotalItemsSold,
                       SUM(oi.subtotal) AS TotalSales
                FROM orders o
                INNER JOIN order_items oi ON o.id = oi.order_id
                WHERE o.status <> 'CANCELLED'
                """);
        q.and("o.order_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("o.order_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        return q.raw("GROUP BY YEAR(o.order_date), MONTH(o.order_date), DATENAME(MONTH, o.order_date) "
                + "ORDER BY YEAR(o.order_date) DESC, MONTH(o.order_date) DESC").run(jdbc);
    }

    // INVENTORY STATUS
    public List<Map<String, Object>> getInventoryStatusReport(String category) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       p.stock_quantity AS CurrentStock, p.reorder_level AS ReorderLevel,
                       CASE WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK'
                            WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK'
                            ELSE 'IN STOCK' END AS InventoryStatus,
                       p.price AS UnitPrice, (p.stock_quantity * p.price) AS InventoryValue
                FROM parts p
                WHERE p.is_active = 1
                """);
        return q.and("p.category = ?", category).raw("""
                ORDER BY CASE WHEN p.stock_quantity = 0 THEN 1
                              WHEN p.stock_quantity <= p.reorder_level THEN 2 ELSE 3 END, p.name
                """).run(jdbc);
    }

    // BILLING: INVOICES AND PAYMENT STATUS
    public List<Map<String, Object>> getInvoiceReport(String fromDate, String toDate, String status) {
        ReportQuery q = new ReportQuery("""
                SELECT i.invoice_number AS InvoiceNumber, o.order_number AS OrderNumber,
                       i.customer_name AS Customer, i.invoice_date AS InvoiceDate,
                       i.total_amount AS TotalAmount, i.amount_paid AS AmountPaid,
                       (i.total_amount - i.amount_paid) AS BalanceDue, i.payment_status AS PaymentStatus
                FROM invoices i
                INNER JOIN orders o ON i.order_id = o.id
                WHERE 1 = 1
                """);
        q.and("i.invoice_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("i.invoice_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        q.and("i.payment_status = ?", status);
        return q.raw("ORDER BY i.invoice_date DESC").run(jdbc);
    }
}

