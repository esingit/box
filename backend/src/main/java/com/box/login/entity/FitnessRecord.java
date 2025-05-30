package com.box.login.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fitness_record")
public class FitnessRecord extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String type;
    private Integer count;
    private LocalDateTime finishTime;
} 