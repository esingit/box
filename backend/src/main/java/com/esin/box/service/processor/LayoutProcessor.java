package com.esin.box.service.processor;


import com.esin.box.dto.ocr.PageLayout;
import com.esin.box.dto.ocr.ProductItem;

import java.util.List;

/**
 * 布局处理器接口
 * 定义不同布局类型的处理策略
 */
public interface LayoutProcessor {

    /**
     * 处理页面布局，返回识别出的产品项列表
     *
     * @param layout 页面布局信息
     * @return 产品项列表
     */
    List<ProductItem> processLayout(PageLayout layout);
}