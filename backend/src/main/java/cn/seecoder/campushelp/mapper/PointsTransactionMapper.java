package cn.seecoder.campushelp.mapper;

import cn.seecoder.campushelp.entity.PointsTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsTransactionMapper extends BaseMapper<PointsTransaction> {

    /** Sum of all positive point amounts (issued, not deductions). */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM points_transaction WHERE amount > 0")
    Long sumPositivePoints();

    /** Count distinct users who had points transactions since the given date. */
    @Select("SELECT COUNT(DISTINCT user_id) FROM points_transaction WHERE create_time >= #{since}")
    Long countDistinctUsersSince(@Param("since") java.time.LocalDateTime since);
}
