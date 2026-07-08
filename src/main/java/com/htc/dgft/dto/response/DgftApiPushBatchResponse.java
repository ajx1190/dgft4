package com.htc.dgft.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response DTO returned by the DGFT API Push endpoint.
 * Carries the simulated API request and response JSON for a batch
 * so the caller can see exactly what was "sent to" and "received from"
 * the (mock) DGFT API.
 */
public record DgftApiPushBatchResponse(
        String messageMasterId,
        String uniqueTxId,
        String requestJson,
        String responseJson
) {
}
