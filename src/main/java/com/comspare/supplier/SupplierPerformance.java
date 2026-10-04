package com.comspare.supplier;

public class SupplierPerformance {

    private Long supplierId;
    private String supplierName;
    private long totalDeliveries;
    private long onTimeDeliveries;
    private long lateDeliveries;
    private double reliabilityPercentage;

    public SupplierPerformance() {
    }

    public SupplierPerformance(
            Long supplierId,
            String supplierName,
            long totalDeliveries,
            long onTimeDeliveries,
            long lateDeliveries,
            double reliabilityPercentage) {

        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.totalDeliveries = totalDeliveries;
        this.onTimeDeliveries = onTimeDeliveries;
        this.lateDeliveries = lateDeliveries;
        this.reliabilityPercentage = reliabilityPercentage;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public long getTotalDeliveries() {
        return totalDeliveries;
    }

    public long getOnTimeDeliveries() {
        return onTimeDeliveries;
    }

    public long getLateDeliveries() {
        return lateDeliveries;
    }

    public double getReliabilityPercentage() {
        return reliabilityPercentage;
    }
}