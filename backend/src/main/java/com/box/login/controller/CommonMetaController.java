package com.box.login.controller;

import com.box.login.dto.ApiResponse;
import com.box.login.entity.CommonMeta;
import com.box.login.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/common-meta")
public class CommonMetaController {
    @Autowired
    private CommonMetaService commonMetaService;

    @GetMapping("/by-type")
    public ApiResponse listByType(String typeCode) {
        List<CommonMeta> list = commonMetaService.lambdaQuery()
                .eq(CommonMeta::getTypeCode, typeCode)
                .list();
        return ApiResponse.success(list);
    }

    @GetMapping("/key1-value1-by-type")
    public ApiResponse listKey1Value1ByType(String typeCode) {
        List<CommonMeta> list = commonMetaService.lambdaQuery()
                .eq(CommonMeta::getTypeCode, typeCode)
                .list();
        List<Map<String, String>> result = list.stream()
                .map(meta -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", meta.getKey1());
                    map.put("value1", meta.getValue1());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }
}
