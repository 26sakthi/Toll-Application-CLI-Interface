# Toll Management System (FASTag)
## Implementation Plan, 5-Member Module Division, ER Diagram & Workflows

---

## 1. Project Overview & Indian Highway FASTag Context

The **Toll Management System (FASTag)** is an automated highway toll collection application modeled on Indian National Highway Authority of India (NHAI) and MoRTH guidelines. It handles electronic toll collection (ETC) using RFID FASTags, cash and emergency fallback lanes, automated dynamic rate discounting (24-hour return trips, monthly passes), balance lifecycle management, dispute handling, and analytical reporting.

### Core Technical Pillars:
- **Language**: Java (Standard SE 8 / 11 / 17 / 21) — no third-party library dependencies.
- **Architecture**: Domain-Driven Design (DDD) with clear separation of models, services, repositories, and CLI presentation.
- **Storage**: In-Memory Collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `PriorityQueue`) for thread-safe runtime state management.
- **Interface**: Indian Highway themed Interactive Console CLI (with ANSI terminal colors, ASCII tables, and automated simulation modes).
- **OOP Tenets**:
  - **Encapsulation**: Private fields, immutable identifiers, state mutations strictly via validated business methods.
  - **Inheritance**: `Vehicle` hierarchy (`Car`, `Van`, `Bus`, `Truck`, `MultiAxleVehicle`) and `Lane` hierarchy (`FASTagLane`, `CashLane`, `EmergencyLane`).
  - **Polymorphism**: `PaymentMethod` strategy interface (`FASTagPayment`, `CashPayment`) executing payments polymorphically.
  - **Abstraction**: High-level service contracts (`IPlazaService`, `IAccountService`, `ITollProcessingService`, `IPricingService`, `IBillingReportingService`).

---

## 2. 5-Member Team Allocation & Responsibility Matrix

The system is partitioned into **5 autonomous modules**, each assigned to a dedicated team member. Clear interface contracts ensure independent development, unit testing, and smooth integration.

| Member | Role & Ownership | Module Name | Core Domain Deliverables | Direct Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **Member 1** | **Infrastructure & Setup Lead** | **Module 1: Toll Plaza & Lane Setup** | Toll Plaza lifecycle, Multi-modal Lanes ($\ge 2$), Rate Configurations, Effective Dating engine, Lane Processing Locks. | None (Base Infrastructure) |
| **Member 2** | **Account & Identity Lead** | **Module 2: Vehicle & FASTag Accounts** | Vehicle Registry, 1:1 Tag Association, Balance Lifecycle (₹100–₹50k), Auto-block (<₹100), Auto-unblock, Admin Blacklisting. | `Vehicle` models |
| **Member 3** | **Edge & Toll Processing Lead** | **Module 3: Toll Processing Engine** | RFID Crossing Engine, 2-Min Anti-Double-Billing, Emergency Bypass, AVC Mismatch Detection & Flagging, Cash Fallback Router. | Modules 1, 2, 4, 5 |
| **Member 4** | **Pricing & Subscription Lead** | **Module 4: Rates, Discounts & Passes** | 24-Hour Return Trip (50% discount) engine, Monthly Pass Issuance & Validation, Nearest ₹5 Rounding Algorithm. | Modules 1, 2 |
| **Member 5** | **Billing, Disputes & Analytics Lead**| **Module 5: Payments, Disputes & Reports** | Recharge Transactions (UPI/Card/NetBanking), Cash Operator Auditing, 7-Day Dispute & Refund Engine, Low Balance Alerts (<₹200), SQL Queries 1, 2, 3. | Modules 2, 3 |

---

## 3. Detailed Module Breakdown for the 5 Team Members

```mermaid
graph TD
    subgraph Team_Allocation["5-Member Module Division Architecture"]
        M1["Member 1: Toll Plaza & Lane Setup\n- TollPlaza & Lane Entities (>=2 Lanes)\n- TollRate with EffectiveDate\n- Plaza & Lane Safety Controllers"]
        M2["Member 2: Vehicle & FASTag Accounts\n- Vehicle Class Hierarchy (Car/Van/Bus/Truck/MultiAxle)\n- FASTagAccount (1:1 Link, Balance, Blacklist)\n- Auto-Block (<₹100) & Auto-Unblock"]
        M3["Member 3: Toll Processing Engine\n- Crossing Pipeline & Barrier Control\n- 2-Min Duplicate Cooldown\n- AVC Mismatch: Higher Toll + Flagging\n- Cash Fallbacks (2x Double or Normal)"]
        M4["Member 4: Rates, Discounts & Passes\n- 24h Return Trip Engine (50% Off)\n- Monthly Pass Management (₹0 Crossings)\n- Round to Nearest ₹5 Algorithm"]
        M5["Member 5: Payments, Disputes & Reports\n- Recharge Engine (UPI, Card, NetBanking)\n- Cash Operator Audit Journal\n- 7-Day Dispute & Admin Refund\n- Low Balance Alert (<₹200)\n- SQL Queries 1, 2, 3 & Daily Reports"]
    end

    M1 --> M3
    M2 --> M3
    M4 --> M3
    M3 --> M5
    M2 --> M5
    M1 --> M4
    M2 --> M4
```

---

### Member 1: Infrastructure & Setup Lead
#### **Module 1: Toll Plaza and Lane Setup**
* **Assigned Member**: Developer 1 (Infrastructure Lead)
* **Package**: `com.toll.module1.plaza`
* **Core Responsibilities**:
  1. **Toll Plaza Administration**: Only an Admin can create plazas. A plaza has an ID, name (e.g. *Khedshivapur Plaza*, *Kherki Daula Plaza*), highway name (e.g. *NH-48*, *NH-44*), location, and must have **at least 2 lanes**.
  2. **Lane Lifecycle & Safety**:
     - Lane types: `FASTAG`, `CASH`, `EMERGENCY`.
     - Status: `OPEN`, `CLOSED`.
     - **Safety Rule**: A lane can be set to `CLOSED` **only if** `isProcessingVehicle == false`. Closing is rejected if a vehicle is in the middle of crossing.
  3. **Effective Dating Rate Engine**:
     - Rates configured per plaza, vehicle type (`CAR`, `VAN`, `BUS`, `TRUCK`, `MULTI_AXLE`), and trip type (`SINGLE`, `RETURN`) with an `effectiveDate`.
     - **Plaza Opening Guard**: A plaza **cannot open** until an active rate exists for **every vehicle type**.
     - Rate changes apply **strictly on or after** the effective date without modifying past transaction historical rates.
* **Key Classes & Interfaces**:
  - `TollPlaza.java`: Encapsulates plaza attributes, list of lanes, and rate tables.
  - `Lane.java` *(Abstract)*: Base class with `laneNumber`, `status`, `isProcessingVehicle`.
  - `FASTagLane.java`, `CashLane.java`, `EmergencyLane.java` *(Subclasses)*.
  - `TollRate.java`: Stores plaza, vehicle type, trip type, rate amount, and effective date.
  - `IPlazaService.java` & `PlazaServiceImpl.java`: Admin operations and readiness validations.
* **Module 1 Specific Workflow**:

```mermaid
flowchart TD
    M1_Start(["Admin Initiates Setup"]) --> CreatePlaza["Create Toll Plaza\n(Name, Highway, Location)\nInitial Status = INACTIVE"]
    CreatePlaza --> AddLanes["Add Lanes to Plaza\n(FASTag, Cash, Emergency)"]
    AddLanes --> SetRates["Set Toll Rates with Effective Date\n(Per Vehicle Type & Trip Type)"]
    
    SetRates --> AttemptOpen{"Admin Attempts to\nOpen Plaza"}
    AttemptOpen --> CheckLaneCount{"Total Lanes >= 2?"}
    CheckLaneCount -- No --> RejectOpen1["REJECT: Plaza must have at least 2 lanes!"]
    CheckLaneCount -- Yes --> CheckRates{"Rates exist for ALL 5 Vehicle Types\n(Car, Van, Bus, Truck, Multi-Axle)?"}
    CheckRates -- No --> RejectOpen2["REJECT: Missing rates for one or more vehicle types!"]
    CheckRates -- Yes --> ApproveOpen["Plaza Status = ACTIVE\nLanes can now be opened"]

    ApproveOpen --> LaneCloseReq{"Request to\nClose a Lane"}
    LaneCloseReq --> CheckBusy{"Is vehicle being\nprocessed in lane?"}
    CheckBusy -- Yes --> RejectClose["REJECT CLOSE: Lane busy with active vehicle!"]
    CheckBusy -- No --> CloseLane["Lane Status = CLOSED"]
```

---

### Member 2: Account & Identity Lead
#### **Module 2: Vehicle and FASTag Accounts**
* **Assigned Member**: Developer 2 (Account & Identity Lead)
* **Package**: `com.toll.module2.account`
* **Core Responsibilities**:
  1. **Vehicle Registry & Hierarchy**:
     - Base `Vehicle` class extended by `Car`, `Van`, `Bus`, `Truck`, and `MultiAxleVehicle`.
     - Unique Indian registration number (e.g. `MH-12-DE-1433`, `DL-01-AB-1234`), owner name, phone number.
  2. **1:1 FASTag Account Association**:
     - Exactly one active tag linked per vehicle.
     - Unique `tagId` (e.g. `IND-FASTAG-7001`).
  3. **Balance Rules & State Transitions**:
     - Status: `ACTIVE`, `BLOCKED`, `CLOSED`.
     - Balance must **never be negative**. Max allowed balance is **₹50,000**.
     - **Auto-Block**: Automatically transitions to `BLOCKED` if balance drops below **₹100**.
     - **Auto-Unblock**: A blocked tag automatically returns to `ACTIVE` when a recharge takes balance to **$\ge$ ₹100**.
     - Recharge bounds: Minimum **₹100**, maximum **₹10,000** per recharge transaction.
  4. **Admin Blacklist Engine**:
     - Admin can blacklist fraudulent or stolen tags. Blacklisted tags are rejected at all plazas.
* **Key Classes & Interfaces**:
  - `Vehicle.java` *(Abstract)*, `Car.java`, `Van.java`, `Bus.java`, `Truck.java`, `MultiAxleVehicle.java`.
  - `FASTagAccount.java`: Private balance, status, blacklisted flag, mutators with automatic state triggers.
  - `IAccountService.java` & `AccountServiceImpl.java`: Registry management, recharges, blacklisting.
* **Module 2 Specific Workflow**:

```mermaid
flowchart TD
    M2_Reg["Register Vehicle & Link 1:1 Tag"] --> TagActive["FASTag Account Created\nStatus = ACTIVE"]
    
    TagActive --> DebitAction["Toll Deducted from Balance"]
    DebitAction --> BalanceCheck{"Remaining Balance < ₹100?"}
    BalanceCheck -- Yes --> AutoBlock["AUTOMATIC TRANSITION:\nStatus = BLOCKED"]
    BalanceCheck -- No --> RemainActive["Status Remains ACTIVE"]
    
    AutoBlock --> RechargeAction["User Performs Recharge\n(Min ₹100, Max ₹10,000, Cap ₹50,000)"]
    RemainActive --> RechargeAction
    
    RechargeAction --> CheckNewBal{"New Balance >= ₹100\n& Previous Status was BLOCKED?"}
    CheckNewBal -- Yes --> AutoUnblock["AUTOMATIC TRANSITION:\nStatus = ACTIVE"]
    CheckNewBal -- No --> KeepStatus["Status Unchanged"]

    TagActive --> AdminBlacklist["Admin Flags Tag as Stolen/Fraud"]
    AdminBlacklist --> BlacklistState["Status = BLACKLISTED\nRefused at All Plazas"]
```

---

### Member 3: Edge & Toll Processing Lead
#### **Module 3: Toll Processing Engine**
* **Assigned Member**: Developer 3 (Core Processing Lead)
* **Package**: `com.toll.module3.tollengine`
* **Core Responsibilities**:
  1. **Real-time Lane Crossing Pipeline**:
     - Reads RFID tag and fetches linked vehicle.
     - Controls barrier actuator: barrier opens **only after** toll is deducted successfully.
  2. **Emergency Lane Bypass**:
     - Emergency vehicles (Ambulance, Fire Engine) crossing via Emergency Lane pay **₹0** toll.
  3. **Anti-Tailgating & Anti-Double-Billing**:
     - The same tag **cannot be charged twice** at the same plaza within **2 minutes**.
  4. **Vehicle Type Mismatch Enforcement**:
     - If optical AVC (observed vehicle type) differs from registered type, the **higher toll** is charged, and `isFlaggedForReview` is set to `true`.
  5. **Cash Lane Fallback Routing**:
     - **No Valid Active Tag**: Diverted to Cash Lane and charged **Double Toll (2x)**.
     - **Insufficient Balance**: Barrier stays closed. Diverted to Cash Lane and pays **Normal Toll** in cash.
  6. **Transaction Logging**:
     - Creates immutable `TollTransaction` record (UUID, vehicle, plaza, lane, timestamp, amount, status, failure reason, flag status).
* **Key Classes & Interfaces**:
  - `TollTransaction.java`: Data model capturing crossing details and review flags.
  - `ITollProcessingService.java` & `TollProcessingServiceImpl.java`: Orchestrates the crossing pipeline.
  - `CrossingContext.java`: Value object encapsulating lane, tag, observed vehicle type, and timestamp.
* **Module 3 Specific Workflow**:

```mermaid
flowchart TD
    VehicleArrive(["Vehicle Enters Lane"]) --> LaneCheck{"Lane Type?"}
    
    %% Emergency Lane
    LaneCheck -- "Emergency" --> IsEmg{"Is Emergency Vehicle?"}
    IsEmg -- Yes --> FreePass["Toll = ₹0 | Status = SUCCESS"] --> OpenBarrier["Open Boom Barrier"]
    IsEmg -- No --> EmgReject["Refuse Crossing in Emergency Lane"] --> StopCrossing(["End"])

    %% FASTag Lane
    LaneCheck -- "FASTag" --> ReadTag["Scan FASTag RFID"]
    ReadTag --> TagValid{"Tag Exists & Linked?"}
    TagValid -- No --> PenaltyCash["DIVERT TO CASH LANE:\nPay DOUBLE TOLL (2x)"] --> CashProcess["Process in Cash Lane"]
    
    TagValid -- Yes --> CheckBL{"Is Blacklisted?"}
    CheckBL -- Yes --> RejectBL["REFUSE AT PLAZA:\nBlacklisted Tag Alarm"] --> StopCrossing

    CheckBL -- No --> Cooldown{"Charged at this plaza\nwithin last 2 mins?"}
    Cooldown -- Yes --> RejectDup["REJECT: 2-Min Duplicate Protection"] --> StopCrossing

    Cooldown -- No --> CheckPassDiscount["Member 4 Engine:\nCheck Pass & Return Discount"]
    CheckPassDiscount --> MismatchCheck{"Observed Type ==\nRegistered Type?"}
    MismatchCheck -- Mismatch --> HigherToll["Charge HIGHER Toll Rate\nSet Flagged For Review = TRUE"]
    MismatchCheck -- Match --> NormalToll["Charge Applicable Toll Rate"]

    HigherToll --> BalCheck{"Tag Status == ACTIVE\n& Balance >= Toll?"}
    NormalToll --> BalCheck

    BalCheck -- "Blocked or Low Bal" --> NormalCash["DIVERT TO CASH LANE:\nPay NORMAL TOLL in Cash"] --> CashProcess
    BalCheck -- "Sufficient" --> DeductBal["Deduct Toll from Tag Balance\nLog SUCCESS Transaction"]
    DeductBal --> AlertTrigger["Member 5 Trigger:\nLow Balance Alert if < ₹200"]
    AlertTrigger --> OpenBarrier
    OpenBarrier --> VehicleExit["Vehicle Clears Lane\nReset Lane Idle State"] --> StopCrossing
```

---

### Member 4: Pricing & Subscription Lead
#### **Module 4: Rates, Discounts and Passes**
* **Assigned Member**: Developer 4 (Pricing & Subscription Lead)
* **Package**: `com.toll.module4.pricing`
* **Core Responsibilities**:
  1. **24-Hour Return Trip Discount Engine**:
     - If a vehicle returns through the same plaza within **24 hours** of a previous Single trip, the **Return Trip Rate (50% of Single trip rate)** is applied automatically.
  2. **Monthly Pass Management**:
     - Vehicle owner can hold a monthly pass for a specific plaza with a valid start and end date.
     - Rule: A vehicle can have **only one valid pass per plaza** at a time.
     - A vehicle with a valid pass pays **₹0** toll at that plaza.
     - Pass fees are configured by Admin per vehicle type and plaza.
  3. **Nearest ₹5 Rounding Algorithm**:
     - All computed toll amounts must be mathematically rounded to the nearest ₹5:
       $$\text{Rounded Amount} = \text{Math.round}(\text{amount} / 5.0) \times 5$$
* **Key Classes & Interfaces**:
  - `MonthlyPass.java`: Encapsulates pass ID, vehicle reg number, plaza ID, date range, fee, and validity checker.
  - `IPricingService.java` & `PricingServiceImpl.java`: Rates calculation, return trip detection, rounding logic.
  - `PassFeeConfig.java`: Configuration of pass rates per vehicle type.
* **Module 4 Specific Workflow**:

```mermaid
flowchart TD
    M4_Start(["Toll Amount Evaluation Request"]) --> CheckPass{"Does Vehicle Have Active\nMonthly Pass for THIS Plaza?"}
    
    CheckPass -- Yes --> FreePassToll["Applicable Toll = ₹0.00\nTrip Type = PASS\n(Bypass balance deduction)"] --> ReturnResult(["Return Final Fare"])
    
    CheckPass -- No --> LookupHistory{"Single Trip at THIS Plaza\nwithin past 24 Hours?"}
    
    LookupHistory -- Yes --> ApplyReturnRate["Apply RETURN TRIP RATE:\n50% of Single Trip Base Rate\nTrip Type = RETURN"]
    LookupHistory -- No --> ApplySingleRate["Apply SINGLE TRIP RATE:\n100% of Single Trip Base Rate\nTrip Type = SINGLE"]

    ApplyReturnRate --> RoundStep["Apply Nearest ₹5 Rounding\nFormula: ROUND(fare / 5.0) * 5"]
    ApplySingleRate --> RoundStep
    RoundStep --> ReturnResult
```

---

### Member 5: Billing, Disputes & Analytics Lead
#### **Module 5: Payments, Disputes and Reports**
* **Assigned Member**: Developer 5 (Billing & Analytics Lead)
* **Package**: `com.toll.module5.billing`
* **Core Responsibilities**:
  1. **Polymorphic Payment System**:
     - `PaymentMethod` interface with `FASTagPaymentMethod` and `CashPaymentMethod`.
     - Cash toll payments record collected amount and the **Lane Operator Name / ID**.
     - Recharge methods: `UPI`, `CARD`, `NET_BANKING`. Stores recharge amount, date, method, and status (`PAID`, `FAILED`, `REFUNDED`).
  2. **7-Day Dispute & Refund Engine**:
     - A vehicle owner can raise a dispute for a wrong deduction within **7 days** of transaction timestamp. Disputes older than 7 days are rejected.
     - Admin can review, approve, or reject. Upon approval, the disputed amount is **automatically refunded to the tag balance**.
  3. **Low Balance Alert Trigger**:
     - When balance drops below **₹200** after a deduction, an automated `LowBalanceAlert` is generated.
  4. **Reporting & SQL Analytics**:
     - Daily revenue report per plaza.
     - Total vehicle count per lane.
     - **SQL Exercise Query 1**: Vehicle type revenue summary (>2 successful txns, sorted by toll DESC).
     - **SQL Exercise Query 2**: Plaza crossing & failure audit ($\ge 3$ crossings).
     - **SQL Exercise Query 3**: Multi-plaza account analysis (>2 distinct plazas, sorted by amount DESC).
* **Key Classes & Interfaces**:
  - `PaymentMethod.java` *(Interface)*, `FASTagPayment.java`, `CashPayment.java`.
  - `RechargeRecord.java`, `Dispute.java`, `LowBalanceAlert.java`.
  - `IBillingReportingService.java` & `BillingReportingServiceImpl.java`.
* **Module 5 Specific Workflow**:

```mermaid
flowchart TD
    subgraph Payment_And_Alert["Payment & Low Balance Alert Flow"]
        PostDeduct["Toll Deducted Successfully"] --> CheckAlert{"New Balance < ₹200?"}
        CheckAlert -- Yes --> TriggerAlert["Generate LowBalanceAlert\nLog Alert Record with Tag ID & Balance"]
        CheckAlert -- No --> NormalDone["No Alert Needed"]
    end

    subgraph Dispute_Flow["Dispute & Refund Workflow"]
        OwnerDispute["Vehicle Owner Raises Dispute on Transaction"] --> Check7Days{"Transaction Age <= 7 Days?"}
        Check7Days -- No --> ExpireDispute["REJECT: Dispute filed after 7-day limit!"]
        Check7Days -- Yes --> CreateDispute["Create Dispute Record (Status = RAISED)"]
        CreateDispute --> AdminDecision{"Admin Decision?"}
        AdminDecision -- Reject --> DispRejected["Status = REJECTED\nSave Remarks"]
        AdminDecision -- Approve --> DispApproved["Status = APPROVED\nExecute Automatic Refund to FASTag Balance"]
    end

    subgraph Analytics_Flow["Analytics & SQL Engine"]
        ReqReports["Admin Requests Reports"] --> RunSQL["Execute In-Memory SQL Aggregate Queries\n- Query 1: Vehicle Type Aggregation\n- Query 2: Plaza Performance\n- Query 3: Multi-Plaza Accounts"]
    end
```

---

## 4. Complete Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    ADMIN {
        int admin_id PK
        string username
        string full_name
        string role
    }

    TOLL_PLAZA {
        int plaza_id PK
        string plaza_name "e.g. Khedshivapur Plaza"
        string highway_name "e.g. NH-48"
        string location "e.g. Pune, MH"
        string status "ACTIVE, INACTIVE"
    }

    LANE {
        int lane_id PK
        int plaza_id FK
        int lane_number
        string lane_type "FASTAG, CASH, EMERGENCY"
        string status "OPEN, CLOSED"
        boolean is_processing_vehicle "Safety close guard"
    }

    LANE_OPERATOR {
        int operator_id PK
        string operator_name
        string employee_code UK
    }

    TOLL_RATE {
        int rate_id PK
        int plaza_id FK
        string vehicle_type "CAR, VAN, BUS, TRUCK, MULTI_AXLE"
        string trip_type "SINGLE, RETURN"
        decimal amount
        date effective_date
    }

    VEHICLE {
        string registration_number PK "Unique (e.g. MH-12-DE-1433)"
        string vehicle_type "CAR, VAN, BUS, TRUCK, MULTI_AXLE"
        string owner_name
        string phone_number
    }

    FASTAG_ACCOUNT {
        string tag_id PK "Unique (e.g. IND-TAG-1001)"
        string vehicle_reg_no FK, UK "Strict 1:1"
        decimal balance "Min 0, Max 50,000"
        string status "ACTIVE, BLOCKED, CLOSED"
        boolean is_blacklisted
        datetime blocked_at
    }

    MONTHLY_PASS {
        string pass_id PK
        string vehicle_reg_no FK
        int plaza_id FK
        date start_date
        date end_date
        decimal fee_paid
        string status "ACTIVE, EXPIRED"
    }

    TOLL_TRANSACTION {
        string transaction_id PK "UUID"
        string vehicle_reg_no FK
        int plaza_id FK
        int lane_number
        string tag_id FK "Nullable for pure cash"
        datetime timestamp
        decimal toll_amount "Rounded to nearest ₹5"
        string observed_type
        string billed_type
        string trip_type "SINGLE, RETURN, PASS, EMERGENCY"
        string payment_method "FASTAG, CASH, EXEMPT"
        string status "SUCCESS, FAILED, PENDING"
        boolean is_flagged_for_review
        string operator_name "For cash transactions"
    }

    RECHARGE_RECORD {
        string recharge_id PK
        string tag_id FK
        decimal amount "Min 100, Max 10,000"
        string method "UPI, CARD, NET_BANKING"
        string status "PAID, FAILED, REFUNDED"
        datetime recharge_time
    }

    DISPUTE {
        string dispute_id PK
        string transaction_id FK, UK
        string vehicle_reg_no FK
        decimal disputed_amount
        string reason
        string status "RAISED, APPROVED, REJECTED"
        datetime raised_time
        string admin_remarks
    }

    LOW_BALANCE_ALERT {
        string alert_id PK
        string tag_id FK
        string vehicle_reg_no FK
        decimal balance_after_deduction "Triggered when < 200"
        datetime alert_time
    }

    %% Relationships
    ADMIN ||--o{ TOLL_PLAZA : "configures"
    ADMIN ||--o{ DISPUTE : "reviews"
    TOLL_PLAZA ||--|{ LANE : "has >= 2"
    TOLL_PLAZA ||--|{ TOLL_RATE : "defines rates for"
    TOLL_PLAZA ||--o{ MONTHLY_PASS : "honors"
    TOLL_PLAZA ||--o{ TOLL_TRANSACTION : "records"

    LANE ||--o{ TOLL_TRANSACTION : "processes"
    LANE_OPERATOR ||--o{ TOLL_TRANSACTION : "collects cash in"

    VEHICLE ||--|| FASTAG_ACCOUNT : "linked to exactly 1 active"
    VEHICLE ||--o{ MONTHLY_PASS : "holds"
    VEHICLE ||--o{ TOLL_TRANSACTION : "crosses plaza with"
    VEHICLE ||--o{ DISPUTE : "raises"

    FASTAG_ACCOUNT ||--o{ RECHARGE_RECORD : "credited by"
    FASTAG_ACCOUNT ||--o{ TOLL_TRANSACTION : "debited by"
    FASTAG_ACCOUNT ||--o{ LOW_BALANCE_ALERT : "receives alerts"

    TOLL_TRANSACTION ||--o| DISPUTE : "subject to"
```

---

## 5. End-to-End Integrated System Workflow

This diagram unites all 5 members' deliverables into one continuous operational flow:

```mermaid
sequenceDiagram
    autonumber
    actor Driver as Vehicle Driver
    participant L as Lane (Member 1)
    participant RFID as RFID Reader (Member 3)
    participant Engine as Toll Engine (Member 3)
    participant Acc as Account Service (Member 2)
    participant Pricing as Pricing & Pass Engine (Member 4)
    participant Bill as Billing & Dispute Service (Member 5)
    actor Admin as Plaza Admin

    %% Vehicle Approaches
    Driver->>L: Vehicle approaches Lane
    L->>L: Set isProcessingVehicle = TRUE

    alt Emergency Lane
        Engine->>Engine: Verify Emergency Vehicle
        Engine->>L: Open Barrier (Toll = ₹0)
        Engine->>Bill: Log Transaction (EMERGENCY, SUCCESS)
    else FASTag Lane
        L->>RFID: Scan Tag ID
        RFID->>Engine: Deliver Tag ID & Optical AVC Vehicle Type
        
        %% Step 1: Account Checks
        Engine->>Acc: Fetch Tag & Vehicle Profile
        alt Tag Blacklisted
            Acc-->>Engine: Tag is BLACKLISTED
            Engine->>L: Refuse Crossing, Sound Alarm
            Engine->>Bill: Log Transaction (FAILED, Blacklisted)
        else Tag Valid & Cooldown Check
            Engine->>Engine: Verify 2-Min Cooldown at Plaza
            
            %% Step 2: Pricing & Passes
            Engine->>Pricing: Check Active Monthly Pass for (Vehicle, Plaza)
            alt Has Valid Monthly Pass
                Pricing-->>Engine: Pass Valid (Fare = ₹0)
                Engine->>L: Open Boom Barrier
                Engine->>Bill: Log Transaction (PASS, SUCCESS)
            else Normal Crossing / Return Trip
                Pricing->>Pricing: Check past 24h single trips (Apply 50% if return)
                Pricing->>Pricing: Compare observed vs registered type (Apply higher toll if mismatch)
                Pricing->>Pricing: Round computed toll to nearest ₹5
                Pricing-->>Engine: Return Final Payable Toll Amount
                
                %% Step 3: Payment Deduction
                Engine->>Acc: Verify Status == ACTIVE and Balance >= Toll
                alt Insufficient Balance / Blocked
                    Acc-->>Engine: Balance Insufficient or Account Blocked (<₹100)
                    Engine->>L: Keep Barrier Closed -> Divert to Cash Lane
                    Driver->>L: Pay Normal Toll in Cash to Operator
                    Engine->>Bill: Log Cash Payment with Operator Name
                    Engine->>L: Open Barrier
                else Sufficient Balance
                    Engine->>Acc: Deduct Toll Amount
                    Acc->>Acc: Auto-Block if Balance < ₹100
                    Acc-->>Engine: Debit Successful
                    
                    %% Step 4: Low Balance Alert
                    opt Balance < ₹200
                        Acc->>Bill: Trigger Low Balance Alert (< ₹200)
                    end
                    
                    Engine->>Bill: Log Transaction (FASTAG, SUCCESS)
                    Engine->>L: Open Boom Barrier
                end
            end
        end
    else No Tag in FASTag Lane
        Engine->>L: Divert to Cash Lane (Penalty)
        Driver->>L: Pay DOUBLE TOLL (2x) in Cash to Operator
        Engine->>Bill: Log Cash Penalty with Operator Name
        Engine->>L: Open Barrier
    end

    L->>L: Vehicle Clears Lane -> Set isProcessingVehicle = FALSE

    %% Post-Crossing Dispute Flow
    opt Dispute within 7 Days
        Driver->>Bill: Raise Dispute for Wrong Deduction (within 7 days)
        Admin->>Bill: Review & Approve Dispute
        Bill->>Acc: Refund Disputed Amount to FASTag Balance
    end
```

---

## 6. Implementation Timeline & Integration Roadmap

```mermaid
gantt
    title Toll Management System (FASTag) 5-Member Implementation Roadmap
    dateFormat  YYYY-MM-DD
    section Phase 1: Models & Contracts
    Member 1 Base Plaza & Lane Models        :a1, 2026-11-01, 2d
    Member 2 Vehicle & FASTag Models         :a2, 2026-11-01, 2d
    Member 4 Pass & Pricing Models           :a3, 2026-11-01, 2d
    Member 5 Payment & Dispute Models        :a4, 2026-11-01, 2d

    section Phase 2: Core Domain Logic
    Member 1 Effective Rates & Safety Guards :b1, 2026-11-03, 3d
    Member 2 Balance Lifecycle & Auto-Block  :b2, 2026-11-03, 3d
    Member 4 Return 24h & Nearest 5 Rounding :b3, 2026-11-03, 3d
    Member 5 Recharge & Dispute Refund Engine:b4, 2026-11-03, 3d

    section Phase 3: Integration & Processing Engine
    Member 3 Toll Processing Engine Pipeline :c1, 2026-11-06, 3d
    Member 5 SQL Analytics & Query 1, 2, 3   :c2, 2026-11-07, 2d

    section Phase 4: CLI Interface & Testing
    Team CLI Assembly & Indian Theme Menu    :d1, 2026-11-09, 2d
    Full Business Rules Automated Test Suite :d2, 2026-11-10, 1d
```

---

## 7. Individual Member Acceptance Criteria Checklist

### Member 1 Checklist (Plaza & Lanes)
- [ ] Plazas enforce a minimum of 2 lanes.
- [ ] Plaza open status blocked unless rates exist for all 5 vehicle types (`CAR`, `VAN`, `BUS`, `TRUCK`, `MULTI_AXLE`).
- [ ] Rate effective date is strictly honored without altering historical transaction rates.
- [ ] Lane close operation blocked if `isProcessingVehicle == true`.

### Member 2 Checklist (Vehicles & Accounts)
- [ ] Vehicle registration numbers are unique.
- [ ] FASTag account is linked strictly 1:1 with a vehicle.
- [ ] Balance never negative; maximum balance capped at ₹50,000.
- [ ] Auto-block triggers when balance < ₹100.
- [ ] Auto-unblock triggers when recharge takes balance $\ge$ ₹100.
- [ ] Blacklisted tags are rejected at all plazas.

### Member 3 Checklist (Toll Processing Engine)
- [ ] Barrier opens only after successful deduction or valid pass.
- [ ] Emergency vehicles pay ₹0 in Emergency Lane.
- [ ] 2-minute cooldown prevents double-charging the same tag at the same plaza.
- [ ] Observed vehicle mismatch charges the higher toll and sets `isFlaggedForReview = true`.
- [ ] Missing tag charged Double Toll (2x) in Cash Lane.
- [ ] Insufficient balance charged Normal Toll in Cash Lane.

### Member 4 Checklist (Rates & Passes)
- [ ] 24-hour return trip automatically applies 50% discount on Single trip rate.
- [ ] Monthly pass allows unlimited ₹0 toll crossings at designated plaza.
- [ ] Only one active monthly pass allowed per vehicle per plaza.
- [ ] All final toll amounts rounded to nearest ₹5 (`ROUND(amount / 5.0) * 5`).

### Member 5 Checklist (Payments, Disputes & Reports)
- [ ] Recharge accepts UPI, Card, Net Banking with ₹100–₹10,000 bounds.
- [ ] Cash toll transactions record the collecting Lane Operator's name.
- [ ] Disputes allowed strictly within 7 days of crossing; approved disputes refund directly to FASTag balance.
- [ ] Low balance alerts trigger when balance drops below ₹200.
- [ ] In-memory execution of SQL Queries 1, 2, and 3 produces exact grouped/filtered results.
