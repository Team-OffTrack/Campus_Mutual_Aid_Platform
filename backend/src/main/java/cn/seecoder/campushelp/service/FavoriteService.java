package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.DemandResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Set;

public interface FavoriteService {

    /** Add a demand to user's favorites. Throws if demand not found or already favorited. */
    void favorite(Long userId, Long demandId);

    /** Remove a demand from user's favorites. No-op if not favorited. */
    void unfavorite(Long userId, Long demandId);

    /** Paginated list of the user's favorited demands. All statuses included. */
    Page<DemandResponse> listFavorites(Long userId, int pageNum, int pageSize);

    /** Returns the set of demand IDs the given user has favorited (for batch checking). */
    Set<Long> getFavoritedDemandIds(Long userId);

    /** Check if a specific demand is favorited by the user. */
    boolean isFavorited(Long userId, Long demandId);
}
