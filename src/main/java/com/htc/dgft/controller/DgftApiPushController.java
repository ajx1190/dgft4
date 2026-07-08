package com.htc.dgft.controller;

import com.htc.dgft.dto.response.DgftApiPushBatchResponse;
import com.htc.dgft.service.DgftApiPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dgft-push")
public class DgftApiPushController {

    private final DgftApiPushService apiPushService;

    public DgftApiPushController(DgftApiPushService apiPushService) {
        this.apiPushService = apiPushService;
    }
    
    @PostMapping("/trigger-api-push")
    public ResponseEntity<Object> triggerApiPush() {
        try {
            List<DgftApiPushBatchResponse> results = apiPushService.processBatchFromStaging();

            if (results.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "No batches with MSG_PUSH_NEW status found to process");
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}