package com.box.login.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 资产名称实体类
 * 用于存储资产的名称和描述信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_name")
public class AssetName extends BaseEntity {
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id; // 资产名称主键ID

    @NotBlank(message = "资产名称不能为空")
    @Size(max = 255, message = "资产名称长度不能超过255个字符")
    private String name; // 资产名称

    @Size(max = 500, message = "资产名称描述长度不能超过500个字符")
    private String description; // 资产名称描述

    private String remark; // 备注
}
