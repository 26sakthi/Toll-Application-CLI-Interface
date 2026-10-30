# National Highway Toll Management System (FASTag)
### Enterprise In-Memory Electronic Toll Collection (ETC) System

---

## 1. Project Overview

The **Toll Management System (FASTag)** is a high-performance, domain-driven console application modeled after the National Highways Authority of India (**NHAI**) and Ministry of Road Transport & Highways (**MoRTH**) electronic toll collection standards.

It implements complete in-memory thread-safe state management (`ConcurrentHashMap`, `CopyOnWriteArrayList`), automated optical vehicle classification (AVC) mismatch detection, 24-hour return trip discounts, monthly passes, balance lifecycle rules, 7-day dispute settlements, and in-memory execution of advanced SQL analytical queries.

---

## 2. System Architecture & 5-Module Division

The system is decomposed into 5 autonomous domain modules:

```
src/
??? com/toll/
    ??? Main.java                               # Application Entry Point
    ??? app/
    ?   ??? TollManagementApp.java              # Interactive Console CLI (ANSI Themed)
    ??? context/
    ?   ??? TollSystemContext.java              # Dependency Container
    ?   ??? TollSystemBootstrap.java            # Seed Data & Initial State Factory
    ??? exception/                              # Domain-specific Unchecked Exceptions
    ?   ??? TollManagementException.java
    ?   ??? UnauthorizedException.java
    ?   ??? PlazaNotReadyException.java
    ?   ??? LaneOperationException.java
    ?   ??? AccountException.java
    ?   ??? TollProcessingException.java
    ?   ??? DisputeException.java
    ?   ??? PassException.java
    ??? model/
    ?   ??? admin/                              # Administrative Authority Model
    ?   ?   ??? Admin.java
    ?   ??? enums/                              # Domain Enumerations
    ?   ?   ??? VehicleType.java
    ?   ?   ??? TripType.java
    ?   ?   ??? LaneType.java
    ?   ?   ??? LaneStatus.java
    ?   ?   ??? PlazaStatus.java
    ?   ?   ??? AccountStatus.java
    ?   ?   ??? TransactionStatus.java
    ?   ?   ??? PaymentType.java
    ?   ?   ??? RechargeMethod.java
    ?   ?   ??? RechargeStatus.java
    ?   ?   ??? DisputeStatus.java
    ?   ?   ??? PassStatus.java
    ?   ??? plaza/                              # Module 1: Plazas, Lanes & Effective Rates
    ?   ?   ??? TollPlaza.java
    ?   ?   ??? Lane.java (Abstract)
    ?   ?   ??? FASTagLane.java
    ?   ?   ??? CashLane.java
    ?   ?   ??? EmergencyLane.java
    ?   ?   ??? TollRate.java
    ?   ??? vehicle/                            # Module 2: Vehicle Inheritance Hierarchy
    ?   ?   ??? Vehicle.java (Abstract)
    ?   ?   ??? Car.java
    ?   ?   ??? Van.java
    ?   ?   ??? Bus.java
    ?   ?   ??? Truck.java
    ?   ?   ??? MultiAxleVehicle.java
    ?   ?   ??? EmergencyVehicle.java
    ?   ??? account/                            # Module 2: FASTag Wallet Account
    ?   ?   ??? FASTagAccount.java
    ?   ??? pass/                               # Module 4: Monthly Passes
    ?   ?   ??? MonthlyPass.java
    ?   ??? transaction/                        # Module 3: Crossing Audit Log
    ?   ?   ??? TollTransaction.java
    ?   ??? billing/                            # Module 5: Billing, Alerts & SQL DTOs
    ?   ?   ??? PaymentContext.java
    ?   ?   ??? PaymentResult.java
    ?   ?   ??? RechargeRecord.java
    ?   ?   ??? LowBalanceAlert.java
    ?   ?   ??? VehicleTypeRevenueReport.java
    ?   ?   ??? PlazaAuditReport.java
    ?   ?   ??? MultiPlazaAccountReport.java
    ?   ??? dispute/                            # Module 5: Customer Disputes
    ?       ??? Dispute.java
    ??? service/
    ?   ??? plaza/                              # Module 1: Plaza Services
    ?   ?   ??? IPlazaService.java
    ?   ?   ??? PlazaServiceImpl.java
    ?   ??? account/                            # Module 2: Vehicle & Account Services
    ?   ?   ??? IAccountService.java
    ?   ?   ??? AccountServiceImpl.java
    ?   ??? tollengine/                         # Module 3: Edge Processing Engine
    ?   ?   ??? CrossingRequest.java
    ?   ?   ??? CrossingResponse.java
    ?   ?   ??? ITollProcessingService.java
    ?   ?   ??? TollProcessingServiceImpl.java
    ?   ??? pricing/                            # Module 4: Pricing & Discount Engine
    ?   ?   ??? TollCalculationResult.java
    ?   ?   ??? IPricingService.java
    ?   ?   ??? PricingServiceImpl.java
    ?   ??? billing/                            # Module 5: Payments, Disputes & Analytics
    ?       ??? payment/
    ?       ?   ??? PaymentMethod.java (Interface)
    ?       ?   ??? FASTagPaymentMethod.java
    ?       ?   ??? CashPaymentMethod.java
    ?       ?   ??? ExemptPaymentMethod.java
    ?       ??? IBillingReportingService.java
    ?       ??? BillingReportingServiceImpl.java
    ??? test/
        ??? Module1Demo.java                    # Module 1 Isolated Verification
        ??? SystemVerificationSuite.java        # Comprehensive 27-Test End-to-End Suite
```

---

## 3. Object-Oriented Principles Demonstrated

| OOP Principle | Implementation Examples |
| :--- | :--- |
| **Encapsulation** | `FASTagAccount` keeps `balance`, `status`, and `isBlacklisted` private. Balance mutations strictly guarded: negative balances impossible, auto-blocking triggered when balance < ?100, auto-unblocking when ? ?100. |
| **Inheritance** | Base `Vehicle` class extended by `Car`, `Van`, `Bus`, `Truck`, `MultiAxleVehicle`, and `EmergencyVehicle`. Base `Lane` class extended by `FASTagLane`, `CashLane`, and `EmergencyLane`. |
| **Polymorphism** | Strategy interface `PaymentMethod` with polymorphic implementations `FASTagPaymentMethod` (wallet debit), `CashPaymentMethod` (operator cash collection log), and `ExemptPaymentMethod`. |
| **Abstraction** | Service contracts (`IPlazaService`, `IAccountService`, `ITollProcessingService`, `IPricingService`, `IBillingReportingService`) hide in-memory collections, validation rules, and concurrency mechanics. |

---

## 4. Business Rules Enforced

### Module 1: Toll Plaza & Lane Setup
- **Admin-Only Operations**: Only authenticated Admins can create plazas, lanes, and rates.
- **Minimum 2 Lanes Guard**: A plaza cannot open with fewer than 2 lanes.
- **Rate Coverage Guard**: A plaza cannot open until rates exist for **all 5 vehicle categories** (`CAR`, `VAN`, `BUS`, `TRUCK`, `MULTI_AXLE`).
- **Lane Processing Safety Guard**: A lane cannot be closed while a vehicle is actively being processed (`isProcessingVehicle == true`).
- **Effective Dating**: Rate revisions apply strictly on or after the specified effective date; historical transaction rates remain unchanged.

### Module 2: Vehicle & FASTag Accounts
- **Strict 1:1 Tag Link**: Exactly one active FASTag can be associated with any vehicle.
- **Balance Limits**: Minimum balance ?0.00, maximum wallet cap ?50,000.
- **Auto-Block Engine**: Transition to `BLOCKED` triggers automatically when balance drops below ?100.
- **Recharge Rules**: Min ?100, Max ?10,000 per recharge transaction.
- **Auto-Unblock Engine**: Transition from `BLOCKED` back to `ACTIVE` triggers automatically when recharge brings balance ? ?100.
- **Admin Blacklist Engine**: Blacklisted tags (stolen/fraud) are refused at all plazas.

### Module 3: Toll Processing Engine
- **Barrier Control**: Boom barrier opens only upon successful electronic deduction or valid pass.
- **Emergency Priority Bypass**: Ambulances and fire engines cross via Emergency Lane with **?0.00** toll.
- **Anti-Double-Billing Cooldown**: The same tag cannot be charged twice at the same plaza within **2 minutes**.
- **AVC Optical Mismatch**: If optical sensor reads a different vehicle class than registered, the **higher toll** is charged and `isFlaggedForReview` is set to `true`.
- **Cash Fallback - Missing Tag**: Vehicles without active FASTags are diverted to the Cash Lane and pay **Double Toll (2x penalty)**.
- **Cash Fallback - Insufficient Balance**: Blocked/low-balance tags have their FASTag barrier closed, are diverted to the Cash Lane, and pay **Normal Toll** in cash.

### Module 4: Rates, Discounts & Passes
- **24-Hour Return Discount**: Vehicles returning through the same plaza within 24 hours of a Single trip receive the **Return Trip Rate (50% of single rate)** automatically.
- **Monthly Passes**: Unlimited ?0.00 toll crossings during pass validity.
- **Single Pass Guard**: Only one active pass is permitted per vehicle per plaza at a time.
- **Rounding Algorithm**: All computed toll fares are rounded to the nearest ?5 (`Math.round(amount / 5.0) * 5.0`).

### Module 5: Payments, Disputes & Analytics
- **Recharge Records**: Records amount, date, method (`UPI`, `CARD`, `NET_BANKING`), and status (`PAID`, `FAILED`, `REFUNDED`).
- **Cash Operator Auditing**: Cash toll payments record the operator's name / ID.
- **7-Day Dispute Window**: Customer claims must be filed within 7 days of crossing; older claims are rejected.
- **Automatic Refund Engine**: Admin approval of a dispute automatically credits the disputed amount back to the customer's FASTag balance.
- **Low Balance Alerts**: Triggered automatically when wallet balance drops below ?200 after deduction.

---

## 5. SQL Analytical Queries

The system implements the 3 required SQL queries in-memory using Java 17 Streams with equivalent SQL semantics:

### Query 1: Vehicle Type Revenue Summary
For each vehicle type, find total transactions, total toll collected, and average toll per crossing. Show only vehicle types with > 2 successful transactions, sorted by total toll DESC.

```sql
SELECT 
    billed_type AS vehicle_type,
    COUNT(*) AS total_transactions,
    SUM(toll_amount) AS total_toll_collected,
    AVG(toll_amount) AS average_toll
FROM toll_transactions
WHERE status = 'SUCCESS'
GROUP BY billed_type
HAVING COUNT(*) > 2
ORDER BY total_toll_collected DESC;
```

### Query 2: Plaza Crossing & Failure Audit
For each toll plaza, list total crossings, total revenue collected, and number of failed transactions. Group by plaza and include only plazas with at least 3 crossings.

```sql
SELECT 
    plaza_id,
    plaza_name,
    COUNT(*) AS total_crossings,
    SUM(CASE WHEN status = 'SUCCESS' THEN toll_amount ELSE 0 END) AS total_revenue_collected,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_transactions
FROM toll_transactions
GROUP BY plaza_id, plaza_name
HAVING COUNT(*) >= 3;
```

### Query 3: Multi-Plaza FASTag Account Analysis
For each FASTag account, find total crossings, total amount deducted, and distinct plazas used. Include only accounts that crossed at more than 2 distinct plazas, ordered by total amount deducted DESC.

```sql
SELECT 
    tag_id,
    vehicle_reg_no,
    COUNT(*) AS total_crossings,
    SUM(toll_amount) AS total_amount_deducted,
    COUNT(DISTINCT plaza_id) AS distinct_plazas_used
FROM toll_transactions
WHERE tag_id IS NOT NULL AND status = 'SUCCESS'
GROUP BY tag_id, vehicle_reg_no
HAVING COUNT(DISTINCT plaza_id) > 2
ORDER BY total_amount_deducted DESC;
```

---

## 6. How to Build & Run

### Prerequisites
- Java JDK 17 (or JDK 8 / 11 / 21) installed.

### Compile
```powershell
$files = (Get-ChildItem -Recurse -Filter *.java).FullName
javac -d bin -sourcepath src $files
```

### Run the Interactive Application
```powershell
java -cp bin com.toll.Main
```

### Run the Automated 27-Test Verification Suite
```powershell
java -cp bin com.toll.test.SystemVerificationSuite
```
