package com.box.login.controller;

import com.box.login.dto.ApiResponse;
import com.box.login.entity.CommonMeta;
import com.box.login.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ApiResponse<List<CommonMeta>> listByType(String typeCode) {
        List<CommonMeta> list = commonMetaService.lambdaQuery()
                .eq(CommonMeta::getTypeCode, typeCode)
                .list();
        return ApiResponse.success(list);
    }

    @GetMapping("/key1-value1-by-type")
    public ApiResponse<List<Map<String, String>>> listKey1Value1ByType(String typeCode) {
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

    @GetMapping("/all-key-value-by-type")
    public ApiResponse<List<Map<String, Map<String, String>>>> listAllKeyValueByType(String typeCode) {
        List<CommonMeta> list = commonMetaService.lambdaQuery()
                .eq(CommonMeta::getTypeCode, typeCode)
                .list();
        List<Map<String, Map<String, String>>> result = list.stream()
                .map(meta -> {
                    Map<String, Map<String, String>> itemMap = new HashMap<>();
                    Map<String, String> keyValuePairs = new HashMap<>();
                    if (meta.getKey1() != null && !meta.getKey1().isEmpty()) {
                        keyValuePairs.put(meta.getKey1(), meta.getValue1());
                    }
                    if (meta.getKey2() != null && !meta.getKey2().isEmpty()) {
                        keyValuePairs.put(meta.getKey2(), meta.getValue2());
                    }
                    if (meta.getKey3() != null && !meta.getKey3().isEmpty()) {
                        keyValuePairs.put(meta.getKey3(), meta.getValue3());
                    }
                    if (meta.getKey4() != null && !meta.getKey4().isEmpty()) {
                        keyValuePairs.put(meta.getKey4(), meta.getValue4());
                    }
                    itemMap.put(meta.getTypeName(), keyValuePairs);
                    return itemMap;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/types")
    public ApiResponse<List<Map<String, String>>> listTypes() {
        List<Map<String, String>> types = commonMetaService.lambdaQuery()
                .select(CommonMeta::getTypeCode, CommonMeta::getTypeName)
                .groupBy(CommonMeta::getTypeCode, CommonMeta::getTypeName)
                .list()
                .stream()
                .map(meta -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("typeCode", meta.getTypeCode());
                    map.put("typeName", meta.getTypeName());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(types);
    }

    @GetMapping("/by-id/{id}")
    public ApiResponse<CommonMeta> getById(@PathVariable Long id) {
        CommonMeta meta = commonMetaService.getById(id);
        return ApiResponse.success(meta);
    }
}
