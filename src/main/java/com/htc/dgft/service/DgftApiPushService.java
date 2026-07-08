package com.htc.dgft.service;

import com.htc.dgft.dto.response.DgftApiPushBatchResponse;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Service class handling API push operations for DGFT (Instruction 3).
 *
 * Pulls batches from the staging table (dgft_orm_message_master with status
 * 'MSG_PUSH_NEW' - created by the earlier instruction), simulates a DGFT API
 * push, and updates the staging statuses as per the third instruction:
 *
 *   dgft_orm_message_master : DGFT_ACK_STATUS = "Validated", STATUS = "MSG_PUSH_SUCCESS"
 *   dgft_orm_message_detail  : STATUS = "PENDING", DGFT_ACK_STATUS = null
 *
 * The method returns the generated request/response JSON for each batch so the
 * caller (REST endpoint) can display exactly what was "sent to" / "received from"
 * the (mock) DGFT API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DgftApiPushService {

    private final DgftOrmMessageMasterRepository messageMasterRepo;
    private final DgftOrmMessageDetailRepository detailRepo;
    private final DgftOrmMsgTxStatusLogRepository logRepo;
    private final ObjectMapper objectMapper;

    /**
     * Process up to 20 batches (message masters) that are awaiting a DGFT push.
     *
     * @return list of per-batch request/response JSON that was persisted to the DB
     */
    @Transactional
    public List<DgftApiPushBatchResponse> processBatchFromStaging() {
        // Step 1: Fetch pending batches (staging records created by instruction 2)
        List<DgftOrmMessageMaster> masters = messageMasterRepo.findTop20ByStatus("MSG_PUSH_NEW");

        if (masters.isEmpty()) {
            log.info("No batches with MSG_PUSH_NEW status found to process");
            return List.of();
        }

        List<DgftApiPushBatchResponse> results = new ArrayList<>();

        for (DgftOrmMessageMaster master : masters) {
            // Step 2: Build the simulated API "request" payload from this batch's details
            List<DgftOrmMessageDetail> details = detailRepo.findByDgftOrmMessageMasterId(master.getId());
            String requestJson = buildRequestJson(master, details);
            master.setRequestJsonObj(requestJson);

            // Step 3: Simulate the DGFT API response
            String responseJson = simulateApiResponse(master, details.size());

            // Step 4: Apply third-instruction status updates to the master
            master.setDgftAckStatus("Validated");
            master.setStatus("MSG_PUSH_SUCCESS");
            master.setResponseJsonObj(responseJson);
            master = messageMasterRepo.save(master);

            // Step 5: Apply third-instruction status updates to the details
            details.forEach(detail -> {
                detail.setStatus("PENDING");
                detail.setDgftAckStatus(null); // remains null as per specification
            });
            detailRepo.saveAll(details);

            // Step 6: Log the simulated transaction status
            DgftOrmMsgTxStatusLog logEntry = new DgftOrmMsgTxStatusLog();
            logEntry.setDgftOrmMessageMaster(master);
            logEntry.setDgftTxStatusJsonObj(responseJson);
            logRepo.save(logEntry);

            results.add(new DgftApiPushBatchResponse(
                    master.getId(),
                    master.getUniqueTxId(),
                    requestJson,
                    responseJson
            ));
        }

        log.info("Successfully processed {} batches", masters.size());
        return results;
    }

    /**
     * Builds the JSON request body that would be posted to the DGFT API for a batch.
     * Mirrors the structure stored in REQUEST_JSON_OBJ.
     */
    private String buildRequestJson(DgftOrmMessageMaster master, List<DgftOrmMessageDetail> details) {
        try {
            ApiRequestPayload payload = new ApiRequestPayload(
                    master.getUniqueTxId(),
                    details.size(),
                    details
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize request payload to JSON", e);
            return "{}";
        }
    }

    /**
     * Generates a mock DGFT API response JSON for a batch.
     */
    private String simulateApiResponse(DgftOrmMessageMaster master, int recordCount) {
        try {
            ApiResponsePayload response = new ApiResponsePayload(
                    "SUCCESS",
                    "Validated",
                    "MSG_PUSH_SUCCESS",
                    master.getUniqueTxId(),
                    recordCount,
                    "IRM records received and validated by DGFT"
            );
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize response payload to JSON", e);
            return "{\"status\":\"SUCCESS\"}";
        }
    }

    /** Mock request body posted to the (simulated) DGFT API. */
    private record ApiRequestPayload(
            String uniqueTxId,
            int recordCount,
            List<DgftOrmMessageDetail> records
    ) {
    }

    /** Mock response body returned by the (simulated) DGFT API. */
    private record ApiResponsePayload(
            String status,
            String dgftAckStatus,
            String messageMasterStatus,
            String uniqueTxId,
            int batchSize,
            String acknowledgement
    ) {
    }
}