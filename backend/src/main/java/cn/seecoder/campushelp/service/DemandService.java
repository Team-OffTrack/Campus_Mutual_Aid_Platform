package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Demand publishing and discovery service.
 * <p>
 * Supports creating demands, browsing a paginated list with type/keyword filters,
 * fetching a single demand detail, and cancelling an open demand.
 */
public interface DemandService {

    /** Publish a new demand. Returns the created demand with publisher info. */
    DemandResponse publish(Long publisherId, CreateDemandRequest request);

    /** Paginated list with optional type, keyword filters and sort order. */
    Page<DemandResponse> list(int pageNum, int pageSize, String type, String keyword, String sortBy);

    /** Get a single demand by ID, including publisher display info. Throws if not found. */
    DemandResponse getById(Long demandId);

    /** Cancel an OPEN demand. Only the publisher may cancel. */
    void cancel(Long demandId, Long userId);
}
