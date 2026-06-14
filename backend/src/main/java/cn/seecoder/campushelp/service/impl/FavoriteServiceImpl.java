package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.Favorite;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.FavoriteMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.FavoriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, DemandMapper demandMapper,
                                UserMapper userMapper) {
        this.favoriteMapper = favoriteMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void favorite(Long userId, Long demandId) {
        Demand demand = demandMapper.selectById(demandId);
        if (demand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getDemandId, demandId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "已收藏");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setDemandId(demandId);
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional
    public void unfavorite(Long userId, Long demandId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getDemandId, demandId);
        favoriteMapper.delete(wrapper);
    }

    @Override
    public Page<DemandResponse> listFavorites(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreateTime);
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Favorite> favorites = page.getRecords();

        // Batch-load demands
        List<Long> demandIds = favorites.stream().map(Favorite::getDemandId).toList();
        Map<Long, Demand> demandMap;
        if (!demandIds.isEmpty()) {
            demandMap = demandMapper.selectBatchIds(demandIds).stream()
                    .collect(Collectors.toMap(Demand::getDemandId, d -> d));
        } else {
            demandMap = Map.of();
        }

        // Batch-load publishers
        List<Long> publisherIds = demandMap.values().stream()
                .map(Demand::getPublisherId).distinct().toList();
        final Map<Long, User> userMap;
        if (!publisherIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(publisherIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
        } else {
            userMap = Map.of();
        }

        Page<DemandResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(favorites.stream()
                .map(f -> DemandResponse.from(demandMap.get(f.getDemandId()),
                        userMap.get(demandMap.get(f.getDemandId()).getPublisherId())))
                .toList());
        return resultPage;
    }

    @Override
    public Set<Long> getFavoritedDemandIds(Long userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        return favoriteMapper.selectList(wrapper).stream()
                .map(Favorite::getDemandId)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isFavorited(Long userId, Long demandId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getDemandId, demandId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
