package com.box.login.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FitnessStatsMapper {
    @Select("""
        SELECT COUNT(*) 
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser} 
        AND fr.finish_time >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'EXERCISE'
    """)
    Integer getMonthlyCount(@Param("createUser") String createUser);

    @Select("""
        SELECT COUNT(*) 
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser}
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'EXERCISE'
    """)
    Integer getTotalCount(@Param("createUser") String createUser);
    
    @Select("""
        SELECT COUNT(DISTINCT DATE(fr.finish_time))
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser} 
        AND YEARWEEK(fr.finish_time) = YEARWEEK(NOW())
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'EXERCISE'
    """)
    Integer getWeeklyCount(@Param("createUser") String createUser);

    @Select("""
        SELECT COALESCE(SUM(fr.count), 0)
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser}
        AND DATE(fr.finish_time) = CURRENT_DATE
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'INTAKE'
        AND cm.key1 = 'CARBOHYDRATE'
    """)
    Integer getCarbsIntake(@Param("createUser") String createUser);

    @Select("""
        SELECT COALESCE(SUM(fr.count), 0)
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser}
        AND DATE(fr.finish_time) = CURRENT_DATE
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'INTAKE'
        AND cm.key1 = 'PROTEIN'
    """)
    Integer getProteinIntake(@Param("createUser") String createUser);

    @Select("""
        SELECT DATEDIFF(CURRENT_DATE, MAX(DATE(fr.finish_time))) 
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser}
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'EXERCISE'
    """)
    Integer getLastWorkoutDays(@Param("createUser") String createUser);

    @Select("""
        SELECT DATE_FORMAT(DATE_ADD(MAX(DATE(fr.finish_time)), INTERVAL 3 DAY), '%m月%d日') 
        FROM fitness_record fr
        LEFT JOIN common_meta cm ON fr.type_id = cm.id
        WHERE fr.create_user = #{createUser}
        AND cm.type_code = 'FITNESS_TYPE'
        AND cm.key2 = 'EXERCISE'
    """)
    String getNextWorkoutDay(@Param("createUser") String createUser);

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
