package com.denzel.mpesa.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

// ─── Inbound: what the client sends to initiate an STK push ──────────────────

/**
 * Mirrors the real Daraja STK Push (LipaNaMpesa Online) request body.
 * See: https://developer.safaricom.co.ke/APIs/MpesaExpressSimulate
 */
@Data
public class StkPushRequest {

    @NotBlank(message = "BusinessShortCode is required")
    private String businessShortCode;

    /**
     * In production this is a Base64-encoded timestamp password.
     * Here we accept any non-blank value — validation mirrors the real API.
     */
    @NotBlank(message = "Password is required")
    private String password;

    /** Format: YYYYMMDDHHmmss */
    @NotBlank(message = "Timestamp is required")
    @Pattern(regexp = "\\d{14}", message = "Timestamp must be 14 digits: YYYYMMDDHHmmss")
    private String timestamp;

    @NotBlank(message = "TransactionType is required")
    private String transactionType;  // e.g. "CustomerPayBillOnline"

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least KES 1")
    private BigDecimal amount;

    /** MSISDN in format 2547XXXXXXXX */
    @NotBlank(message = "PartyA (phone number) is required")
    @Pattern(regexp = "^254[17]\\d{8}$", message = "PartyA must be in format 2547XXXXXXXX or 2541XXXXXXXX")
    private String partyA;

    @NotBlank(message = "PartyB (short code) is required")
    private String partyB;

    @NotBlank(message = "PhoneNumber is required")
    @Pattern(regexp = "^254[17]\\d{8}$", message = "PhoneNumber must be in format 2547XXXXXXXX or 2541XXXXXXXX")
    private String phoneNumber;

    /** URL where Safaricom would POST the callback — stored but not actually called in simulator */
    @NotBlank(message = "CallBackURL is required")
    private String callBackURL;

    @NotBlank(message = "AccountReference is required")
    private String accountReference;

    @NotBlank(message = "TransactionDesc is required")
    private String transactionDesc;
}
