package com.htc.dgft.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record DgftApiPushBatchResponse(
        String messageMasterId,
        String uniqueTxId,
        String requestJson,
        String responseJson
) {
}
