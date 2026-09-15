<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.comspare.model.ReorderPrediction" %>
<!DOCTYPE html>
<html>
<head>
    <title>Stock Prediction & Analytics</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 30px; background-color: #f4f6f9; }
        h2 { color: #333; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; background: white; }
        th, td { padding: 12px; border: 1px solid #ddd; text-align: left; }
        th { background-color: #007bff; color: white; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .badge-danger { background-color: #dc3545; color: white; padding: 4px 8px; border-radius: 4px; }
        .nav { margin-bottom: 20px; }
        .nav a { margin-right: 15px; text-decoration: none; color: #007bff; font-weight: bold; }
    </style>
</head>
<body>
    <div class="nav">
        <a href="<%=request.getContextPath()%>/analytics">Dashboard</a>
        <a href="<%=request.getContextPath()%>/reports">Reports & Analytics</a>
    </div>

    <h2>Reorder Stock Predictions</h2>
    <table>
        <thead>
            <tr>
                <th>Part ID</th>
                <th>Part Name</th>
                <th>Current Stock</th>
                <th>Reorder Level</th>
                <th>Suggested Order Qty</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody>
            <%
                List<ReorderPrediction> list = (List<ReorderPrediction>) request.getAttribute("predictions");
                if (list != null && !list.isEmpty()) {
                    for (ReorderPrediction item : list) {
            %>
            <tr>
                <td><%= item.getPartId() %></td>
                <td><%= item.getPartName() %></td>
                <td><%= item.getCurrentStock() %></td>
                <td><%= item.getReorderLevel() %></td>
                <td><strong><%= item.getSuggestedReorderQty() %></strong></td>
                <td><span class="badge-danger"><%= item.getStatus() %></span></td>
            </tr>
            <%
                    }
                } else {
            %>
            <tr>
                <td colspan="6">All stock levels are sufficient. No reorders needed!</td>
            </tr>
            <% } %>
        </tbody>
    </table>
</body>
</html>