package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.Report;
import cn.seecoder.campushelp.entity.User;

import java.time.LocalDateTime;

public class ReportResponse {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String reason;
    private String description;
    private String status;
    private String adminNote;
    private Long adminId;
    private LocalDateTime createTime;
    private LocalDateTime resolveTime;

    public static ReportResponse from(Report report, User reporter) {
        ReportResponse rsp = new ReportResponse();
        rsp.id = report.getId();
        rsp.reporterId = report.getReporterId();
        rsp.reporterName = reporter != null ? reporter.getName() : null;
        rsp.targetType = report.getTargetType();
        rsp.targetId = report.getTargetId();
        rsp.reason = report.getReason();
        rsp.description = report.getDescription();
        rsp.status = report.getStatus();
        rsp.adminNote = report.getAdminNote();
        rsp.adminId = report.getAdminId();
        rsp.createTime = report.getCreateTime();
        rsp.resolveTime = report.getResolveTime();
        return rsp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getResolveTime() { return resolveTime; }
    public void setResolveTime(LocalDateTime resolveTime) { this.resolveTime = resolveTime; }
}
