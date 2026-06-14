package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.dto.TeamMemberResponse;
import cn.seecoder.campushelp.dto.UpdateDemandRequest;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.enums.DemandStatus;
import cn.seecoder.campushelp.entity.enums.NotificationType;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.DemandService;
import cn.seecoder.campushelp.service.NotificationService;
import cn.seecoder.campushelp.service.PointsService;
import cn.seecoder.campushelp.service.TeamMemberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DemandServiceImpl implements DemandService {

    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final TeamMemberService teamMemberService;
    private final PointsService pointsService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public DemandServiceImpl(DemandMapper demandMapper, UserMapper userMapper,
                             NotificationService notificationService,
                             TeamMemberService teamMemberService,
                             PointsService pointsService) {
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.teamMemberService = teamMemberService;
        this.pointsService = pointsService;
    }

    @Override
    @Transactional
    public DemandResponse publish(Long publisherId, CreateDemandRequest request) {
        Demand demand = new Demand();
        demand.setPublisherId(publisherId);
        demand.setType(request.getType());
        demand.setTitle(request.getTitle());
        demand.setDescription(request.getDescription());
        demand.setLocation(request.getLocation());
        demand.setDeadline(request.getDeadline());
        demand.setRewardType(request.getRewardType() != null ? request.getRewardType() : "point");
        demand.setRewardAmount(request.getRewardAmount() != null ? request.getRewardAmount() : 0);
        demand.setIsAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous() ? 1 : 0);
        demand.setImages(request.getImages());
        demand.setStatus(DemandStatus.OPEN);

        // Serialize type-specific attributes to JSON
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            validateAttributes(request.getType(), request.getAttributes());
            try {
                demand.setAttributes(OBJECT_MAPPER.writeValueAsString(request.getAttributes()));
            } catch (IOException e) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "属性数据格式错误");
            }
        }

        if (demand.getRewardAmount() < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "悬赏金额不能为负数");
        }
        if (demand.getDeadline() != null && demand.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "截止时间不能是过去的时间");
        }

        demandMapper.insert(demand);

        // Auto-join publisher as leader for team demands
        if ("team".equals(request.getType())) {
            teamMemberService.autoJoinLeader(demand.getDemandId(), publisherId);
        }

        // Freeze points if this is a point-reward demand
        if ("point".equals(demand.getRewardType()) && demand.getRewardAmount() > 0) {
            pointsService.freezeOnPublish(publisherId, demand.getRewardAmount(), demand.getDemandId());
        }

        User publisher = userMapper.selectById(publisherId);
        return DemandResponse.from(demand, publisher);
    }

    @Override
    public String uploadImage(Long userId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只支持图片文件");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || ext.isBlank()) ext = "jpg";
        String filename = userId + "_" + System.currentTimeMillis() + "." + ext.toLowerCase();

        try {
            Path dir = Path.of("uploads", "demands");
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toAbsolutePath());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "图片上传失败");
        }

        return "/uploads/demands/" + filename;
    }

    @Override
    public Page<DemandResponse> list(int pageNum, int pageSize, String type, String keyword, String sortBy) {
        LambdaQueryWrapper<Demand> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(Demand::getType, type);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Demand::getTitle, keyword).or().like(Demand::getDescription, keyword));
        }
        // Only show open/in-progress demands in list by default
        wrapper.in(Demand::getStatus, DemandStatus.OPEN, DemandStatus.IN_PROGRESS);

        // Sort: newest (default), reward_high, reward_low, deadline
        if ("reward_high".equals(sortBy)) {
            wrapper.orderByDesc(Demand::getRewardAmount);
        } else if ("reward_low".equals(sortBy)) {
            wrapper.orderByAsc(Demand::getRewardAmount);
        } else if ("deadline".equals(sortBy)) {
            wrapper.orderByAsc(Demand::getDeadline);
        } else {
            wrapper.orderByDesc(Demand::getCreateTime);
        }

        Page<Demand> page = demandMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Demand> demands = page.getRecords();

        // Batch-load publishers to avoid N+1 queries
        List<Long> publisherIds = demands.stream().map(Demand::getPublisherId).distinct().toList();
        final Map<Long, User> userMap;
        if (!publisherIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(publisherIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
        } else {
            userMap = Map.of();
        }

        // Pre-compute joinedCount for team-type demands
        final Map<Long, Integer> teamCounts = new java.util.HashMap<>();
        for (Demand d : demands) {
            if ("team".equals(d.getType())) {
                teamCounts.put(d.getDemandId(), teamMemberService.countJoined(d.getDemandId()));
            }
        }

        Page<DemandResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        final Map<Long, User> finalMap = userMap;
        resultPage.setRecords(demands.stream()
                .map(d -> {
                    DemandResponse rsp = DemandResponse.from(d, finalMap.get(d.getPublisherId()));
                    if ("team".equals(d.getType())) {
                        rsp.setJoinedCount(teamCounts.getOrDefault(d.getDemandId(), 0));
                    }
                    return rsp;
                })
                .toList());
        return resultPage;
    }

    @Override
    public DemandResponse getById(Long demandId) {
        Demand demand = findDemandOrFail(demandId);
        User publisher = userMapper.selectById(demand.getPublisherId());
        User acceptor = demand.getAcceptorId() != null ? userMapper.selectById(demand.getAcceptorId()) : null;

        List<TeamMemberResponse> teamMembers = null;
        if ("team".equals(demand.getType())) {
            teamMembers = teamMemberService.getJoinedMembers(demandId);
        }

        return DemandResponse.from(demand, publisher, acceptor, teamMembers);
    }

    @Override
    @Transactional
    public void cancel(Long demandId, Long userId) {
        Demand demand = findDemandOrFail(demandId);
        if (!demand.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己的需求");
        }
        if (DemandStatus.COMPLETED.equals(demand.getStatus()) || DemandStatus.CANCELLED.equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该需求已结束，无法取消");
        }

        // Notify acceptor if there is one
        if (demand.getAcceptorId() != null) {
            notificationService.notifyDemandCancelled(
                    demand.getAcceptorId(), demand.getTitle(), demand.getDemandId());
        }
        // Notify all team members for team demands
        if ("team".equals(demand.getType())) {
            List<TeamMemberResponse> members = teamMemberService.getJoinedMembers(demandId);
            for (TeamMemberResponse m : members) {
                if (!m.getUserId().equals(userId)) {
                    notificationService.create(m.getUserId(),
                            NotificationType.CANCEL,
                            "队伍已解散", "队伍「" + demand.getTitle() + "」已被队长解散", demandId);
                }
            }
        }

        executeCancel(demand);
    }

    @Override
    @Transactional
    public void autoCancelExpired(Long demandId) {
        Demand demand = findDemandOrFail(demandId);
        if (DemandStatus.COMPLETED.equals(demand.getStatus()) || DemandStatus.CANCELLED.equals(demand.getStatus())) {
            return; // Already ended, nothing to do
        }

        // Notify publisher that their demand expired
        notificationService.create(demand.getPublisherId(),
                NotificationType.CANCEL,
                "需求已过期",
                "您的需求「" + demand.getTitle() + "」已超过截止时间，系统自动取消",
                demandId);

        // Notify acceptor if there is one
        if (demand.getAcceptorId() != null) {
            notificationService.notifyDemandCancelled(
                    demand.getAcceptorId(), demand.getTitle(), demand.getDemandId());
        }

        // Notify all team members for team demands (including publisher)
        if ("team".equals(demand.getType())) {
            List<TeamMemberResponse> members = teamMemberService.getJoinedMembers(demandId);
            for (TeamMemberResponse m : members) {
                notificationService.create(m.getUserId(),
                        NotificationType.CANCEL,
                        "队伍已解散",
                        "队伍「" + demand.getTitle() + "」因需求过期已自动解散",
                        demandId);
            }
        }

        executeCancel(demand);
    }

    /**
     * Execute the actual cancellation: unfreeze points and update status.
     * Callers are responsible for permission checks and notifications.
     */
    private void executeCancel(Demand demand) {
        // Unfreeze points if this was a point-reward demand
        if ("point".equals(demand.getRewardType()) && demand.getRewardAmount() > 0) {
            pointsService.unfreezeOnCancel(demand.getPublisherId(), demand.getRewardAmount(), demand.getDemandId());
        }
        demand.setStatus(DemandStatus.CANCELLED);
        demandMapper.updateById(demand);
    }

    @Override
    @Transactional
    public DemandResponse accept(Long demandId, Long acceptorId) {
        Demand demand = findDemandOrFail(demandId);
        if ("team".equals(demand.getType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "组队需求不使用接单，请申请加入队伍");
        }
        if (demand.getPublisherId().equals(acceptorId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能接自己的需求");
        }
        if (!DemandStatus.OPEN.equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该需求已被接取或已结束");
        }
        demand.setAcceptorId(acceptorId);
        demand.setStatus(DemandStatus.IN_PROGRESS);
        demandMapper.updateById(demand);

        User publisher = userMapper.selectById(demand.getPublisherId());
        User acceptor = userMapper.selectById(acceptorId);

        // Notify publisher
        notificationService.notifyDemandAccepted(
                publisher.getUserId(), demand.getTitle(), demand.getDemandId(), acceptor.getName());
        return DemandResponse.from(demand, publisher, acceptor);
    }

    @Override
    @Transactional
    public DemandResponse complete(Long demandId, Long userId) {
        Demand demand = findDemandOrFail(demandId);
        if (!demand.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有发布者可以确认完成");
        }
        if (!DemandStatus.IN_PROGRESS.equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能完成进行中的需求");
        }
        // Transfer points from publisher to acceptor
        if ("point".equals(demand.getRewardType()) && demand.getRewardAmount() > 0 && demand.getAcceptorId() != null) {
            pointsService.transferOnComplete(demand.getPublisherId(), demand.getAcceptorId(),
                    demand.getRewardAmount(), demandId);
        }
        demand.setStatus(DemandStatus.COMPLETED);
        demandMapper.updateById(demand);

        User publisher = userMapper.selectById(demand.getPublisherId());
        User acceptor = demand.getAcceptorId() != null ? userMapper.selectById(demand.getAcceptorId()) : null;

        // Notify publisher
        notificationService.notifyDemandCompleted(
                publisher.getUserId(), demand.getTitle(), demand.getDemandId());
        // Also notify acceptor that the task is complete and they can evaluate
        if (acceptor != null) {
            notificationService.notifyTaskCompleted(
                    acceptor.getUserId(), demand.getTitle(), demand.getDemandId());
        }
        return DemandResponse.from(demand, publisher, acceptor);
    }

    @Override
    public List<DemandResponse> myOrders(Long userId, String role) {
        LambdaQueryWrapper<Demand> wrapper = new LambdaQueryWrapper<>();
        if ("publisher".equals(role)) {
            wrapper.eq(Demand::getPublisherId, userId);
        } else if ("acceptor".equals(role)) {
            wrapper.eq(Demand::getAcceptorId, userId);
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的角色参数，需要 publisher 或 acceptor");
        }
        wrapper.orderByDesc(Demand::getCreateTime);
        List<Demand> demands = demandMapper.selectList(wrapper);

        // Batch-load all relevant users
        List<Long> userIds = demands.stream().flatMap(d -> {
            java.util.List<Long> ids = new java.util.ArrayList<>();
            ids.add(d.getPublisherId());
            if (d.getAcceptorId() != null) ids.add(d.getAcceptorId());
            return ids.stream();
        }).distinct().toList();

        final Map<Long, User> userMap;
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
        } else {
            userMap = Map.of();
        }

        return demands.stream()
                .map(d -> DemandResponse.from(d,
                        userMap.get(d.getPublisherId()),
                        userMap.get(d.getAcceptorId())))
                .toList();
    }

    @Override
    public List<DemandResponse> myTeamOrders(Long userId) {
        List<Long> demandIds = teamMemberService.getJoinedDemandIds(userId);
        if (demandIds.isEmpty()) return List.of();

        List<Demand> demands = demandMapper.selectBatchIds(demandIds);

        // Batch-load all relevant users
        java.util.Set<Long> userIds = new java.util.HashSet<>();
        for (Demand d : demands) {
            userIds.add(d.getPublisherId());
        }

        final Map<Long, User> userMap;
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(new java.util.ArrayList<>(userIds)).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
        } else {
            userMap = Map.of();
        }

        return demands.stream()
                .map(d -> DemandResponse.from(d, userMap.get(d.getPublisherId())))
                .toList();
    }

    @Override
    @Transactional
    public DemandResponse updateDemand(Long demandId, Long userId, UpdateDemandRequest request) {
        Demand demand = findDemandOrFail(demandId);

        // 1. Ownership check
        if (!demand.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己的需求");
        }

        // 2. Status check — only OPEN
        if (!DemandStatus.OPEN.equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能编辑已发布状态的需求");
        }

        // 3. Type match validation
        if (!demand.getType().equals(request.getType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "需求类型不可更改");
        }

        // 4. Validate deadline
        if (request.getDeadline() != null && request.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "截止时间不能是过去的时间");
        }

        // 5. Validate rewardAmount
        int newAmount = request.getRewardAmount() != null ? request.getRewardAmount() : 0;
        if (newAmount < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "悬赏金额不能为负数");
        }

        // 6. Validate type-specific attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            validateAttributes(demand.getType(), request.getAttributes());
        }

        // 7. Handle points adjustment for point-reward demands
        int oldAmount = demand.getRewardAmount() != null ? demand.getRewardAmount() : 0;
        if ("point".equals(demand.getRewardType()) && newAmount != oldAmount) {
            pointsService.adjustFrozenPoints(userId, oldAmount, newAmount, demandId);
        }

        // 8. Serialize attributes to JSON
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            try {
                demand.setAttributes(OBJECT_MAPPER.writeValueAsString(request.getAttributes()));
            } catch (IOException e) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "属性数据格式错误");
            }
        } else {
            demand.setAttributes(null);
        }

        // 9. Update all editable fields
        demand.setTitle(request.getTitle());
        demand.setDescription(request.getDescription());
        demand.setLocation(request.getLocation());
        demand.setDeadline(request.getDeadline());
        demand.setRewardAmount(newAmount);
        demand.setIsAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous() ? 1 : 0);
        demand.setImages(request.getImages());

        demandMapper.updateById(demand);

        User publisher = userMapper.selectById(userId);
        return DemandResponse.from(demand, publisher);
    }

    private void validateAttributes(String type, Map<String, Object> attrs) {
        if ("errand".equals(type)) {
            if (!(attrs.get("pickup_location") instanceof String s) || s.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "跑腿代取需要填写取件地点");
            }
        } else if ("lost_found".equals(type)) {
            if (!(attrs.get("lf_type") instanceof String s) || s.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "失物招领需要选择类型（寻物/招领）");
            }
            String lfType = (String) attrs.get("lf_type");
            if (!"LOST".equals(lfType) && !"FOUND".equals(lfType)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "失物招领类型必须为 LOST 或 FOUND");
            }
        } else if ("team".equals(type)) {
            Object sizeObj = attrs.get("team_size");
            if (!(sizeObj instanceof Number n) || n.intValue() < 2) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "组队人数至少为2人");
            }
            if (attrs.containsKey("team_type")) {
                Object typeObj = attrs.get("team_type");
                if (typeObj instanceof String s && !s.isBlank()
                    && !java.util.Set.of("course_project", "competition", "club", "other").contains(s)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "无效的队伍类型");
                }
            }
        }
    }

    private Demand findDemandOrFail(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        }
        return demand;
    }
}
