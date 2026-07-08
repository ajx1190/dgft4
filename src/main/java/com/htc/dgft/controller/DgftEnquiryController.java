package com.htc.dgft.controller;

import com.htc.dgft.scheduler.DgftEnquiryJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dgft-push")
public class DgftEnquiryController {

    private final DgftEnquiryJob enquiryJob;

    public DgftEnquiryController(DgftEnquiryJob enquiryJob) {
        this.enquiryJob = enquiryJob;
    }

    @PostMapping("/trigger-enquiry")
    public ResponseEntity<Object> triggerEnquiry() {
        try {
            int updatedCount = enquiryJob.processEnquiry();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("ormMasterRecordsUpdated", updatedCount);

            if (updatedCount == 0) {
                response.put("message", "No MSG_PUSH_SUCCESS message masters found to inquire");
            } else {
                response.put("message", "Enquiry job completed. " + updatedCount + " ORM master records updated.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}