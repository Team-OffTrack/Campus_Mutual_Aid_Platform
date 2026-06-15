package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.CreateReportRequest;
import cn.seecoder.campushelp.dto.ReportResponse;
import cn.seecoder.campushelp.dto.ResolveReportRequest;
import cn.seecoder.campushelp.entity.Report;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.enums.BadgeDefinition;
import cn.seecoder.campushelp.entity.enums.NotificationType;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.MessageMapper;
import cn.seecoder.campushelp.mapper.ReportMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.BadgeService;
import cn.seecoder.campushelp.service.DemandService;
import cn.seecoder.campushelp.service.NotificationService;
import cn.seecoder.campushelp.service.ReportService;
import cn.seecoder.campushelp.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Set<String> VALID_TARGET_TYPES = Set.of("DEMAND", "USER", "MESSAGE");
    private static final Set<String> VALID_REASONS = Set.of("MISLEADING", "HARASSMENT", "ILLEGAL", "SPAM", "OTHER");
    private static final Set<String> VALID_RESOLVE_STATUSES = Set.of("RESOLVED", "DISMISSED");

    private static final Map<String, String> TARGET_TYPE_LABELS = Map.of(
            "DEMAND", "需求", "USER", "用户", "MESSAGE", "消息");

    private final ReportMapper reportMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;
    private final NotificationService notificationService;
    private final DemandService demandService;
    private final UserService userService;
    private final BadgeService badgeService;

    public ReportServiceImpl(ReportMapper reportMapper, DemandMapper demandMapper,
                              UserMapper userMapper, MessageMapper messageMapper,
                              NotificationService notificationService,
                              DemandService demandService,
                              UserService userService,
                              BadgeService badgeService) {
        this.reportMapper = reportMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
        this.messageMapper = messageMapper;
        this.notificationService = notificationService;
        this.demandService = demandService;
        this.userService = userService;
        this.badgeService = badgeService;
    }

    @Override
    @Transactional
    public void createReport(Long reporterId, CreateReportRequest request) {
        // Validate target type
        if (!VALID_TARGET_TYPES.contains(request.getTargetType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的举报目标类型");
        }

        // Validate reason
        if (!VALID_REASONS.contains(request.getReason())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的举报原因");
        }

        // Validate target exists
        boolean targetExists = switch (request.getTargetType()) {
            case "DEMAND" -> demandMapper.selectById(request.getTargetId()) != null;
            case "USER" -> userMapper.selectById(request.getTargetId()) != null;
            case "MESSAGE" -> messageMapper.selectById(request.getTargetId()) != null;
            default -> false;
        };
        if (!targetExists) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报目标不存在");
        }

        // Check duplicate pending report
        LambdaQueryWrapper<Report> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, request.getTargetType())
                .eq(Report::getTargetId, request.getTargetId())
                .eq(Report::getStatus, "PENDING");
        if (reportMapper.selectCount(dupWrapper) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "您已举报过该内容，请等待管理员处理");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus("PENDING");
        reportMapper.insert(report);

        // Notify all admins about the new report
        notifyAdmins(request);
    }

    private void notifyAdmins(CreateReportRequest request) {
        List<User> admins = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        String typeLabel = TARGET_TYPE_LABELS.getOrDefault(request.getTargetType(), request.getTargetType());
        for (User admin : admins) {
            notificationService.create(admin.getUserId(),
                    NotificationType.REPORT,
                    "新举报",
                    "用户举报了一条" + typeLabel + "，请查看处理",
                    null);
        }
    }

    @Override
    public Page<ReportResponse> listReports(int pageNum, int pageSize, String status) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreateTime);

        Page<Report> page = reportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Report> reports = page.getRecords();

        // Batch-load reporters
        List<Long> reporterIds = reports.stream()
                .map(Report::getReporterId).distinct().toList();
        Map<Long, User> userMap;
        if (!reporterIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(reporterIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
        } else {
            userMap = Map.of();
        }

        Page<ReportResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(reports.stream()
                .map(r -> ReportResponse.from(r, userMap.get(r.getReporterId())))
                .toList());
        return resultPage;
    }

    @Override
    @Transactional
    public void resolveReport(Long reportId, Long adminId, ResolveReportRequest request) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }

        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已被处理");
        }

        if (!VALID_RESOLVE_STATUSES.contains(request.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的处理状态");
        }

        // Execute admin action if specified
        String actionNote = executeAction(report, request.getAction(), adminId);

        report.setStatus(request.getStatus());
        String combinedNote = request.getAdminNote();
        if (actionNote != null) {
            combinedNote = (combinedNote != null ? combinedNote + "；" : "") + actionNote;
        }
        report.setAdminNote(combinedNote);
        report.setAdminId(adminId);
        report.setResolveTime(LocalDateTime.now());
        reportMapper.updateById(report);

        // Badge: FIRST_REPORT_SUCCESS — reporter's first resolved report
        if ("RESOLVED".equals(request.getStatus())) {
            badgeService.checkAndAward(report.getReporterId(), BadgeDefinition.FIRST_REPORT_SUCCESS.getKey());
        }
    }

    /**
     * Execute the admin action associated with this resolution.
     * Returns a note describing what was done, or null if no action taken.
     */
    private String executeAction(Report report, String action, Long adminId) {
        if (action == null) return null;

        if ("DELETE_DEMAND".equals(action) && "DEMAND".equals(report.getTargetType())) {
            demandService.adminCancelDemand(report.getTargetId(), adminId);
            return "已下架需求";
        }

        if ("BAN_USER".equals(action) && "USER".equals(report.getTargetType())) {
            userService.updateUserStatus(report.getTargetId(), 0, adminId);
            return "已封禁用户";
        }

        return null;
    }
}
