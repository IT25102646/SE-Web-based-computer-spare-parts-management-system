package com.comspare.service;

import com.comspare.dao.AnalyticsDAO;
import com.comspare.model.*;
import java.util.List;

public class AnalyticsService {
    private final AnalyticsDAO dao = new AnalyticsDAO();

    public List<StockReportItem> getStockReport() {
        return dao.getAllStockItems();
    }

    public List<SalesReportItem> getSalesReport() {
        return dao.getAllSalesItems();
    }

    public List<ReorderPrediction> getReorderPredictions() {
        return dao.getLowStockItems();
    }
}