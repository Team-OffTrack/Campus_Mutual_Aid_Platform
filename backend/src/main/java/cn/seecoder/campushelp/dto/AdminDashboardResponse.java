package cn.seecoder.campushelp.dto;

import java.util.Map;

/**
 * Admin dashboard statistics.
 */
public class AdminDashboardResponse {

    // User stats
    private long totalUsers;
    private long newUsersToday;
    private long activeUsers7d;
    private long activeUsers30d;

    // Demand stats
    private long totalDemands;
    private long newDemandsToday;
    private Map<String, Long> demandTypeDistribution;

    // Points stats
    private long totalPointsIssued;
    private double checkinRate; // 0.0 ~ 1.0

    // Report stats
    private long pendingReportCount;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getNewUsersToday() { return newUsersToday; }
    public void setNewUsersToday(long newUsersToday) { this.newUsersToday = newUsersToday; }

    public long getActiveUsers7d() { return activeUsers7d; }
    public void setActiveUsers7d(long activeUsers7d) { this.activeUsers7d = activeUsers7d; }

    public long getActiveUsers30d() { return activeUsers30d; }
    public void setActiveUsers30d(long activeUsers30d) { this.activeUsers30d = activeUsers30d; }

    public long getTotalDemands() { return totalDemands; }
    public void setTotalDemands(long totalDemands) { this.totalDemands = totalDemands; }

    public long getNewDemandsToday() { return newDemandsToday; }
    public void setNewDemandsToday(long newDemandsToday) { this.newDemandsToday = newDemandsToday; }

    public Map<String, Long> getDemandTypeDistribution() { return demandTypeDistribution; }
    public void setDemandTypeDistribution(Map<String, Long> demandTypeDistribution) { this.demandTypeDistribution = demandTypeDistribution; }

    public long getTotalPointsIssued() { return totalPointsIssued; }
    public void setTotalPointsIssued(long totalPointsIssued) { this.totalPointsIssued = totalPointsIssued; }

    public double getCheckinRate() { return checkinRate; }
    public void setCheckinRate(double checkinRate) { this.checkinRate = checkinRate; }

    public long getPendingReportCount() { return pendingReportCount; }
    public void setPendingReportCount(long pendingReportCount) { this.pendingReportCount = pendingReportCount; }
}
