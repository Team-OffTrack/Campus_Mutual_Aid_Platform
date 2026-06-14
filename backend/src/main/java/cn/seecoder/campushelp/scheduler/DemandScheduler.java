package cn.seecoder.campushelp.scheduler;

import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.enums.DemandStatus;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.service.DemandService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for demand lifecycle management.
 * <p>
 * Scans for expired OPEN/IN_PROGRESS demands and auto-cancels them,
 * unfreezing points and notifying affected users.
 */
@Component
public class DemandScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemandScheduler.class);

    private final DemandMapper demandMapper;
    private final DemandService demandService;

    public DemandScheduler(DemandMapper demandMapper, DemandService demandService) {
        this.demandMapper = demandMapper;
        this.demandService = demandService;
    }

    /**
     * Every 60 seconds, scan for demands past their deadline and auto-cancel them.
     * Each demand is processed independently — failure on one does not block others.
     */
    @Scheduled(fixedRate = 60_000)
    public void cancelExpiredDemands() {
        LambdaQueryWrapper<Demand> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Demand::getStatus, DemandStatus.OPEN, DemandStatus.IN_PROGRESS)
                .lt(Demand::getDeadline, LocalDateTime.now());

        List<Demand> expired = demandMapper.selectList(wrapper);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Found {} expired demand(s), auto-cancelling...", expired.size());

        int success = 0;
        int fail = 0;
        for (Demand d : expired) {
            try {
                demandService.autoCancelExpired(d.getDemandId());
                success++;
            } catch (Exception e) {
                fail++;
                log.error("Auto-cancel failed for demand {}: {}", d.getDemandId(), e.getMessage(), e);
            }
        }

        log.info("Auto-cancel complete: {} succeeded, {} failed (out of {})", success, fail, expired.size());
    }
}
