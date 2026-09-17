package com.comspare.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InventoryReportService {

    private final JdbcTemplate jdbcTemplate;

    public InventoryReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================
    // 1. STOCK REPORT
    // =========================================================

    public List<Map<String, Object>> getStockReport(
            String category,
            String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    p.brand AS Brand,
                    p.stock_quantity AS StockQuantity,
                    p.reorder_level AS ReorderLevel,
                    p.price AS UnitPrice,
                    (p.stock_quantity * p.price) AS StockValue,
                    p.location AS Location
                FROM parts p
                WHERE 1 = 1
                """);

        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ? ");
        }

        if (status != null && !status.isBlank()) {
            switch (status.toUpperCase()) {
                case "LOW" ->
                        sql.append(" AND p.stock_quantity > 0 AND p.stock_quantity <= p.reorder_level ");
                case "OUT" ->
                        sql.append(" AND p.stock_quantity = 0 ");
                case "AVAILABLE" ->
                        sql.append(" AND p.stock_quantity > p.reorder_level ");
            }
        }

        sql.append(" ORDER BY p.name ");

        if (category != null && !category.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), category);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // 2. LOW STOCK REPORT
    // =========================================================

    public List<Map<String, Object>> getLowStockReport(
            String category) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    p.brand AS Brand,
                    p.stock_quantity AS CurrentStock,
                    p.reorder_level AS ReorderLevel,
                    (p.reorder_level - p.stock_quantity) AS Shortage,
                    p.price AS UnitPrice,
                    ((p.reorder_level - p.stock_quantity) * p.price) AS EstimatedReorderValue,
                    p.location AS Location
                FROM parts p
                WHERE p.stock_quantity > 0
                  AND p.stock_quantity <= p.reorder_level
                """);

        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ? ");
        }

        sql.append(" ORDER BY p.stock_quantity ASC, p.name ASC ");

        if (category != null && !category.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), category);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // 3. OUT OF STOCK REPORT
    // =========================================================

    public List<Map<String, Object>> getOutOfStockReport(
            String category) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    p.brand AS Brand,
                    p.stock_quantity AS CurrentStock,
                    p.reorder_level AS ReorderLevel,
                    p.price AS UnitPrice,
                    p.location AS Location
                FROM parts p
                WHERE p.stock_quantity = 0
                """);

        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ? ");
        }

        sql.append(" ORDER BY p.name ASC ");

        if (category != null && !category.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), category);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // 4. STOCK MOVEMENT REPORT
    // Uses part_history because it records RECEIVED, SOLD,
    // RETURNED, ADJUSTED and DAMAGED events.
    // =========================================================

    public List<Map<String, Object>> getMovementReport(
            String fromDate,
            String toDate,
            String category) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    ph.id AS MovementID,
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    ph.event_type AS MovementType,
                    ph.quantity AS Quantity,
                    ph.event_date AS MovementDate,
                    ph.notes AS Notes
                FROM part_history ph
                INNER JOIN parts p
                    ON ph.part_id = p.id
                WHERE 1 = 1
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

        return queryWithDates(sql.toString(), fromDate, toDate, category);
    }

    // =========================================================
    // 5. INVENTORY VALUATION REPORT
    // =========================================================

    public List<Map<String, Object>> getValuationReport(
            String category) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    p.category AS Category,
                    p.stock_quantity AS StockQuantity,
                    p.price AS UnitPrice,
                    (p.stock_quantity * p.price) AS InventoryValue,
                    p.location AS Location
                FROM parts p
                WHERE 1 = 1
                """);

        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ? ");
        }

        sql.append(" ORDER BY InventoryValue DESC ");

        if (category != null && !category.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), category);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // 6. PURCHASE REPORT
    // =========================================================

    public List<Map<String, Object>> getPurchaseReport(
            String fromDate,
            String toDate,
            String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    po.order_number AS PurchaseOrder,
                    s.supplier_name AS Supplier,
                    po.order_date AS OrderDate,
                    po.expected_delivery_date AS ExpectedDelivery,
                    po.delivery_date AS DeliveryDate,
                    po.status AS Status,
                    p.product_code AS ProductCode,
                    p.name AS PartName,
                    poi.ordered_quantity AS OrderedQuantity,
                    poi.received_quantity AS ReceivedQuantity,
                    poi.unit_cost AS UnitCost,
                    (poi.ordered_quantity * ISNULL(poi.unit_cost, 0)) AS OrderedValue,
                    (poi.received_quantity * ISNULL(poi.unit_cost, 0)) AS ReceivedValue
                FROM purchase_orders po
                INNER JOIN suppliers s
                    ON po.supplier_id = s.id
                INNER JOIN purchase_order_items poi
                    ON po.id = poi.purchase_order_id
                INNER JOIN parts p
                    ON poi.part_id = p.id
                WHERE 1 = 1
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND po.order_date >= CAST(? AS DATE) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND po.order_date <= CAST(? AS DATE) ");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND po.status = ? ");
        }

        sql.append(" ORDER BY po.order_date DESC, po.order_number ");

        return queryPurchase(sql.toString(), fromDate, toDate, status);
    }

    // =========================================================
    // 7. PURCHASE EXPENSE REPORT
    // =========================================================

    public List<Map<String, Object>> getPurchaseExpenseReport(
            String fromDate,
            String toDate) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    po.order_number AS PurchaseOrder,
                    s.supplier_name AS Supplier,
                    po.order_date AS OrderDate,
                    po.status AS Status,
                    SUM(poi.ordered_quantity * ISNULL(poi.unit_cost, 0))
                        AS TotalPurchaseExpense,
                    SUM(poi.received_quantity * ISNULL(poi.unit_cost, 0))
                        AS ReceivedStockExpense
                FROM purchase_orders po
                INNER JOIN suppliers s
                    ON po.supplier_id = s.id
                INNER JOIN purchase_order_items poi
                    ON po.id = poi.purchase_order_id
                WHERE 1 = 1
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND po.order_date >= CAST(? AS DATE) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND po.order_date <= CAST(? AS DATE) ");
        }

        sql.append("""
                GROUP BY
                    po.order_number,
                    s.supplier_name,
                    po.order_date,
                    po.status
                ORDER BY po.order_date DESC
                """);

        if (fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank()) {
            return jdbcTemplate.queryForList(
                    sql.toString(),
                    fromDate,
                    toDate
            );
        }

        if (fromDate != null && !fromDate.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), toDate);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // 8. SUPPLIER PERFORMANCE REPORT
    // =========================================================

    public List<Map<String, Object>> getSupplierPerformanceReport(
            String fromDate,
            String toDate) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    s.supplier_name AS Supplier,
                    COUNT(po.id) AS TotalOrders,
                    SUM(CASE
                        WHEN po.status = 'DELIVERED' THEN 1
                        ELSE 0
                    END) AS DeliveredOrders,
                    SUM(CASE
                        WHEN po.delivery_date IS NOT NULL
                             AND po.expected_delivery_date IS NOT NULL
                             AND po.delivery_date <= po.expected_delivery_date
                        THEN 1
                        ELSE 0
                    END) AS OnTimeDeliveries,
                    SUM(poi.ordered_quantity) AS TotalOrderedQuantity,
                    SUM(poi.received_quantity) AS TotalReceivedQuantity,
                    CAST(
                        CASE
                            WHEN SUM(poi.ordered_quantity) = 0 THEN 0
                            ELSE
                                SUM(poi.received_quantity) * 100.0
                                / SUM(poi.ordered_quantity)
                        END
                        AS DECIMAL(10,2)
                    ) AS FulfillmentRate
                FROM suppliers s
                LEFT JOIN purchase_orders po
                    ON s.id = po.supplier_id
                LEFT JOIN purchase_order_items poi
                    ON po.id = poi.purchase_order_id
                WHERE 1 = 1
                """);

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND (po.order_date IS NULL OR po.order_date >= CAST(? AS DATE)) ");
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND (po.order_date IS NULL OR po.order_date <= CAST(? AS DATE)) ");
        }

        sql.append("""
                GROUP BY s.id, s.supplier_name
                ORDER BY s.supplier_name
                """);

        if (fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), fromDate, toDate);
        }

        if (fromDate != null && !fromDate.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), toDate);
        }

        return jdbcTemplate.queryForList(sql.toString());
    }

    // =========================================================
    // HELPER: MOVEMENT QUERY
    // =========================================================

    private List<Map<String, Object>> queryWithDates(
            String sql,
            String fromDate,
            String toDate,
            String category) {

        if (fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank()
                && category != null && !category.isBlank()) {

            return jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    toDate,
                    category
            );
        }

        if (fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank()) {

            return jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    toDate
            );
        }

        if (fromDate != null && !fromDate.isBlank()
                && category != null && !category.isBlank()) {

            return jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    category
            );
        }

        if (toDate != null && !toDate.isBlank()
                && category != null && !category.isBlank()) {

            return jdbcTemplate.queryForList(
                    sql,
                    toDate,
                    category
            );
        }

        if (fromDate != null && !fromDate.isBlank()) {
            return jdbcTemplate.queryForList(sql, fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            return jdbcTemplate.queryForList(sql, toDate);
        }

        if (category != null && !category.isBlank()) {
            return jdbcTemplate.queryForList(sql, category);
        }

        return jdbcTemplate.queryForList(sql);
    }

    // =========================================================
    // HELPER: PURCHASE QUERY
    // =========================================================

    private List<Map<String, Object>> queryPurchase(
            String sql,
            String fromDate,
            String toDate,
            String status) {

        if (fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank()
                && status != null && !status.isBlank()) {

            return jdbcTemplate.queryForList(
                    sql,
                    fromDate,
                    toDate,
                    status
            );
        }

        if (fromDate != null && !fromDate.isBlank()
                && toDate != null && !toDate.isBlank()) {

            return jdbcTemplate.queryForList(sql, fromDate, toDate);
        }

        if (fromDate != null && !fromDate.isBlank()
                && status != null && !status.isBlank()) {

            return jdbcTemplate.queryForList(sql, fromDate, status);
        }

        if (toDate != null && !toDate.isBlank()
                && status != null && !status.isBlank()) {

            return jdbcTemplate.queryForList(sql, toDate, status);
        }

        if (fromDate != null && !fromDate.isBlank()) {
            return jdbcTemplate.queryForList(sql, fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            return jdbcTemplate.queryForList(sql, toDate);
        }

        if (status != null && !status.isBlank()) {
            return jdbcTemplate.queryForList(sql, status);
        }

        return jdbcTemplate.queryForList(sql);
    }
}