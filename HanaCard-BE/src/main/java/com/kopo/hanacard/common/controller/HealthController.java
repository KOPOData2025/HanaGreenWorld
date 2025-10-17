package com.kopo.hanacard.common.controller;

import com.kopo.hanacard.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "hanacard-server");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        
        return ApiResponse.success("서버가 정상적으로 동작 중입니다.", health);
    }
}













