package com.box.login.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FitnessStatsMapper {
    @Select("SELECT COUNT(*) FROM fitness_record WHERE create_user = #{createUser} AND finish_time >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)")
    Integer getMonthlyCount(@Param("createUser") String createUser);

    @Select("SELECT COUNT(*) FROM fitness_record WHERE create_user = #{createUser}")
    Integer getTotalCount(@Param("createUser") String createUser);

    @Select("""
        WITH RECURSIVE days AS (
            SELECT DATE(finish_time) as date
            FROM fitness_record
            WHERE create_user = #{createUser}
            GROUP BY DATE(finish_time)
            ORDER BY date DESC
        ),
        streak AS (
            SELECT date, 1 as streak, date as start_date
            FROM days
            WHERE date = CURRENT_DATE
            UNION ALL
            SELECT d.date, s.streak + 1, s.start_date
            FROM days d
            INNER JOIN streak s ON d.date = DATE_SUB(s.date, INTERVAL 1 DAY)
        )
        SELECT IFNULL(MAX(streak), 0) as streak_days
        FROM streak
        """)
    Integer getStreakDays(@Param("createUser") String createUser);
}
