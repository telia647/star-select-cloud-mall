package com.demo.mall.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeResponseWithComputedSuccessField() throws Exception {
        String json = """
                {"code":0,"message":"success","data":null,"traceId":"trace-1","success":true}
                """;

        Result<?> result = objectMapper.readValue(json, Result.class);

        assertTrue(result.isSuccess());
        assertEquals("trace-1", result.getTraceId());
    }
}
