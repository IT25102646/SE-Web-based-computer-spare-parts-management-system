package com.comspare.model;

public class ReorderPrediction {
    private String partId;
    private String partName;
    private int currentStock;
    private int reorderLevel;
    private int suggestedReorderQty;
    private String status;

    public ReorderPrediction() {}

    public ReorderPrediction(String partId, String partName, int currentStock, int reorderLevel, int suggestedReorderQty, String status) {
        this.partId = partId;
        this.partName = partName;
        this.currentStock = currentStock;
        this.reorderLevel = reorderLevel;
        this.suggestedReorderQty = suggestedReorderQty;
        this.status = status;
    }

    public String getPartId() { return partId; }
    public void setPartId(String partId) { this.partId = partId; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public int getSuggestedReorderQty() { return suggestedReorderQty; }
    public void setSuggestedReorderQty(int suggestedReorderQty) { this.suggestedReorderQty = suggestedReorderQty; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}