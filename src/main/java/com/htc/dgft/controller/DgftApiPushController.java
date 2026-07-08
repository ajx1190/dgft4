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

/**
 * Controller for the DGFT API Push Service Scheduler (Third Instruction).
 * Provides endpoint to manually trigger the API push scheduler that processes
 * batches from staging tables and updates statuses as per the third instruction.
 *
 * Returns the simulated request/response JSON for each processed batch so the
 * caller can see exactly what was "sent to" and "received from" the (mock) DGFT API.
 *
 * This endpoint is separate from the existing QuartzTriggerController to avoid
 * conflicts with the first and second instruction schedulers.
 */
@RestController
@RequestMapping("/api/dgft-push")
public class DgftApiPushController {

    private final DgftApiPushService apiPushService;

    public DgftApiPushController(DgftApiPushService apiPushService) {
        this.apiPushService = apiPushService;
    }

    /**
     * Manual trigger endpoint for the DGFT API push scheduler.
     * Processes batches from staging table and simulates API push.
     *
     * Updates statuses according to third instruction:
     * - DGFT_IRM_MESSAGE_MASTER.DGFT_ACK_STATUS = "Validated"
     * - DGFT_IRM_MESSAGE_MASTER.STATUS = "MSG_PUSH_SUCCESS"
     * - DGFT_IRM_MESSAGE_DETAIL.STATUS = "PENDING"
     * - DGFT_IRM_MESSAGE_DETAIL.DGFT_ACK_STATUS = null
     *
     * @return List of request/response JSON for each processed batch
     */
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