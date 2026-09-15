package com.comspare.controller;

import com.comspare.service.AnalyticsService;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/analytics")
public class AnalyticsController extends HttpServlet {
    private final AnalyticsService service = new AnalyticsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("predictions", service.getReorderPredictions());
        req.getRequestDispatcher("/views/analyticsDashboard.jsp").forward(req, resp);
    }
}