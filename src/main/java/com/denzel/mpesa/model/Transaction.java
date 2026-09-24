package com.denzel.mpesa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single M-Pesa STK Push transaction, from initiation through
 * callback resolution. Maps to the real Daraja API response fields.
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mirrors the real CheckoutRequestID from Safaricom (e.g. ws_CO_…).
     * Used by the client to poll for transaction status.
     */
    @Column(nullable = false, unique = true)
    private String checkoutRequestId;

    /** Mirrors the MerchantRequestID from Safaricom. */
    @Column(nullable = false)
    private String merchantRequestId;

    /** The phone number (MSISDN) that receives the STK push — format: 2547XXXXXXXX */
    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Business short code the payment is made to. */
    @Column(nullable = false)
    private String businessShortCode;

    /** Free-text description sent in the STK push prompt. */
    private String accountReference;
    private String transactionDesc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /** Mirrors Daraja's ResultCode: 0 = success, 1 = insufficient funds, etc. */
    private Integer resultCode;
    private String resultDesc;

    /** Safaricom M-Pesa receipt number (only populated on success). */
    private String mpesaReceiptNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = TransactionStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
