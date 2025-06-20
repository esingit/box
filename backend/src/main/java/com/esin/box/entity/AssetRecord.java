package com.esin.box.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 资产名称ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assetNameId;

    /**
     * 资产类型ID（关联common_meta表）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assetTypeId;
    
    /**
     * 数量/金额
     */
    private BigDecimal amount;

    /**
     * 货币单位ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unitId;

    /**
     * 资产位置ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assetLocationId;

    /**
     * 购入/登记时间
     */
    private LocalDateTime acquireTime;

    /**
     * 备注
     */
    private String remark;
}