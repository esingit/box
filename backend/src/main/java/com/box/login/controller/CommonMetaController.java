package com.box.login.controller;

import com.box.login.dto.ApiResponse;
import com.box.login.entity.CommonMeta;
import com.box.login.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<List<CommonMeta>> getMetaListByType(@RequestParam String typeCode) {
        validateTypeCode(typeCode);
        List<CommonMeta> list = queryByTypeCode(typeCode);
        return ApiResponse.success(list);
    }

    @GetMapping("/key1-value1-by-type")
    public ApiResponse<List<Map<String, String>>> getKey1Value1PairsByType(@RequestParam String typeCode) {
        validateTypeCode(typeCode);
        List<CommonMeta> list = queryByTypeCode(typeCode);
        List<Map<String, String>> result = list.stream()
                .map(this::convertToKey1Value1Pair)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/all-key-value-by-type")
    public ApiResponse<List<Map<String, Map<String, String>>>> getAllKeyValuePairsByType(@RequestParam String typeCode) {
        validateTypeCode(typeCode);
        List<CommonMeta> list = queryByTypeCode(typeCode);
        List<Map<String, Map<String, String>>> result = list.stream()
                .map(this::convertToAllKeyValuePairs)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @GetMapping("/types")
    public ApiResponse<List<Map<String, String>>> getAllTypes() {
        List<Map<String, String>> types = commonMetaService.lambdaQuery()
                .select(CommonMeta::getTypeCode, CommonMeta::getTypeName)
                .groupBy(CommonMeta::getTypeCode, CommonMeta::getTypeName)
                .list()
                .stream()
                .map(this::convertToTypeInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(types);
    }

    @GetMapping("/by-id/{id}")
    public ApiResponse<CommonMeta> getMetaById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid id parameter");
        }
        CommonMeta meta = commonMetaService.getById(id);
        if (meta == null) {
            throw new IllegalArgumentException("Meta not found with id: " + id);
        }
        return ApiResponse.success(meta);
    }

    // 私有辅助方法

    private void validateTypeCode(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            throw new IllegalArgumentException("Type code cannot be empty");
        }
    }

    private List<CommonMeta> queryByTypeCode(String typeCode) {
        return commonMetaService.lambdaQuery()
                .eq(CommonMeta::getTypeCode, typeCode)
                .list();
    }

    private Map<String, String> convertToKey1Value1Pair(CommonMeta meta) {
        Map<String, String> map = new HashMap<>();
        map.put("key1", meta.getKey1());
        map.put("value1", meta.getValue1());
        return map;
    }

    private Map<String, Map<String, String>> convertToAllKeyValuePairs(CommonMeta meta) {
        Map<String, Map<String, String>> itemMap = new HashMap<>();
        Map<String, String> keyValuePairs = new HashMap<>();
        
        addIfNotEmpty(keyValuePairs, meta.getKey1(), meta.getValue1());
        addIfNotEmpty(keyValuePairs, meta.getKey2(), meta.getValue2());
        addIfNotEmpty(keyValuePairs, meta.getKey3(), meta.getValue3());
        addIfNotEmpty(keyValuePairs, meta.getKey4(), meta.getValue4());
        
        itemMap.put(meta.getTypeName(), keyValuePairs);
        return itemMap;
    }

    private Map<String, String> convertToTypeInfo(CommonMeta meta) {
        Map<String, String> map = new HashMap<>();
        map.put("typeCode", meta.getTypeCode());
        map.put("typeName", meta.getTypeName());
        return map;
    }

    private void addIfNotEmpty(Map<String, String> map, String key, String value) {
        if (StringUtils.hasText(key)) {
            map.put(key, value);
        }
    }
}
