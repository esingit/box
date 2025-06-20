package com.esin.box.dto;

import com.esin.box.entity.FitnessRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FitnessRecordDTO extends FitnessRecord {
    private String typeName; // 健身类型名称
    private String typeValue; // 健身类型值
    private String unitName; // 单位名称
    private String unitValue; // 单位值
}