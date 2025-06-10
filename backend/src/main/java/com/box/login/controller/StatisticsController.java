package com.box.login.controller;

import com.box.login.service.AssetService;
import com.box.login.service.FitnessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    @Autowired
    private FitnessService fitnessService;

    @Autowired
    private AssetService assetService;

    @GetMapping("/fitness/statistics")
    public ResponseEntity<List<Map<String, Object>>> getFitnessStatistics() {
        return ResponseEntity.ok(fitnessService.getStatistics());
    }

    @GetMapping("/asset/statistics")
    public ResponseEntity<List<Map<String, Object>>> getAssetStatistics() {
        return ResponseEntity.ok(assetService.getStatistics());
    }
}
