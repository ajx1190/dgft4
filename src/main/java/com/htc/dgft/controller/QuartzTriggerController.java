package com.htc.dgft.controller;

import lombok.RequiredArgsConstructor;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class QuartzTriggerController {

    private final Scheduler scheduler;

    @PostMapping("/trigger/push")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerPushJob() {
        try {
            scheduler.triggerJob(new JobKey("dgftOrmPushJob", "dgftGroup"));
            return ResponseEntity.ok("Quartz DgftOrmPushJob triggered successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error triggering job: " + e.getMessage());
        }
    }
}
