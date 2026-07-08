package com.htc.dgft.service;

import com.htc.dgft.entity.DgftOrmMessageDetail;
import com.htc.dgft.entity.DgftOrmMessageMaster;
import com.htc.dgft.entity.DgftOrmMsgTxStatusLog;
import com.htc.dgft.repository.DgftOrmMessageDetailRepository;
import com.htc.dgft.repository.DgftOrmMessageMasterRepository;
import com.htc.dgft.repository.DgftOrmMsgTxStatusLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class handling API push operations for DGFT
 *
 * This service:
 * 1. Processes batches from staging table
 * 2. Simulates API push with mock responses
 * 3. Updates entity statuses correctly
 * 4. Logs transaction status
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DgftApiPushService {

    private final DgftOrmMessageMasterRepository messageMasterRepo;
    private final DgftOrmMessageDetailRepository detailRepo;
    private final DgftOrmMsgTxStatusLogRepository logRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processBatchFromStaging() {
        // Step 1: Fetch 20 records with status 'CREATED'
//        List<DgftOrmMessageMaster> batch = messageMasterRepo.findTop20ByStatus("CREATED");
        List<DgftOrmMessageMaster> batch = messageMasterRepo.findTop20ByStatus("MSG_PUSH_NEW");

        if (batch.isEmpty()) {
//            log.info("No batches with CREATED status found to process");
            log.info("No batches with MSG_PUSH_NEW status found to process");
            return;
        }

        // Step 2: Create/update message master entry
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 15).toUpperCase();
        DgftOrmMessageMaster master = batch.get(0); // Use existing master record

        try {
            // Convert batch to JSON for request payload
            master.setRequestJsonObj(objectMapper.writeValueAsString(batch));
        } catch (Exception e) {
            log.error("Failed to serialize batch to JSON", e);
            master.setRequestJsonObj("{}");
        }

        master = messageMasterRepo.save(master);

        // Step 3: Get existing details
        List<DgftOrmMessageDetail> details = detailRepo.findByDgftOrmMessageMasterId(master.getId());

        // Step 4: Simulate API response
        String mockResponse = simulateApiResponse(batch, transactionId);

        // Step 5: Update master with response (third instruction updates)
        master.setDgftAckStatus("Validated");
        master.setStatus("MSG_PUSH_SUCCESS");
        master.setResponseJsonObj(mockResponse);
        master = messageMasterRepo.save(master);

        // Step 6: Update details (third instruction updates)
        details.forEach(detail -> {
            detail.setStatus("PENDING");
            // DGFT_ACK_STATUS remains null as per specification
        });
        detailRepo.saveAll(details);

        // Step 7: Log transaction status
        DgftOrmMsgTxStatusLog logEntry = new DgftOrmMsgTxStatusLog();
        logEntry.setDgftOrmMessageMaster(master);
        logEntry.setDgftTxStatusJsonObj(mockResponse);
        logRepo.save(logEntry);

        log.info("Successfully processed batch with {} records", batch.size());
    }

    /**
     * Generates a mock API response
     * @param batch of records
     * @param transactionId the transaction ID
     * @return JSON string
     */
    private String simulateApiResponse(List<DgftOrmMessageMaster> batch, String transactionId) {
        StringBuilder response = new StringBuilder();
        response.append("{\n");
        response.append("  \"status\": \"SUCCESS\",\n");
        response.append("  \"batchId\": \"").append(transactionId).append("\",\n");
        response.append("  \"processed\": true,\n");
        response.append("  \"recordCount\": ").append(batch.size()).append("\n");
        response.append("}");
        return response.toString();
    }
}