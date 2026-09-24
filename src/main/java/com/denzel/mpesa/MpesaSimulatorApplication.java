package com.denzel.mpesa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * M-Pesa STK Push Simulator
 *
 * A Spring Boot application that simulates the Safaricom Daraja STK Push API,
 * demonstrating async request handling, JPA persistence, and Basic Auth security.
 *
 * Built by Denzel Kioko Wambua
 * Inspired by real-world banking API integration work at National Bank of Kenya.
 */
@SpringBootApplication
public class MpesaSimulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MpesaSimulatorApplication.class, args);
    }
}
