package com.box.login.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("common_meta")
public class CommonMeta extends BaseEntity {
    @TableId
    private Long id;
    private String typeCode; // 类型编号
    private String typeName; // 类型名称
    private String key1;
    private String value1;
    private String key2;
    private String value2;
    private String key3;
    private String value3;
    private String key4;
    private String value4;
}
