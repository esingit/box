// BatchAddResult.java
package com.esin.box.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// BatchAddResult.java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchAddResult {
    /**
     * 成功处理的记录数
     */
    private int successCount;

    /**
     * 失败记录数
     */
    private int failedCount;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 总记录数
     */
    private int totalCount;

    /**
     * 是否覆盖了现有记录
     */
    private boolean overwrote;

    /**
     * 是否复制了历史记录
     */
    private boolean copied;

    /**
     * 更新记录数
     */
    private int updateCount;

    /**
     * 新增记录数
     */
    private int addCount;

    /**
     * 操作结果消息
     */
    private String message;
}