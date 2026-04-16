package com.pes.marketplace.dto;

import java.math.BigDecimal;

/**
 * Carries aggregated statistics for the admin report (UC8).
 * SOLID – SRP: Only a data carrier; no computation logic.
 */
public class ReportDto {
    private long totalUsers;
    private long totalBuyers;
    private long totalSellers;
    private long pendingListings;
    private long approvedListings;
    private long rejectedListings;
    private long soldListings;
    private long totalOrders;
    private BigDecimal totalRevenue;

    // Getters & Setters
    public long getTotalUsers()                       { return totalUsers; }
    public void setTotalUsers(long v)                 { this.totalUsers = v; }
    public long getTotalBuyers()                      { return totalBuyers; }
    public void setTotalBuyers(long v)                { this.totalBuyers = v; }
    public long getTotalSellers()                     { return totalSellers; }
    public void setTotalSellers(long v)               { this.totalSellers = v; }
    public long getPendingListings()                  { return pendingListings; }
    public void setPendingListings(long v)            { this.pendingListings = v; }
    public long getApprovedListings()                 { return approvedListings; }
    public void setApprovedListings(long v)           { this.approvedListings = v; }
    public long getRejectedListings()                 { return rejectedListings; }
    public void setRejectedListings(long v)           { this.rejectedListings = v; }
    public long getSoldListings()                     { return soldListings; }
    public void setSoldListings(long v)               { this.soldListings = v; }
    public long getTotalOrders()                      { return totalOrders; }
    public void setTotalOrders(long v)                { this.totalOrders = v; }
    public BigDecimal getTotalRevenue()               { return totalRevenue; }
    public void setTotalRevenue(BigDecimal v)         { this.totalRevenue = v; }
}
