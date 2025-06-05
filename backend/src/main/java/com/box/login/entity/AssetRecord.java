package com.box.login.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_record")
public class AssetRecord extends BaseEntity {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 资产类型ID
     */
    private Long assetTypeId;

    /**
     * 资产名称ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assetNameId;

    /**
     * 数量/金额
     */
    private BigDecimal amount;

    /**
     * 单位ID
     */
    private Long unitId;

    /**
     * 购入/登记时间
     */
    private LocalDateTime acquireTime;

    /**
     * 资产位置ID
     */
    private Long assetLocationId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 版本号
     */
    @Version
    private Integer version;

    private String type; // 收入/支出

    private LocalDateTime recordTime;

    @TableField(exist = false)
    private String assetName;
}