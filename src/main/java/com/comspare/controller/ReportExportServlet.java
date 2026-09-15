package com.comspare.controller;

import com.comspare.service.AnalyticsService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/reports")
public class ReportExportServlet extends HttpServlet {
    private final AnalyticsService service = new AnalyticsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("stockList", service.getStockReport());
        req.setAttribute("salesList", service.getSalesReport());
        req.getRequestDispatcher("/views/reportsView.jsp").forward(req, resp);
    }
}