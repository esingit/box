package com.box.login.dto;

import com.box.login.entity.FitnessRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class FitnessRecordDTO extends FitnessRecord {
    private String typeName; // 健身类型名称
    private String typeValue; // 健身类型值
    private String unitName; // 单位名称
    private String unitValue; // 单位值
}