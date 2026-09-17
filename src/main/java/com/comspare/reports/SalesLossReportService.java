package com.comspare.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SalesLossReportService {

    private final JdbcTemplate jdbcTemplate;

    public SalesLossReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================
    // 9. SALES REPORT
    // Cancelled orders are excluded from sales.
    // =========================================================

    public List<Map<String, Object>> getSalesReport(
            String fromDate,
            String toDate,
            String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    o.order_number AS OrderNumber,
                    o.customer_name AS Customer,
                    o.order_date AS OrderDate,
                    o.status AS OrderStatus,
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    oi.quantity AS Quantity,
                    oi.unit_price AS UnitPrice,
                    oi.subtotal AS SalesValue
                FROM orders o
                INNER JOIN order_items oi
                    ON o.id = oi.order_id
                INNER JOIN parts p
                    ON oi.part_id = p.id
                WHERE o.status <> 'CANCELLED'
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND o.order_date >= CAST(? AS DATETIME2) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND o.order_date < DATEADD(DAY, 1, CAST(? AS DATE)) ");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND o.status = ? ");
        }

        sql.append(" ORDER BY o.order_date DESC ");

        return querySales(sql.toString(), fromDate, toDate, status);
    }

    // =========================================================
    // 10. POPULAR ITEMS
    // =========================================================

    public List<Map<String, Object>> getPopularItemsReport(
            String fromDate,
            String toDate) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    SUM(oi.quantity) AS TotalQuantitySold,
                    SUM(oi.subtotal) AS TotalSalesValue
                FROM orders o
                INNER JOIN order_items oi
                    ON o.id = oi.order_id
                INNER JOIN parts p
                    ON oi.part_id = p.id
                WHERE o.status <> 'CANCELLED'
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND o.order_date >= CAST(? AS DATETIME2) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND o.order_date < DATEADD(DAY, 1, CAST(? AS DATE)) ");
        }

        sql.append("""
                GROUP BY
                    p.product_code,
                    p.name,
                    p.category
                ORDER BY TotalQuantitySold DESC
                """);

        return queryDateOnly(sql.toString(), fromDate, toDate);
    }

    // =========================================================
    // 11. DAMAGED ITEMS REPORT
    // =========================================================

    public List<Map<String, Object>> getDamagedReport(
            String fromDate,
            String toDate,
            String category) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    ph.id AS RecordID,
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    ph.quantity AS DamagedQuantity,
                    p.price AS UnitPrice,
                    (ph.quantity * p.price) AS EstimatedLoss,
                    ph.event_date AS DamageDate,
                    ph.notes AS Notes
                FROM part_history ph
                INNER JOIN parts p
                    ON ph.part_id = p.id
                WHERE ph.event_type = 'DAMAGED'
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND ph.event_date >= CAST(? AS DATETIME2) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND ph.event_date < DATEADD(DAY, 1, CAST(? AS DATE)) ");
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ? ");
        }

        sql.append(" ORDER BY ph.event_date DESC ");

        return queryDamaged(sql.toString(), fromDate, toDate, category);
    }

    // =========================================================
    // 12. RETURNED ITEMS REPORT
    // =========================================================

    public List<Map<String, Object>> getReturnedReport(
            String fromDate,
            String toDate,
            String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    rr.id AS ReturnID,
                    rr.customer_name AS Customer,
                    rr.customer_contact AS Contact,
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    rr.quantity AS Quantity,
                    rr.return_reason AS ReturnReason,
                    rr.claim_type AS ClaimType,
                    rr.status AS Status,
                    rr.resolution AS Resolution,
                    rr.inventory_processed AS InventoryProcessed,
                    rr.created_at AS CreatedAt,
                    rr.updated_at AS UpdatedAt
                FROM return_requests rr
                INNER JOIN parts p
                    ON rr.part_id = p.id
                WHERE 1 = 1
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND rr.created_at >= CAST(? AS DATETIME2) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND rr.created_at < DATEADD(DAY, 1, CAST(? AS DATE)) ");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND rr.status = ? ");
        }

        sql.append(" ORDER BY rr.created_at DESC ");

        return queryReturns(sql.toString(), fromDate, toDate, status);
    }

    // =========================================================
    // 13. LOSS REPORT
    //
    // There is no separate "loss" column/table in the database.
    // Therefore this report uses DAMAGED records from part_history
    // as the recorded stock loss.
    // =========================================================

    public List<Map<String, Object>> getLossReport(
            String fromDate,
            String toDate) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    SUM(ph.quantity) AS TotalLostQuantity,
                    p.price AS UnitPrice,
                    (SUM(ph.quantity) * p.price) AS EstimatedLoss
                FROM part_history ph
                INNER JOIN parts p
                    ON ph.part_id = p.id
                WHERE ph.event_type = 'DAMAGED'
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND ph.event_date >= CAST(? AS DATETIME2) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND ph.event_date < DATEADD(DAY, 1, CAST(? AS DATE)) ");
        }

        sql.append("""
                GROUP BY
                    p.product_code,
                    p.name,
                    p.category,
                    p.price
                ORDER BY EstimatedLoss DESC
                """);

        return queryDateOnly(sql.toString(), fromDate, toDate);
    }

    // =========================================================
    // 14. MONTHLY SUMMARY
    // =========================================================

    public List<Map<String, Object>> getMonthlySummaryReport(
            String fromDate,
            String toDate) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    YEAR(o.order_date) AS SaleYear,
                    MONTH(o.order_date) AS SaleMonth,
                    DATENAME(MONTH, o.order_date) AS MonthName,
                    COUNT(DISTINCT o.id) AS TotalOrders,
                    SUM(oi.quantity) AS TotalItemsSold,
                    SUM(oi.subtotal) AS TotalSales
                FROM orders o
                INNER JOIN order_items oi
                    ON o.id = oi.order_id
                WHERE o.status <> 'CANCELLED'
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND o.order_date >= CAST(? AS DATETIME2) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND o.order_date < DATEADD(DAY, 1, CAST(? AS DATE)) ");
        }

        sql.append("""
                GROUP BY
                    YEAR(o.order_date),
                    MONTH(o.order_date),
                    DATENAME(MONTH, o.order_date)
                ORDER BY
                    SaleYear DESC,
                    SaleMonth DESC
                """);

        return queryDateOnly(sql.toString(), fromDate, toDate);
    }

    // =========================================================
    // 15. INVENTORY STATUS REPORT
    // =========================================================

    public List<Map<String, Object>> getInventoryStatusReport(
            String category) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    p.stock_quantity AS CurrentStock,
                    p.reorder_level AS ReorderLevel,
                    CASE
                        WHEN p.stock_quantity = 0 THEN 'OUT OF STOCK'
                        WHEN p.stock_quantity <= p.reorder_level THEN 'LOW STOCK'
                        ELSE 'IN STOCK'
                    END AS InventoryStatus,
                    p.price AS UnitPrice,
                    (p.stock_quantity * p.price) AS InventoryValue
                FROM parts p
                WHERE 1 = 1
                """);

        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ? ");
        }

        sql.append("""
                ORDER BY
                    CASE
                        WHEN p.stock_quantity = 0 THEN 1
                        WHEN p.stock_quantity <= p.reorder_level THEN 2
                        ELSE 3
                    END,
                    p.name
                """);

        if (category != null && !category.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), category);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private List<Map<String, Object>> querySales(
            String sql,
            String fromDate,
            String toDate,
            String status) {

        if (has(fromDate) && has(toDate) && has(status)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate, status);
        }

        if (has(fromDate) && has(toDate)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate);
        }

        if (has(fromDate) && has(status)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, status);
        }

        if (has(toDate) && has(status)) {
            return jdbcTemplate.queryForList(
                    sql, toDate, status);
        }

        if (has(fromDate)) {
            return jdbcTemplate.queryForList(sql, fromDate);
        }

        if (has(toDate)) {
            return jdbcTemplate.queryForList(sql, toDate);
        }

        if (has(status)) {
            return jdbcTemplate.queryForList(sql, status);
        }

        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> queryDateOnly(
            String sql,
            String fromDate,
            String toDate) {

        if (has(fromDate) && has(toDate)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate);
        }

        if (has(fromDate)) {
            return jdbcTemplate.queryForList(sql, fromDate);
        }

        if (has(toDate)) {
            return jdbcTemplate.queryForList(sql, toDate);
        }

        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> queryDamaged(
            String sql,
            String fromDate,
            String toDate,
            String category) {

        if (has(fromDate) && has(toDate) && has(category)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate, category);
        }

        if (has(fromDate) && has(toDate)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate);
        }

        if (has(fromDate) && has(category)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, category);
        }

        if (has(toDate) && has(category)) {
            return jdbcTemplate.queryForList(
                    sql, toDate, category);
        }

        if (has(fromDate)) {
            return jdbcTemplate.queryForList(sql, fromDate);
        }

        if (has(toDate)) {
            return jdbcTemplate.queryForList(sql, toDate);
        }

        if (has(category)) {
            return jdbcTemplate.queryForList(sql, category);
        }

        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> queryReturns(
            String sql,
            String fromDate,
            String toDate,
            String status) {

        if (has(fromDate) && has(toDate) && has(status)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate, status);
        }

        if (has(fromDate) && has(toDate)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, toDate);
        }

        if (has(fromDate) && has(status)) {
            return jdbcTemplate.queryForList(
                    sql, fromDate, status);
        }

        if (has(toDate) && has(status)) {
            return jdbcTemplate.queryForList(
                    sql, toDate, status);
        }

        if (has(fromDate)) {
            return jdbcTemplate.queryForList(sql, fromDate);
        }

        if (has(toDate)) {
            return jdbcTemplate.queryForList(sql, toDate);
        }

        if (has(status)) {
            return jdbcTemplate.queryForList(sql, status);
        }

        return jdbcTemplate.queryForList(sql);
    }

    private boolean has(String value) {
        return value != null && !value.isBlank();
    }
}