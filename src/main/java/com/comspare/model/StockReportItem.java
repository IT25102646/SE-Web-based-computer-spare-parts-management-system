package com.comspare.model;

public class StockReportItem {
    private String partId;
    private String partName;
    private String category;
    private int currentStock;
    private int reorderLevel;
    private double unitPrice;

    public StockReportItem() {}

    public StockReportItem(String partId, String partName, String category, int currentStock, int reorderLevel, double unitPrice) {
        this.partId = partId;
        this.partName = partName;
        this.category = category;
        this.currentStock = currentStock;
        this.reorderLevel = reorderLevel;
        this.unitPrice = unitPrice;
    }

    public String getPartId() { return partId; }
    public void setPartId(String partId) { this.partId = partId; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
