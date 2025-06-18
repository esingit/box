package com.box.login.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.box.login.dto.CommonMetaDTO;
import com.box.login.entity.CommonMeta;

import java.util.List;

public interface CommonMetaService extends IService<CommonMeta> {
    List<CommonMeta> queryByFields(List<CommonMetaDTO> commonMetaDTOList);
}