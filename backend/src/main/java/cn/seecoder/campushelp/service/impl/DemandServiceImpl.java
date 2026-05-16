package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.DemandService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DemandServiceImpl implements DemandService {

    private final DemandMapper demandMapper;
    private final UserMapper userMapper;

    public DemandServiceImpl(DemandMapper demandMapper, UserMapper userMapper) {
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
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
        demand.setStatus("OPEN");
        demandMapper.insert(demand);

        User publisher = userMapper.selectById(publisherId);
        return DemandResponse.from(demand, publisher);
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
        wrapper.in(Demand::getStatus, "OPEN", "IN_PROGRESS");

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

        Page<DemandResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        final Map<Long, User> finalMap = userMap;
        resultPage.setRecords(demands.stream()
                .map(d -> DemandResponse.from(d, finalMap.get(d.getPublisherId())))
                .toList());
        return resultPage;
    }

    @Override
    public DemandResponse getById(Long demandId) {
        Demand demand = findDemandOrFail(demandId);
        User publisher = userMapper.selectById(demand.getPublisherId());
        return DemandResponse.from(demand, publisher);
    }

    @Override
    @Transactional
    public void cancel(Long demandId, Long userId) {
        Demand demand = findDemandOrFail(demandId);
        if (!demand.getPublisherId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消自己的需求");
        }
        if (!"OPEN".equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能取消进行中的需求");
        }
        demand.setStatus("CANCELLED");
        demandMapper.updateById(demand);
    }

    private Demand findDemandOrFail(Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        }
        return demand;
    }
}
