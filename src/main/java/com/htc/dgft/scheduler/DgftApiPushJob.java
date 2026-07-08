package com.htc.dgft.scheduler;

import com.htc.dgft.service.DgftApiPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quartz job that processes batches from the staging table and simulates
 * pushing them to the DGFT API. Updates statuses as per the third instruction.
 *
 * This scheduler:
 * 1. Fetches batches from dgft_orm_message_master with status 'CREATED'
 * 2. Simulates a mock API push
 * 3. Updates DGFT_ACK_STATUS to 'Validated' and STATUS to 'MSG_PUSH_SUCCESS' in message master
 * 4. Updates detail status to 'PENDING' and DGFT_ACK_STATUS to null
 * 5. Logs the mock JSON response to dgft_orm_msg_tx_status_log
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DgftApiPushJob implements Job {

    private final DgftApiPushService apiPushService;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting DGFT API Push Job execution");
            apiPushService.processBatchFromStaging();
            log.info("DGFT API Push Job completed successfully");
        } catch (Exception e) {
            log.error("Error executing DGFT API Push Job", e);
            throw new JobExecutionException("API push batch failed", e, false);
        }
    }
}