package com.denzel.mpesa.dto;

import com.denzel.mpesa.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ─── STK Push initiation response (mirrors Safaricom's 202) ──────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StkPushResponse {
    /** "0" on success, non-zero on error — mirrors Daraja convention */
    private String merchantRequestID;
    private String checkoutRequestID;
    private String responseCode;           // "0" = accepted
    private String responseDescription;   // "Success. Request accepted for processing"
    private String customerMessage;        // Shown to the merchant
}

// ─── Transaction status response (polling endpoint) ──────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TransactionStatusResponse {
    private String checkoutRequestID;
    private TransactionStatus status;
    private Integer resultCode;
    private String resultDesc;
    private String mpesaReceiptNumber;
    private BigDecimal amount;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// ─── Simulated Daraja callback payload (what Safaricom POSTs to CallBackURL) ─

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CallbackBody {
    private String merchantRequestID;
    private String checkoutRequestID;
    private int resultCode;
    private String resultDesc;

    // Only present when resultCode == 0
    private CallbackMetadata callbackMetadata;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CallbackMetadata {
    private String mpesaReceiptNumber;
    private BigDecimal amount;
    private String transactionDate;
    private String phoneNumber;
}
