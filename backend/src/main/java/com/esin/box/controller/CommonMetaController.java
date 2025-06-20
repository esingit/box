package com.esin.box.controller;

import com.esin.box.dto.ApiResponse;
import com.esin.box.dto.CommonMetaDTO;
import com.esin.box.entity.CommonMeta;
import com.esin.box.service.CommonMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

}
