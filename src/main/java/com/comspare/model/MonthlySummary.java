package com.comspare.model;

public class MonthlySummary {
    private String month;
    private double totalRevenue;
    private int totalItemsSold;

    public MonthlySummary() {}

    public MonthlySummary(String month, double totalRevenue, int totalItemsSold) {
        this.month = month;
        this.totalRevenue = totalRevenue;
        this.totalItemsSold = totalItemsSold;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getTotalItemsSold() { return totalItemsSold; }
    public void setTotalItemsSold(int totalItemsSold) { this.totalItemsSold = totalItemsSold; }
}