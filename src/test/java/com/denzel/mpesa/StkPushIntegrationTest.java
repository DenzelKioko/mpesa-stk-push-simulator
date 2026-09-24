package com.denzel.mpesa;

import com.denzel.mpesa.model.TransactionRepository;
import com.denzel.mpesa.model.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "mpesa.simulator.callback-delay-ms=500",   // Fast for tests
        "mpesa.simulator.success-rate=1.0"          // Always succeed in tests
    })
@AutoConfigureMockMvc
class StkPushIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    private Map<String, Object> validRequestBody() {
        return Map.of(
                "businessShortCode", "174379",
                "password", "MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTVkZDM4NDhlN2Q0MjlhMzUzNTc0NTYyMjU1N2QzMjE4MGEyMjViMzgwNjk0YzE2ZDJmNDYwMGVlZDk1ZTkxMzIxNzQ5OTM=",
                "timestamp", "20240101120000",
                "transactionType", "CustomerPayBillOnline",
                "amount", 100,
                "partyA", "254712345678",
                "partyB", "174379",
                "phoneNumber", "254712345678",
                "callBackURL", "https://example.com/callback",
                "accountReference", "TEST-REF-001",
                "transactionDesc", "Payment for order 001"
        );
    }

    @Test
    @DisplayName("Health check returns UP without auth")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/mpesa/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("STK Push returns 401 without credentials")
    void stkPushRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/mpesa/stkpush/v1/processrequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestBody())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid STK Push returns 202 Accepted with checkoutRequestId")
    void validStkPushReturns202() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/mpesa/stkpush/v1/processrequest")
                .with(httpBasic("testConsumerKey", "testConsumerSecret"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestBody())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.responseCode").value("0"))
                .andExpect(jsonPath("$.checkoutRequestID").isNotEmpty())
                .andExpect(jsonPath("$.merchantRequestID").isNotEmpty())
                .andReturn();

        // Verify it was persisted as PENDING
        String body = result.getResponse().getContentAsString();
        String checkoutId = objectMapper.readTree(body).get("checkoutRequestID").asText();
        var txn = transactionRepository.findByCheckoutRequestId(checkoutId);
        assertTrue(txn.isPresent());
        assertEquals(TransactionStatus.PENDING, txn.get().getStatus());
    }

    @Test
    @DisplayName("Transaction resolves to SUCCESS after async callback")
    void transactionResolvesAfterCallback() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/mpesa/stkpush/v1/processrequest")
                .with(httpBasic("testConsumerKey", "testConsumerSecret"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequestBody())))
                .andExpect(status().isAccepted())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String checkoutId = objectMapper.readTree(body).get("checkoutRequestID").asText();

        // Await async resolution (up to 5s, checking every 200ms)
        await().atMost(5, TimeUnit.SECONDS).pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> {
                    var txn = transactionRepository.findByCheckoutRequestId(checkoutId);
                    return txn.isPresent() && txn.get().getStatus() == TransactionStatus.SUCCESS;
                });

        var txn = transactionRepository.findByCheckoutRequestId(checkoutId).orElseThrow();
        assertEquals(0, txn.getResultCode());
        assertNotNull(txn.getMpesaReceiptNumber());
    }

    @Test
    @DisplayName("Invalid phone number returns 400 with field error")
    void invalidPhoneReturns400() throws Exception {
        var req = new java.util.HashMap<>(validRequestBody());
        req.put("phoneNumber", "07123456");  // Not in 254XXXXXXXXX format

        mockMvc.perform(post("/api/mpesa/stkpush/v1/processrequest")
                .with(httpBasic("testConsumerKey", "testConsumerSecret"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());
    }

    @Test
    @DisplayName("Status polling returns 404 for unknown checkoutRequestId")
    void unknownCheckoutIdReturns404() throws Exception {
        mockMvc.perform(get("/api/mpesa/transactions/UNKNOWN-ID-123")
                .with(httpBasic("testConsumerKey", "testConsumerSecret")))
                .andExpect(status().isNotFound());
    }
}
