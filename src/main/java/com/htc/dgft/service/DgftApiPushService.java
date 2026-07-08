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


@Slf4j
@Service
@RequiredArgsConstructor
public class DgftApiPushService {

    private final DgftOrmMessageMasterRepository messageMasterRepo;
    private final DgftOrmMessageDetailRepository detailRepo;
    private final DgftOrmMsgTxStatusLogRepository logRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<DgftApiPushBatchResponse> processBatchFromStaging() {
        List<DgftOrmMessageMaster> masters = messageMasterRepo.findTop20ByStatus("MSG_PUSH_NEW");

        if (masters.isEmpty()) {
            log.info("No batches with MSG_PUSH_NEW status found to process");
            return List.of();
        }

        List<DgftApiPushBatchResponse> results = new ArrayList<>();

        for (DgftOrmMessageMaster master : masters) {
            List<DgftOrmMessageDetail> details = detailRepo.findByDgftOrmMessageMasterId(master.getId());
            String requestJson = buildRequestJson(master, details);
            master.setRequestJsonObj(requestJson);

         
            String responseJson = simulateApiResponse(master, details.size());

        
            master.setDgftAckStatus("Validated");
            master.setStatus("MSG_PUSH_SUCCESS");
            master.setResponseJsonObj(responseJson);
            master = messageMasterRepo.save(master);

            details.forEach(detail -> {
                detail.setStatus("PENDING");
                detail.setDgftAckStatus(null); 
            });
            detailRepo.saveAll(details);


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

    private record ApiRequestPayload(
            String uniqueTxId,
            int recordCount,
            List<DgftOrmMessageDetail> records
    ) {
    }

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