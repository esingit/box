package com.box.login.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fitness_record")
public class FitnessRecord extends BaseEntity {
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id; // 主键ID
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long typeId; // 健身类型ID
    
    private BigDecimal count; // 数量
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId; // 单位ID
    
    private LocalDateTime finishTime; // 健身完成时间

    private String remark; // 备注
}