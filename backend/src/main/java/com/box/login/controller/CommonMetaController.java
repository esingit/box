package com.box.login.controller;

import com.box.login.dto.ApiResponse;
import com.box.login.dto.CommonMetaDTO;
import com.box.login.entity.CommonMeta;
import com.box.login.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/common-meta")
public class CommonMetaController {

    @Autowired
    private CommonMetaService commonMetaService;

    @PostMapping("/query")
    public ApiResponse<List<CommonMeta>> queryCommonMeta(@RequestBody List<CommonMetaDTO> request) {
        List<CommonMeta> list = commonMetaService.queryByFields(request);
        return ApiResponse.success(list);
    }
}
