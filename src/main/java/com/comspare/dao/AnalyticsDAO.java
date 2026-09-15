package com.comspare.dao;

import com.comspare.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsDAO {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/comspare_db?useSSL=false&serverTimezone=UTC";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    public List<StockReportItem> getAllStockItems() {
        List<StockReportItem> list = new ArrayList<>();
        String sql = "SELECT * FROM parts";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new StockReportItem(
                        rs.getString("part_id"),
                        rs.getString("part_name"),
                        rs.getString("category"),
                        rs.getInt("current_stock"),
                        rs.getInt("reorder_level"),
                        rs.getDouble("unit_price")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<SalesReportItem> getAllSalesItems() {
        List<SalesReportItem> list = new ArrayList<>();
        String sql = "SELECT s.sale_id, s.part_id, p.part_name, s.quantity_sold, s.total_price, s.sale_date " +
                "FROM sales_history s JOIN parts p ON s.part_id = p.part_id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new SalesReportItem(
                        rs.getInt("sale_id"),
                        rs.getString("part_id"),
                        rs.getString("part_name"),
                        rs.getInt("quantity_sold"),
                        rs.getDouble("total_price"),
                        rs.getDate("sale_date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<ReorderPrediction> getLowStockItems() {
        List<ReorderPrediction> list = new ArrayList<>();
        String sql = "SELECT * FROM parts WHERE current_stock <= reorder_level";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int stock = rs.getInt("current_stock");
                int level = rs.getInt("reorder_level");
                int suggested = (level * 2) - stock;
                list.add(new ReorderPrediction(
                        rs.getString("part_id"),
                        rs.getString("part_name"),
                        stock,
                        level,
                        suggested,
                        "CRITICAL_LOW"
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}