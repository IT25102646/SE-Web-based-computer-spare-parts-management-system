package com.comspare.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Inventory and purchasing reports.
 * Every report is a live SQL query on the existing ComSpareDB tables (parts, part_history,
 * purchase_orders, purchase_order_items, suppliers) - there is no "reports" table.
 */
@Service
public class InventoryReportService {

    private final JdbcTemplate jdbc;

    public InventoryReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 1. STOCK REPORT
    public List<Map<String, Object>> getStockReport(String category, String status) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       p.brand AS Brand, p.stock_quantity AS StockQuantity, p.reorder_level AS ReorderLevel,
                       p.price AS UnitPrice, (p.stock_quantity * p.price) AS StockValue, p.location AS Location
                FROM parts p
                WHERE p.is_active = 1
                """);
        q.and("p.category = ?", category);
        if (status != null) {
            switch (status.toUpperCase()) {
                case "LOW" -> q.raw("AND p.stock_quantity > 0 AND p.stock_quantity <= p.reorder_level");
                case "OUT" -> q.raw("AND p.stock_quantity = 0");
                case "AVAILABLE" -> q.raw("AND p.stock_quantity > p.reorder_level");
                default -> { }
            }
        }
        return q.raw("ORDER BY p.name").run(jdbc);
    }

    // 2. LOW STOCK
    public List<Map<String, Object>> getLowStockReport(String category) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       p.brand AS Brand, p.stock_quantity AS CurrentStock, p.reorder_level AS ReorderLevel,
                       (p.reorder_level - p.stock_quantity) AS Shortage, p.price AS UnitPrice,
                       ((p.reorder_level - p.stock_quantity) * p.price) AS EstimatedReorderValue,
                       p.location AS Location
                FROM parts p
                WHERE p.is_active = 1 AND p.stock_quantity > 0 AND p.stock_quantity <= p.reorder_level
                """);
        return q.and("p.category = ?", category).raw("ORDER BY p.stock_quantity ASC, p.name ASC").run(jdbc);
    }

    // 3. OUT OF STOCK
    public List<Map<String, Object>> getOutOfStockReport(String category) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       p.brand AS Brand, p.stock_quantity AS CurrentStock, p.reorder_level AS ReorderLevel,
                       p.price AS UnitPrice, p.location AS Location
                FROM parts p
                WHERE p.is_active = 1 AND p.stock_quantity = 0
                """);
        return q.and("p.category = ?", category).raw("ORDER BY p.name ASC").run(jdbc);
    }

    // 4. STOCK MOVEMENT (part_history: RECEIVED, SOLD, RETURNED, ADJUSTED, DAMAGED)
    public List<Map<String, Object>> getMovementReport(String fromDate, String toDate, String category) {
        ReportQuery q = new ReportQuery("""
                SELECT ph.id AS MovementID, p.product_code AS ProductCode, p.name AS PartName,
                       p.category AS Category, ph.event_type AS MovementType, ph.quantity AS Quantity,
                       ph.event_date AS MovementDate, ph.notes AS Notes
                FROM part_history ph
                INNER JOIN parts p ON ph.part_id = p.id
                WHERE 1 = 1
                """);
        q.and("ph.event_date >= CAST(? AS DATETIME2)", fromDate);
        q.and("ph.event_date < DATEADD(DAY, 1, CAST(? AS DATE))", toDate);
        q.and("p.category = ?", category);
        return q.raw("ORDER BY ph.event_date DESC").run(jdbc);
    }

    // 5. VALUATION
    public List<Map<String, Object>> getValuationReport(String category) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       p.stock_quantity AS StockQuantity, p.price AS UnitPrice,
                       (p.stock_quantity * p.price) AS InventoryValue, p.location AS Location
                FROM parts p
                WHERE p.is_active = 1
                """);
        return q.and("p.category = ?", category).raw("ORDER BY (p.stock_quantity * p.price) DESC").run(jdbc);
    }

    // 6. PURCHASE REPORT
    public List<Map<String, Object>> getPurchaseReport(String fromDate, String toDate, String status) {
        ReportQuery q = new ReportQuery("""
                SELECT po.order_number AS PurchaseOrder, s.supplier_name AS Supplier,
                       po.order_date AS OrderDate, po.expected_delivery_date AS ExpectedDelivery,
                       po.delivery_date AS DeliveryDate, po.status AS Status,
                       p.product_code AS ProductCode, p.name AS PartName,
                       poi.ordered_quantity AS OrderedQuantity, poi.received_quantity AS ReceivedQuantity,
                       poi.unit_cost AS UnitCost,
                       (poi.ordered_quantity * ISNULL(poi.unit_cost, 0)) AS OrderedValue,
                       (ISNULL(poi.received_quantity, 0) * ISNULL(poi.unit_cost, 0)) AS ReceivedValue
                FROM purchase_orders po
                INNER JOIN suppliers s ON po.supplier_id = s.id
                INNER JOIN purchase_order_items poi ON po.id = poi.purchase_order_id
                INNER JOIN parts p ON poi.part_id = p.id
                WHERE 1 = 1
                """);
        q.and("po.order_date >= CAST(? AS DATE)", fromDate);
        q.and("po.order_date <= CAST(? AS DATE)", toDate);
        q.and("po.status = ?", status);
        return q.raw("ORDER BY po.order_date DESC, po.order_number").run(jdbc);
    }

    // 7. PURCHASE EXPENSE (cancelled orders are not an expense)
    public List<Map<String, Object>> getPurchaseExpenseReport(String fromDate, String toDate) {
        ReportQuery q = new ReportQuery("""
                SELECT po.order_number AS PurchaseOrder, s.supplier_name AS Supplier,
                       po.order_date AS OrderDate, po.status AS Status,
                       SUM(poi.ordered_quantity * ISNULL(poi.unit_cost, 0)) AS TotalPurchaseExpense,
                       SUM(ISNULL(poi.received_quantity, 0) * ISNULL(poi.unit_cost, 0)) AS ReceivedStockExpense
                FROM purchase_orders po
                INNER JOIN suppliers s ON po.supplier_id = s.id
                INNER JOIN purchase_order_items poi ON po.id = poi.purchase_order_id
                WHERE po.status <> 'CANCELLED'
                """);
        q.and("po.order_date >= CAST(? AS DATE)", fromDate);
        q.and("po.order_date <= CAST(? AS DATE)", toDate);
        return q.raw("GROUP BY po.order_number, s.supplier_name, po.order_date, po.status ORDER BY po.order_date DESC")
                .run(jdbc);
    }

    // 8. SUPPLIER PERFORMANCE (counts are DISTINCT so item rows do not inflate them)
    public List<Map<String, Object>> getSupplierPerformanceReport(String fromDate, String toDate) {
        ReportQuery q = new ReportQuery("""
                SELECT s.supplier_name AS Supplier,
                       COUNT(DISTINCT po.id) AS TotalOrders,
                       COUNT(DISTINCT CASE WHEN po.status = 'RECEIVED' THEN po.id END) AS ReceivedOrders,
                       COUNT(DISTINCT CASE WHEN po.delivery_date IS NOT NULL
                                            AND po.expected_delivery_date IS NOT NULL
                                            AND po.delivery_date <= po.expected_delivery_date
                                           THEN po.id END) AS OnTimeDeliveries,
                       ISNULL(SUM(poi.ordered_quantity), 0) AS TotalOrderedQuantity,
                       ISNULL(SUM(poi.received_quantity), 0) AS TotalReceivedQuantity,
                       CAST(CASE WHEN ISNULL(SUM(poi.ordered_quantity), 0) = 0 THEN 0
                                 ELSE SUM(ISNULL(poi.received_quantity, 0)) * 100.0 / SUM(poi.ordered_quantity)
                            END AS DECIMAL(10,2)) AS FulfillmentRate
                FROM suppliers s
                LEFT JOIN purchase_orders po ON s.id = po.supplier_id
                LEFT JOIN purchase_order_items poi ON po.id = poi.purchase_order_id
                WHERE (po.status IS NULL OR po.status <> 'CANCELLED')
                """);
        q.and("(po.order_date IS NULL OR po.order_date >= CAST(? AS DATE))", fromDate);
        q.and("(po.order_date IS NULL OR po.order_date <= CAST(? AS DATE))", toDate);
        return q.raw("GROUP BY s.id, s.supplier_name ORDER BY s.supplier_name").run(jdbc);
    }

    // 9. REORDER PREDICTION: suggests topping stock up to double the reorder level
    public List<Map<String, Object>> getReorderPredictions(String category) {
        ReportQuery q = new ReportQuery("""
                SELECT p.product_code AS ProductCode, p.name AS PartName, p.category AS Category,
                       p.stock_quantity AS CurrentStock, p.reorder_level AS ReorderLevel,
                       ((p.reorder_level * 2) - p.stock_quantity) AS SuggestedReorderQty,
                       p.price AS UnitPrice,
                       (((p.reorder_level * 2) - p.stock_quantity) * p.price) AS EstimatedReorderCost,
                       CASE WHEN p.stock_quantity = 0 THEN 'OUT_OF_STOCK' ELSE 'CRITICAL_LOW' END AS Status
                FROM parts p
                WHERE p.is_active = 1 AND p.stock_quantity <= p.reorder_level
                """);
        return q.and("p.category = ?", category).raw("ORDER BY (p.stock_quantity - p.reorder_level) ASC").run(jdbc);
    }
}
