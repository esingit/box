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
    public ApiResponse<CommonMeta> getMetaById(@PathVariable("id") String idStr) {
        try {
            long id = Long.parseLong(idStr);

            if (id <= 0) {
                return ApiResponse.error("参数错误：ID 必须为正整数");
            }

            CommonMeta meta = commonMetaService.getById(id);
            if (meta == null) {
                return ApiResponse.error("未找到 ID 为 " + id + " 的元数据");
            }

            return ApiResponse.success(meta);

        } catch (NumberFormatException e) {
            return ApiResponse.error("参数错误：ID 必须是数字");
        } catch (Exception e) {
            return ApiResponse.error("获取元数据失败：" + e.getMessage());
        }
    }


}
