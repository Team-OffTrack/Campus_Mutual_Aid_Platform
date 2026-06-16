package cn.seecoder.campushelp.mapper;

import cn.seecoder.campushelp.entity.DailyCheckin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface DailyCheckinMapper extends BaseMapper<DailyCheckin> {

    /** Count distinct users who checked in since the given date. */
    @Select("SELECT COUNT(DISTINCT user_id) FROM daily_checkin WHERE checkin_date >= #{since}")
    Long countDistinctUsersSince(@Param("since") LocalDate since);
}
