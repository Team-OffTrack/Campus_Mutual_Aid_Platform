package cn.seecoder.campushelp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResolveReportRequest {

    @NotBlank(message = "处理状态不能为空")
    private String status;

    @Size(max = 512, message = "处理备注不能超过512字")
    private String adminNote;

    private String action;  // null = 仅标记, "DELETE_DEMAND" = 下架需求, "BAN_USER" = 封禁用户

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
