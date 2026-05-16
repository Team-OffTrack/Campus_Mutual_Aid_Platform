package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Demand publishing, discovery, and order flow service.
 * <p>
 * Supports creating demands, browsing with filters, accepting, completing,
 * cancelling, and listing personal order history.
 */
public interface DemandService {

    /** Publish a new demand. */
    DemandResponse publish(Long publisherId, CreateDemandRequest request);

    /** Paginated list with optional type, keyword filters and sort order. */
    Page<DemandResponse> list(int pageNum, int pageSize, String type, String keyword, String sortBy);

    /** Get a single demand with full publisher and acceptor info. */
    DemandResponse getById(Long demandId);

    /** Cancel an OPEN or IN_PROGRESS demand. Only the publisher may cancel. */
    void cancel(Long demandId, Long userId);

    /** Accept an OPEN demand. Cannot accept your own demand. */
    DemandResponse accept(Long demandId, Long acceptorId);

    /** Mark an IN_PROGRESS demand as COMPLETED. Only the publisher may complete. */
    DemandResponse complete(Long demandId, Long userId);

    /** List demands where the given user is publisher or acceptor. */
    List<DemandResponse> myOrders(Long userId, String role);
}
