package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.dto.RegisterRequest;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.FavoriteMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FavoriteServiceTest {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private DemandService demandService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DemandMapper demandMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    private Long userId;
    private Long demandId;

    @BeforeEach
    void setUp() {
        favoriteMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Create a test user
        RegisterRequest regReq = new RegisterRequest();
        regReq.setStudentId("svctest");
        regReq.setPassword("pass123");
        regReq.setName("服务测试");
        userService.register(regReq);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "svctest"));
        userId = user.getUserId();

        // Publish a demand
        CreateDemandRequest demandReq = new CreateDemandRequest();
        demandReq.setType("errand");
        demandReq.setTitle("服务测试需求");
        demandReq.setDescription("测试描述");
        demandReq.setRewardType("point");
        demandReq.setRewardAmount(10);
        DemandResponse rsp = demandService.publish(userId, demandReq);
        demandId = rsp.getDemandId();
    }

    @Test
    @DisplayName("favorite should create a record")
    void favorite_shouldCreateRecord() {
        favoriteService.favorite(userId, demandId);

        Set<Long> ids = favoriteService.getFavoritedDemandIds(userId);
        assertEquals(1, ids.size());
        assertTrue(ids.contains(demandId));
        assertTrue(favoriteService.isFavorited(userId, demandId));
    }

    @Test
    @DisplayName("unfavorite should remove the record")
    void unfavorite_shouldRemoveRecord() {
        favoriteService.favorite(userId, demandId);
        favoriteService.unfavorite(userId, demandId);

        Set<Long> ids = favoriteService.getFavoritedDemandIds(userId);
        assertTrue(ids.isEmpty());
        assertFalse(favoriteService.isFavorited(userId, demandId));
    }

    @Test
    @DisplayName("duplicate favorite should throw BusinessException")
    void favorite_duplicate_shouldThrow() {
        favoriteService.favorite(userId, demandId);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.favorite(userId, demandId));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("favorite non-existent demand should throw NOT_FOUND")
    void favorite_nonExistentDemand_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.favorite(userId, 99999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("listFavorites should paginate correctly")
    void listFavorites_shouldPaginate() {
        // Publish 3 more demands and favorite all
        for (int i = 0; i < 3; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("errand");
            req.setTitle("需求" + i);
            req.setDescription("desc");
            req.setRewardType("point");
            DemandResponse rsp = demandService.publish(userId, req);
            favoriteService.favorite(userId, rsp.getDemandId());
        }
        // Also favorite the initial demand
        favoriteService.favorite(userId, demandId);

        // Page size 2, page 1 should have 2 records, 4 total
        Page<DemandResponse> page1 = favoriteService.listFavorites(userId, 1, 2);
        assertEquals(4, page1.getTotal());
        assertEquals(2, page1.getRecords().size());

        // Page 2 should have 2 records
        Page<DemandResponse> page2 = favoriteService.listFavorites(userId, 2, 2);
        assertEquals(2, page2.getRecords().size());
    }

    @Test
    @DisplayName("getFavoritedDemandIds should return correct set")
    void getFavoritedDemandIds_shouldReturnSet() {
        // Publish and favorite another demand
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("study");
        req.setTitle("第二个需求");
        req.setDescription("desc");
        req.setRewardType("point");
        DemandResponse rsp = demandService.publish(userId, req);
        favoriteService.favorite(userId, rsp.getDemandId());
        favoriteService.favorite(userId, demandId);

        Set<Long> ids = favoriteService.getFavoritedDemandIds(userId);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(demandId));
        assertTrue(ids.contains(rsp.getDemandId()));
    }

    @Test
    @DisplayName("unfavorite on non-favorited demand should not throw")
    void unfavorite_idempotent_shouldNotThrow() {
        assertDoesNotThrow(() -> favoriteService.unfavorite(userId, demandId));
        assertFalse(favoriteService.isFavorited(userId, demandId));
    }
}
