package com.esin.box.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公共元数据实体类
 * 用于存储通用的键值对数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("common_meta")
public class CommonMeta extends BaseEntity {
    @TableId
    private Long id;
    private String typeCode; // 类型编号
    private String typeName; // 类型名称
    private String key1; // 键1
    private String value1; // 值1
    private String key2; // 键2
    private String value2; // 值2
    private String key3; // 键3
    private String value3; // 值3
    private String key4; // 键4
    private String value4; // 值4
}
