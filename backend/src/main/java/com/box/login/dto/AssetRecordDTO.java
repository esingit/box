package com.box.login.dto;

import com.box.login.entity.AssetRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资产记录数据传输对象
 */
@Data
public class AssetRecordDTO {
    
    /**
     * 资产ID
     */
    private Long id;
    
    /**
     * 资产名称ID
     */
    private Long assetNameId;
    
    /**
     * 资产名称
     */
    private String assetName;
    
    /**
     * 资产数量
     */
    private BigDecimal amount;
    
    /**
     * 资产类型
     */
    private String type;
    
    /**
     * 记录时间
     */
    private LocalDateTime recordTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 创建用户
     */
    private String createUser;
    
    /**
     * 备注
     */
    private String remark;
}
