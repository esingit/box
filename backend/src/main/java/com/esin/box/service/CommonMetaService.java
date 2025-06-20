package com.esin.box.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.esin.box.dto.CommonMetaDTO;
import com.esin.box.entity.CommonMeta;

import java.util.List;

public interface CommonMetaService extends IService<CommonMeta> {
    List<CommonMeta> queryByFields(List<CommonMetaDTO> commonMetaDTOList);
}