package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.dto.UpdateDemandRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

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

    /** Upload a demand image. Returns the server-relative URL. */
    String uploadImage(Long userId, MultipartFile file);

    /** Paginated list with optional type, keyword filters and sort order. */
    Page<DemandResponse> list(int pageNum, int pageSize, String type, String keyword, String sortBy);

    /** Paginated list with favorited status for the current user. */
    Page<DemandResponse> list(Long userId, int pageNum, int pageSize, String type, String keyword, String sortBy);

    /** Get a single demand with full publisher and acceptor info. */
    DemandResponse getById(Long demandId);

    /** Get a single demand with favorited status for the current user. */
    DemandResponse getById(Long demandId, Long userId);

    /** Cancel an OPEN or IN_PROGRESS demand. Only the publisher may cancel. */
    void cancel(Long demandId, Long userId);

    /** Auto-cancel an expired demand (called by scheduler, skips publisher permission check). */
    void autoCancelExpired(Long demandId);

    /** Admin cancels a demand (skips ownership check, for moderation). */
    void adminCancelDemand(Long demandId, Long adminId);

    /** Admin hard-deletes a demand. Unfreezes points, cleans up references, then deletes. */
    void adminDeleteDemand(Long demandId, Long adminId);

    /** Admin paginated demand list with optional type, keyword, and status filters. */
    Page<DemandResponse> adminListDemands(int pageNum, int pageSize, String type, String keyword, String status);

    /** Accept an OPEN demand. Cannot accept your own demand. */
    DemandResponse accept(Long demandId, Long acceptorId);

    /** Mark an IN_PROGRESS demand as COMPLETED. Only the publisher may complete. */
    DemandResponse complete(Long demandId, Long userId);

    /** List demands where the given user is publisher or acceptor. */
    List<DemandResponse> myOrders(Long userId, String role);

    /** List demands where the user is a joined team member. */
    List<DemandResponse> myTeamOrders(Long userId);

    /** Update an OPEN demand's editable fields. Only the publisher may edit. */
    DemandResponse updateDemand(Long demandId, Long userId, UpdateDemandRequest request);
}
