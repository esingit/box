package com.box.login.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.box.login.entity.CommonMeta;
import com.box.login.mapper.CommonMetaMapper;
import com.box.login.service.CommonMetaService;
import org.springframework.stereotype.Service;

@Service
public class CommonMetaServiceImpl extends ServiceImpl<CommonMetaMapper, CommonMeta> implements CommonMetaService {
}
