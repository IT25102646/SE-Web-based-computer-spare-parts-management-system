package com.comspare.model;

import java.sql.Date;

public class SalesReportItem {
    private int saleId;
    private String partId;
    private String partName;
    private int quantitySold;
    private double totalPrice;
    private Date saleDate;

    public SalesReportItem() {}

    public SalesReportItem(int saleId, String partId, String partName, int quantitySold, double totalPrice, Date saleDate) {
        this.saleId = saleId;
        this.partId = partId;
        this.partName = partName;
        this.quantitySold = quantitySold;
        this.totalPrice = totalPrice;
        this.saleDate = saleDate;
    }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public String getPartId() { return partId; }
    public void setPartId(String partId) { this.partId = partId; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public int getQuantitySold() { return quantitySold; }
    public void setQuantitySold(int quantitySold) { this.quantitySold = quantitySold; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public Date getSaleDate() { return saleDate; }
    public void setSaleDate(Date saleDate) { this.saleDate = saleDate; }
}
