package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.dto.AdminDashboardResponse;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.Report;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DailyCheckinMapper;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.PointsTransactionMapper;
import cn.seecoder.campushelp.mapper.ReportMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.AdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final DemandMapper demandMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final ReportMapper reportMapper;

    public AdminServiceImpl(UserMapper userMapper,
                            DemandMapper demandMapper,
                            PointsTransactionMapper pointsTransactionMapper,
                            DailyCheckinMapper dailyCheckinMapper,
                            ReportMapper reportMapper) {
        this.userMapper = userMapper;
        this.demandMapper = demandMapper;
        this.pointsTransactionMapper = pointsTransactionMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.reportMapper = reportMapper;
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        AdminDashboardResponse rsp = new AdminDashboardResponse();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDate todayDate = LocalDate.now();
        LocalDate sevenDaysAgo = todayDate.minusDays(6); // inclusive: today and 6 days before
        LocalDate thirtyDaysAgo = todayDate.minusDays(29);

        // ── User stats ──
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        rsp.setTotalUsers(totalUsers);

        long newUsersToday = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart));
        rsp.setNewUsersToday(newUsersToday);

        long activeUsers7d = dailyCheckinMapper.countDistinctUsersSince(sevenDaysAgo);
        rsp.setActiveUsers7d(activeUsers7d);

        long activeUsers30d = dailyCheckinMapper.countDistinctUsersSince(thirtyDaysAgo);
        rsp.setActiveUsers30d(activeUsers30d);

        // ── Demand stats ──
        long totalDemands = demandMapper.selectCount(new LambdaQueryWrapper<>());
        rsp.setTotalDemands(totalDemands);

        long newDemandsToday = demandMapper.selectCount(
                new LambdaQueryWrapper<Demand>().ge(Demand::getCreateTime, todayStart));
        rsp.setNewDemandsToday(newDemandsToday);

        // Demand type distribution (group by type)
        QueryWrapper<Demand> typeGroupWrapper = new QueryWrapper<>();
        typeGroupWrapper.select("type", "COUNT(*) as cnt").groupBy("type");
        List<Map<String, Object>> typeRows = demandMapper.selectMaps(typeGroupWrapper);
        Map<String, Long> typeDist = new LinkedHashMap<>();
        for (Map<String, Object> row : typeRows) {
            String type = (String) row.get("type");
            Object cntObj = row.get("cnt");
            long cnt = cntObj instanceof Number n ? n.longValue() : 0L;
            if (type != null) typeDist.put(type, cnt);
        }
        rsp.setDemandTypeDistribution(typeDist);

        // ── Points stats ──
        long totalPointsIssued = pointsTransactionMapper.sumPositivePoints();
        rsp.setTotalPointsIssued(totalPointsIssued);

        long checkinUsersToday = dailyCheckinMapper.selectCount(
                new LambdaQueryWrapper<cn.seecoder.campushelp.entity.DailyCheckin>()
                        .eq(cn.seecoder.campushelp.entity.DailyCheckin::getCheckinDate, todayDate));
        rsp.setCheckinRate(totalUsers > 0 ? (double) checkinUsersToday / totalUsers : 0.0);

        // ── Report stats ──
        long pendingReports = reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, "PENDING"));
        rsp.setPendingReportCount(pendingReports);

        return rsp;
    }
}
