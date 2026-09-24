package com.denzel.mpesa.service;

import com.denzel.mpesa.dto.StkPushRequest;
import com.denzel.mpesa.dto.StkPushResponse;
import com.denzel.mpesa.model.Transaction;
import com.denzel.mpesa.model.TransactionRepository;
import com.denzel.mpesa.model.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Core business logic for the M-Pesa STK Push simulator.
 *
 * Key design decisions:
 *  - initiateSTKPush() is synchronous and returns immediately (HTTP 202), as the
 *    real Daraja API does — the actual payment outcome arrives via async callback.
 *  - simulateCallback() is @Async, running on a separate thread pool, mimicking
 *    the real-world delay between STK prompt and user action.
 *  - Success rate is configurable in application.properties, enabling both happy-
 *    path and failure-scenario testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;

    @Value("${mpesa.simulator.callback-delay-ms:5000}")
    private long callbackDelayMs;

    @Value("${mpesa.simulator.success-rate:0.8}")
    private double successRate;

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter MPESA_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ─── Initiate ─────────────────────────────────────────────────────────────

    /**
     * Validates and persists an STK Push request, then schedules the async
     * callback simulation. Returns immediately with 202 Accepted — exactly as
     * the real Safaricom Daraja endpoint does.
     */
    @Transactional
    public StkPushResponse initiateSTKPush(StkPushRequest request) {
        String checkoutRequestId = generateCheckoutRequestId();
        String merchantRequestId = generateMerchantRequestId();

        Transaction transaction = new Transaction();
        transaction.setCheckoutRequestId(checkoutRequestId);
        transaction.setMerchantRequestId(merchantRequestId);
        transaction.setPhoneNumber(request.getPhoneNumber());
        transaction.setAmount(request.getAmount());
        transaction.setBusinessShortCode(request.getBusinessShortCode());
        transaction.setAccountReference(request.getAccountReference());
        transaction.setTransactionDesc(request.getTransactionDesc());
        transaction.setStatus(TransactionStatus.PENDING);

        transactionRepository.save(transaction);
        log.info("STK Push initiated: checkoutRequestId={} phone={} amount={}",
                checkoutRequestId, request.getPhoneNumber(), request.getAmount());

        // Schedule the simulated callback — this does NOT block the HTTP response
        simulateCallback(checkoutRequestId);

        return StkPushResponse.builder()
                .merchantRequestID(merchantRequestId)
                .checkoutRequestID(checkoutRequestId)
                .responseCode("0")
                .responseDescription("Success. Request accepted for processing")
                .customerMessage("Success. Request accepted for processing")
                .build();
    }

    // ─── Async Simulation ─────────────────────────────────────────────────────

    /**
     * Simulates the delay between STK push and the user's response on their phone.
     * Runs on the "simulatorExecutor" thread pool defined in AsyncConfig.
     *
     * This is the key pattern that mirrors real Daraja behaviour — your client
     * code must handle asynchronous resolution, not assume synchronous payment.
     */
    @Async("simulatorExecutor")
    public void simulateCallback(String checkoutRequestId) {
        try {
            log.debug("Callback simulation started for {}, waiting {}ms",
                    checkoutRequestId, callbackDelayMs);
            Thread.sleep(callbackDelayMs);

            boolean success = RANDOM.nextDouble() < successRate;

            Transaction transaction = transactionRepository
                    .findByCheckoutRequestId(checkoutRequestId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction not found: " + checkoutRequestId));

            if (success) {
                resolveSuccess(transaction);
            } else {
                resolveFailure(transaction);
            }

            transactionRepository.save(transaction);
            log.info("Callback resolved: checkoutRequestId={} status={}",
                    checkoutRequestId, transaction.getStatus());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Callback simulation interrupted for {}", checkoutRequestId);
        }
    }

    // ─── Status Query ─────────────────────────────────────────────────────────

    public Optional<Transaction> getTransactionStatus(String checkoutRequestId) {
        return transactionRepository.findByCheckoutRequestId(checkoutRequestId);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactionsByPhone(String phoneNumber) {
        return transactionRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void resolveSuccess(Transaction t) {
        t.setStatus(TransactionStatus.SUCCESS);
        t.setResultCode(0);
        t.setResultDesc("The service request is processed successfully.");
        // Generate a realistic-looking M-Pesa receipt number
        t.setMpesaReceiptNumber(generateReceiptNumber());
    }

    private void resolveFailure(Transaction t) {
        // Pick a realistic failure reason, weighted toward common ones
        int[] codes = {1032, 1, 17, 2001};
        String[] descs = {
            "Request cancelled by user.",
            "The balance is insufficient for the transaction.",
            "Risk limit exceeded.",
            "The initiator information is invalid."
        };
        int idx = RANDOM.nextInt(codes.length);
        t.setStatus(TransactionStatus.FAILED);
        t.setResultCode(codes[idx]);
        t.setResultDesc(descs[idx]);
    }

    /**
     * Generates a CheckoutRequestID in Safaricom's real format:
     * ws_CO_<timestamp>_<random>
     */
    private String generateCheckoutRequestId() {
        return "ws_CO_" + LocalDateTime.now().format(MPESA_DATE_FMT)
                + "_" + (100000000 + RANDOM.nextInt(900000000));
    }

    private String generateMerchantRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * M-Pesa receipt numbers follow the pattern: [A-Z]{2}[0-9]{8}[A-Z0-9]{5}
     * e.g. RGQ64829340
     */
    private String generateReceiptNumber() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        // Two capital letters
        sb.append((char)('A' + RANDOM.nextInt(26)));
        sb.append((char)('A' + RANDOM.nextInt(26)));
        // Eight digits
        for (int i = 0; i < 8; i++) sb.append(RANDOM.nextInt(10));
        // Three alphanumerics
        for (int i = 0; i < 3; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }
}
