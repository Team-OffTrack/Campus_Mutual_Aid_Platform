package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.CreateReportRequest;
import cn.seecoder.campushelp.dto.ReportResponse;
import cn.seecoder.campushelp.dto.ResolveReportRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ReportService {

    void createReport(Long reporterId, CreateReportRequest request);

    Page<ReportResponse> listReports(int pageNum, int pageSize, String status);

    void resolveReport(Long reportId, Long adminId, ResolveReportRequest request);
}
