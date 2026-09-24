package com.denzel.mpesa.controller;

import com.denzel.mpesa.dto.StkPushRequest;
import com.denzel.mpesa.dto.StkPushResponse;
import com.denzel.mpesa.model.Transaction;
import com.denzel.mpesa.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * M-Pesa STK Push simulation endpoints.
 *
 * Real Daraja endpoint: POST /mpesa/stkpush/v1/processrequest
 * This simulator mirrors that structure under /api/mpesa/...
 *
 * Authentication: HTTP Basic Auth with consumer key:secret,
 * matching the Daraja OAuth flow (simplified here to Basic Auth).
 */
@RestController
@RequestMapping("/api/mpesa")
@RequiredArgsConstructor
@Slf4j
public class StkPushController {

    private final PaymentService paymentService;

    /**
     * Initiate an STK Push.
     * Returns 202 Accepted immediately — outcome arrives asynchronously.
     *
     * POST /api/mpesa/stkpush/v1/processrequest
     */
    @PostMapping("/stkpush/v1/processrequest")
    public ResponseEntity<StkPushResponse> initiateStkPush(
            @Valid @RequestBody StkPushRequest request) {

        log.info("Received STK Push request for phone={} amount={}",
                request.getPhoneNumber(), request.getAmount());

        StkPushResponse response = paymentService.initiateSTKPush(request);

        // 202 Accepted — request is queued, result comes via callback
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Poll for transaction status by CheckoutRequestID.
     * Clients call this to check outcome while waiting for the callback.
     *
     * GET /api/mpesa/transactions/{checkoutRequestId}
     */
    @GetMapping("/transactions/{checkoutRequestId}")
    public ResponseEntity<?> getTransactionStatus(
            @PathVariable String checkoutRequestId) {

        return paymentService.getTransactionStatus(checkoutRequestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all transactions (useful for the dashboard / Postman collection).
     * GET /api/mpesa/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(paymentService.getAllTransactions());
    }

    /**
     * List transactions for a specific phone number.
     * GET /api/mpesa/transactions/phone/{phoneNumber}
     */
    @GetMapping("/transactions/phone/{phoneNumber}")
    public ResponseEntity<List<Transaction>> getByPhone(
            @PathVariable String phoneNumber) {
        return ResponseEntity.ok(paymentService.getTransactionsByPhone(phoneNumber));
    }

    /**
     * Health check — useful for demonstrating the app is running in interviews.
     * GET /api/mpesa/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "M-Pesa STK Push Simulator",
                "version", "1.0.0"
        ));
    }
}
