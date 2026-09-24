package com.denzel.mpesa.model;

/**
 * Lifecycle states of an STK Push transaction.
 *
 *   PENDING   → STK push sent to phone, awaiting user action
 *   SUCCESS   → User entered PIN and payment processed
 *   FAILED    → User cancelled, timeout, or insufficient funds
 *   CANCELLED → User explicitly dismissed the STK prompt
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}
