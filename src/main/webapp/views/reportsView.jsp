<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.comspare.model.StockReportItem" %>
<%@ page import="com.comspare.model.SalesReportItem" %>
<!DOCTYPE html>
<html>
<head>
    <title>Reports - ComSpare</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 30px; background-color: #f4f6f9; }
        h2 { color: #333; margin-top: 30px; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; background: white; }
        th, td { padding: 10px; border: 1px solid #ddd; text-align: left; }
        th { background-color: #28a745; color: white; }
        .sales-th { background-color: #17a2b8; }
        .nav { margin-bottom: 20px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #007bff; font-weight: bold; }
    </style>
</head>
<body>
    <div class="nav">
        <a href="<%=request.getContextPath()%>/analytics">Dashboard</a>
        <a href="<%=request.getContextPath()%>/reports">Reports & Analytics</a>
    </div>

    <h2>Current Stock Inventory Report</h2>
    <table>
        <thead>
            <tr>
                <th>Part ID</th>
                <th>Part Name</th>
                <th>Category</th>
                <th>Current Stock</th>
                <th>Reorder Level</th>
                <th>Unit Price (LKR)</th>
            </tr>
        </thead>
        <tbody>
            <%
                List<StockReportItem> stocks = (List<StockReportItem>) request.getAttribute("stockList");
                if (stocks != null) {
                    for (StockReportItem s : stocks) {
            %>
            <tr>
                <td><%= s.getPartId() %></td>
                <td><%= s.getPartName() %></td>
                <td><%= s.getCategory() %></td>
                <td><%= s.getCurrentStock() %></td>
                <td><%= s.getReorderLevel() %></td>
                <td><%= s.getUnitPrice() %></td>
            </tr>
            <% } } %>
        </tbody>
    </table>

    <h2>Sales History Report</h2>
    <table>
        <thead>
            <tr>
                <th class="sales-th">Sale ID</th>
                <th class="sales-th">Part ID</th>
                <th class="sales-th">Part Name</th>
                <th class="sales-th">Qty Sold</th>
                <th class="sales-th">Total Price (LKR)</th>
                <th class="sales-th">Sale Date</th>
            </tr>
        </thead>
        <tbody>
            <%
                List<SalesReportItem> sales = (List<SalesReportItem>) request.getAttribute("salesList");
                if (sales != null) {
                    for (SalesReportItem sa : sales) {
            %>
            <tr>
                <td><%= sa.getSaleId() %></td>
                <td><%= sa.getPartId() %></td>
                <td><%= sa.getPartName() %></td>
                <td><%= sa.getQuantitySold() %></td>
                <td><%= sa.getTotalPrice() %></td>
                <td><%= sa.getSaleDate() %></td>
            </tr>
            <% } } %>
        </tbody>
    </table>
</body>
</html>