package com.esin.box.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.esin.box.dto.CommonMetaDTO;
import com.esin.box.entity.CommonMeta;
import com.esin.box.mapper.CommonMetaMapper;
import com.esin.box.service.CommonMetaService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CommonMetaServiceImpl extends ServiceImpl<CommonMetaMapper, CommonMeta> implements CommonMetaService {

    @Override
    public List<CommonMeta> queryByFields(List<CommonMetaDTO> commonMetaDTOList) {
        if (CollectionUtils.isEmpty(commonMetaDTOList)) {
            throw new IllegalArgumentException("请求参数不能为空");
        }

        // 校验每一个 DTO 至少有一个字段非空
        boolean allInvalid = commonMetaDTOList.stream().allMatch(dto ->
                (dto.getId() == null) &&
                        !StringUtils.hasText(dto.getTypeCode()) &&
                        !StringUtils.hasText(dto.getKey1()) &&
                        !StringUtils.hasText(dto.getKey2()) &&
                        !StringUtils.hasText(dto.getKey3()) &&
                        !StringUtils.hasText(dto.getKey4())
        );
        if (allInvalid) {
            throw new IllegalArgumentException("每个请求对象至少要包含一个有效查询条件");
        }
        // 合并多个条件（or 查询）
        LambdaQueryWrapper<CommonMeta> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(queryWrapper -> {
            for (CommonMetaDTO dto : commonMetaDTOList) {
                queryWrapper.or(inner -> {
                    if (dto.getId() != null) {
                        inner.eq(CommonMeta::getId, dto.getId());
                    }
                    if (StringUtils.hasText(dto.getTypeCode())) {
                        inner.eq(CommonMeta::getTypeCode, dto.getTypeCode());
                    }
                    if (StringUtils.hasText(dto.getKey1())) {
                        inner.eq(CommonMeta::getKey1, dto.getKey1());
                    }
                    if (StringUtils.hasText(dto.getKey2())) {
                        inner.eq(CommonMeta::getKey2, dto.getKey2());
                    }
                    if (StringUtils.hasText(dto.getKey3())) {
                        inner.eq(CommonMeta::getKey3, dto.getKey3());
                    }
                    if (StringUtils.hasText(dto.getKey4())) {
                        inner.eq(CommonMeta::getKey4, dto.getKey4());
                    }
                });
            }

        });
        return this.list(wrapper);
    }
}