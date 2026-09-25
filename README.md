# M-Pesa STK Push Simulator

A Spring Boot REST API that simulates the [Safaricom Daraja STK Push (Lipa Na M-Pesa Online)](https://developer.safaricom.co.ke/) API. Built as a portfolio project to demonstrate real-world fintech API integration patterns used across Kenyan banking and e-commerce systems.

---

## Why This Project

Every Kenyan fintech, bank, SACCO, and e-commerce platform integrates M-Pesa payments. The Daraja STK Push API has a specific asynchronous pattern that developers must understand to build reliable payment systems — the payment outcome doesn't arrive synchronously, it arrives via a callback after the user interacts with a prompt on their phone.

This simulator replicates that full flow locally, including:
- Immediate 202 Accepted response on initiation (same as real Daraja)
- Configurable delay before callback resolution (default 5 seconds)
- Realistic success and failure outcomes with real Safaricom result codes
- Full transaction lifecycle tracking (PENDING → SUCCESS / FAILED)

---

## Tech Stack

| Technology | Role |
|---|---|
| Java 21 (LTS) | Core language |
| Spring Boot 3.2 | Application framework |
| Spring Data JPA | Database access layer |
| H2 (in-memory) | Lightweight database for dev/demo |
| Spring Security | Basic Auth protecting all endpoints |
| Spring Validation | Request field validation |
| Lombok | Boilerplate reduction |
| Maven | Build tool |
| Awaitility | Async assertion in integration tests |

---

## Prerequisites

- Java 21+ — [Download Temurin 21 LTS](https://adoptium.net)
- Maven 3.8+
- [Postman](https://www.postman.com/downloads/) (for API testing)

Verify your setup:
```bash
java -version   # should show openjdk 21.x.x
mvn -version    # should show Apache Maven 3.x.x
```

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/DenzelKioko/mpesa-stk-push-simulator.git
cd mpesa-stk-push-simulator
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

You should see:
```
Started MpesaSimulatorApplication in ~4s
Tomcat started on port(s): 8080
```

The app is now running at `http://localhost:8080`.

### 3. Verify It's Up

Open your browser and navigate to:
```
http://localhost:8080/api/mpesa/health
```

Expected response:
```json
{
    "status": "UP",
    "service": "M-Pesa STK Push Simulator",
    "version": "1.0.0"
}
```

---

## API Reference

### Authentication

All endpoints except `/api/mpesa/health` and `/h2-console` require **HTTP Basic Auth**:

| Field | Value |
|---|---|
| Username | `testConsumerKey` |
| Password | `testConsumerSecret` |

These mirror the Safaricom Daraja consumer key/secret pattern.

---

### Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/mpesa/health` | None | Health check |
| `POST` | `/api/mpesa/stkpush/v1/processrequest` | Required | Initiate STK Push |
| `GET` | `/api/mpesa/transactions/{checkoutRequestId}` | Required | Poll transaction status |
| `GET` | `/api/mpesa/transactions` | Required | List all transactions |
| `GET` | `/api/mpesa/transactions/phone/{phoneNumber}` | Required | Filter by phone number |

---

### Initiate STK Push

**POST** `/api/mpesa/stkpush/v1/processrequest`

Request body:
```json
{
  "businessShortCode": "174379",
  "password": "anybase64stringhere",
  "timestamp": "20240101120000",
  "transactionType": "CustomerPayBillOnline",
  "amount": 500,
  "partyA": "254712345678",
  "partyB": "174379",
  "phoneNumber": "254712345678",
  "callBackURL": "https://example.com/callback",
  "accountReference": "ORDER-001",
  "transactionDesc": "Payment for order 001"
}
```

Response — **202 Accepted** (returned immediately):
```json
{
    "merchantRequestID": "A1B2C3D4E5F6G7H8",
    "checkoutRequestID": "ws_CO_20240101120000_123456789",
    "responseCode": "0",
    "responseDescription": "Success. Request accepted for processing",
    "customerMessage": "Success. Request accepted for processing"
}
```

> Copy the `checkoutRequestID` — use it to poll for the transaction outcome.

---

### Poll Transaction Status

**GET** `/api/mpesa/transactions/{checkoutRequestID}`

Response immediately after initiation:
```json
{
    "status": "PENDING"
}
```

Response after ~5 seconds — **SUCCESS**:
```json
{
    "status": "SUCCESS",
    "resultCode": 0,
    "resultDesc": "The service request is processed successfully.",
    "mpesaReceiptNumber": "RG64829340ABC",
    "amount": 500.00,
    "phoneNumber": "254712345678"
}
```

Response after ~5 seconds — **FAILED**:
```json
{
    "status": "FAILED",
    "resultCode": 1032,
    "resultDesc": "Request cancelled by user."
}
```

---

### Result Codes

| Code | Description | Real-world meaning |
|---|---|---|
| `0` | Processed successfully | User paid — happy path |
| `1` | Balance insufficient | Not enough funds in M-Pesa wallet |
| `17` | Risk limit exceeded | Safaricom fraud threshold hit |
| `1032` | Cancelled by user | User dismissed the STK prompt |
| `2001` | Invalid initiator info | Auth mismatch on Safaricom's side |

---

## The Async Pattern — How It Works

```
Client                    Simulator                   Async Thread
  |                           |                             |
  |-- POST /stkpush --------->|                             |
  |                           |-- save PENDING to DB        |
  |                           |-- trigger simulateCallback()-->|
  |<-- 202 Accepted ----------|                             |
  |                           |                    (sleeping 5s)
  |-- GET /transactions/{id}->|                             |
  |<-- { status: PENDING } ---|                             |
  |                           |                    (wakes up)
  |                           |<-- updates DB to SUCCESS/FAILED
  |-- GET /transactions/{id}->|                             |
  |<-- { status: SUCCESS } ---|                             |
```

This mirrors the real Daraja flow exactly — the STK Push endpoint returns immediately and the payment outcome arrives asynchronously, requiring clients to either poll for status or handle a callback.

---

## Testing with Postman

A ready-made Postman collection is included in this repository.

### Import the Collection

1. Open Postman
2. Click **Import**
3. Select `M-PESA STK Push.postman_collection.json` from the repo root
4. Set **Authorization** to **Basic Auth** on each request:
   - Username: `testConsumerKey`
   - Password: `testConsumerSecret`

### Recommended Test Sequence

| Step | Request | Expected Result |
|---|---|---|
| 1 | Health Check | `{"status":"UP"}` |
| 2 | Initiate STK Push | 202 Accepted + `checkoutRequestID` |
| 3 | Poll Status (immediately) | `{"status":"PENDING"}` |
| 4 | Poll Status (after 5s) | `{"status":"SUCCESS"}` or `{"status":"FAILED"}` |
| 5 | List All Transactions | Array of all initiated transactions |
| 6 | Invalid phone format | 400 Bad Request with field error |
| 7 | Wrong credentials | 401 Unauthorized |
| 8 | Unknown checkout ID | 404 Not Found |

---

## Configuration

All configurable values are in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | Port the app runs on |
| `mpesa.simulator.callback-delay-ms` | `5000` | Milliseconds before callback fires |
| `mpesa.simulator.success-rate` | `0.8` | 0.0 = always fail, 1.0 = always succeed |
| `mpesa.auth.consumer-key` | `testConsumerKey` | Basic Auth username |
| `mpesa.auth.consumer-secret` | `testConsumerSecret` | Basic Auth password |

### Testing Scenarios

**Always succeed (happy path demo):**
```properties
mpesa.simulator.success-rate=1.0
mpesa.simulator.callback-delay-ms=2000
```

**Always fail (error handling demo):**
```properties
mpesa.simulator.success-rate=0.0
```

---

## H2 Database Console

While the app is running, view live transaction data at:

```
http://localhost:8080/h2-console
```

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:mpesadb` |
| Username | `sa` |
| Password | *(leave blank)* |

Run `SELECT * FROM TRANSACTIONS;` to inspect all transaction records in real time.

> The H2 database is in-memory — all data resets when the app restarts. For persistent storage, see the roadmap below.

---

## Running the Tests

```bash
mvn test
```

The integration test suite spins up a full Spring context and runs 6 end-to-end tests:

| Test | What it verifies |
|---|---|
| Health check returns UP without auth | Public endpoint accessible |
| STK Push returns 401 without credentials | Security layer working |
| Valid STK Push returns 202 with checkoutRequestId | Happy path initiation |
| Transaction resolves to SUCCESS after async callback | Async flow working end-to-end |
| Invalid phone number returns 400 with field error | Bean validation working |
| Unknown checkout ID returns 404 | Error handling working |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/denzel/mpesa/
│   │   ├── MpesaSimulatorApplication.java   # Entry point
│   │   ├── controller/
│   │   │   └── StkPushController.java       # REST endpoints
│   │   ├── service/
│   │   │   └── PaymentService.java          # Business logic + async simulation
│   │   ├── model/
│   │   │   ├── Transaction.java             # JPA entity
│   │   │   ├── TransactionStatus.java       # PENDING/SUCCESS/FAILED enum
│   │   │   └── TransactionRepository.java   # Spring Data JPA repository
│   │   ├── dto/
│   │   │   ├── StkPushRequest.java          # Inbound request DTO + validation
│   │   │   └── StkPushResponse.java         # Response DTOs
│   │   ├── security/
│   │   │   └── SecurityConfig.java          # Basic Auth configuration
│   │   └── config/
│   │       ├── AsyncConfig.java             # Thread pool for async callbacks
│   │       └── GlobalExceptionHandler.java  # JSON error responses
│   └── resources/
│       └── application.properties           # All configuration
└── test/
    └── java/com/denzel/mpesa/
        └── StkPushIntegrationTest.java      # Integration test suite
```

---

## Roadmap

- [ ] Replace H2 with PostgreSQL for persistent storage
- [ ] Add Swagger/OpenAPI documentation at `/swagger-ui.html`
- [ ] Implement real OAuth 2.0 token endpoint (mirrors actual Daraja auth)
- [ ] Add actual callback delivery via RestTemplate to `callBackURL`
- [ ] Add Docker support for one-command deployment
- [ ] Add rate limiting per consumer key

---

## Background

Built by **Denzel Kioko Wambua** — BBIT graduate, Strathmore University (2026).

Inspired by real-world banking API integration work during an internship at National Bank of Kenya, where I built file-processing applications calling banking API endpoints. This project applies those same integration patterns to the M-Pesa context that is central to Kenyan fintech development.

---

## Contact

- **Email:** denzelkioko123@gmail.com
- **LinkedIn:** [linkedin.com/in/denzel-wambua-0478b8203](https://linkedin.com/in/denzel-wambua-0478b8203)
- **GitHub:** [github.com/DenzelKioko](https://github.com/DenzelKioko)
