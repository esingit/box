package com.box.login.dto;

import lombok.Data;

@Data
public class FitnessStatsDTO {
    private Integer monthlyCount;  // 本月运动次数
    private Integer streakDays;    // 连续运动天数
    private Integer totalCount;    // 累计运动次数
}
