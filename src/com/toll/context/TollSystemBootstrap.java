package com.toll.context;

import com.toll.model.account.FASTagAccount;
import com.toll.model.admin.Admin;
import com.toll.model.enums.*;
import com.toll.model.plaza.*;
import com.toll.model.transaction.TollTransaction;
import com.toll.model.vehicle.*;
import com.toll.service.account.AccountServiceImpl;
import com.toll.service.account.IAccountService;
import com.toll.service.billing.BillingReportingServiceImpl;
import com.toll.service.billing.IBillingReportingService;
import com.toll.service.plaza.IPlazaService;
import com.toll.service.plaza.PlazaServiceImpl;
import com.toll.service.pricing.IPricingService;
import com.toll.service.pricing.PricingServiceImpl;
import com.toll.service.tollengine.ITollProcessingService;
import com.toll.service.tollengine.TollProcessingServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bootstrap factory that initializes the full Toll Management System with
 * realistic Indian National Highway toll plazas, lanes, rates, vehicles, accounts,
 * monthly passes, and pre-seeded historical transactions for immediate SQL analytics.
 */
public class TollSystemBootstrap {

    public static TollSystemContext initializeSystem() {
        Admin admin = new Admin("ADMIN-001", "Shri Rajesh Kumar (NHAI Regional Officer)", "rajesh.kumar@nhai.gov.in");

        IPlazaService plazaService = new PlazaServiceImpl();
        IAccountService accountService = new AccountServiceImpl();
        IPricingService pricingService = new PricingServiceImpl();
        IBillingReportingService billingService = new BillingReportingServiceImpl(accountService);
        ITollProcessingService tollProcessingService = new TollProcessingServiceImpl(
                plazaService, accountService, pricingService, billingService
        );

        LocalDate baseRateDate = LocalDate.of(2026, 1, 1);

        // ========================================================================================
        // 1. SETUP PLAZA 101: Khedshivapur Toll Plaza (NH-48, Pune)
        // ========================================================================================
        TollPlaza p101 = plazaService.createPlaza(admin, "Khedshivapur Toll Plaza", "NH-48 (Mumbai-Bengaluru Highway)", "Pune, Maharashtra");
        plazaService.addLane(admin, p101.getPlazaId(), new FASTagLane(1));
        plazaService.addLane(admin, p101.getPlazaId(), new FASTagLane(2));
        plazaService.addLane(admin, p101.getPlazaId(), new CashLane(3, "Suresh Yadav (POS Operator)"));
        plazaService.addLane(admin, p101.getPlazaId(), new EmergencyLane(4));

        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.CAR, TripType.SINGLE, 110.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.CAR, TripType.RETURN, 165.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.VAN, TripType.SINGLE, 180.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.VAN, TripType.RETURN, 270.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.BUS, TripType.SINGLE, 375.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.BUS, TripType.RETURN, 560.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.TRUCK, TripType.SINGLE, 375.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.TRUCK, TripType.RETURN, 560.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.MULTI_AXLE, TripType.SINGLE, 590.0, baseRateDate);
        plazaService.setTollRate(admin, p101.getPlazaId(), VehicleType.MULTI_AXLE, TripType.RETURN, 885.0, baseRateDate);

        plazaService.openPlaza(admin, p101.getPlazaId());

        // ========================================================================================
        // 2. SETUP PLAZA 102: Kherki Daula Toll Plaza (NH-48, Delhi-NCR)
        // ========================================================================================
        TollPlaza p102 = plazaService.createPlaza(admin, "Kherki Daula Toll Plaza", "NH-48 (Delhi-Gurugram Expressway)", "Gurugram, Haryana");
        plazaService.addLane(admin, p102.getPlazaId(), new FASTagLane(1));
        plazaService.addLane(admin, p102.getPlazaId(), new CashLane(2, "Ramesh Kumar (POS Operator)"));
        plazaService.addLane(admin, p102.getPlazaId(), new EmergencyLane(3));
        plazaService.addLane(admin, p102.getPlazaId(), new FASTagLane(4));

        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.CAR, TripType.SINGLE, 80.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.CAR, TripType.RETURN, 120.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.VAN, TripType.SINGLE, 115.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.VAN, TripType.RETURN, 170.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.BUS, TripType.SINGLE, 235.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.BUS, TripType.RETURN, 350.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.TRUCK, TripType.SINGLE, 235.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.TRUCK, TripType.RETURN, 350.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.MULTI_AXLE, TripType.SINGLE, 370.0, baseRateDate);
        plazaService.setTollRate(admin, p102.getPlazaId(), VehicleType.MULTI_AXLE, TripType.RETURN, 555.0, baseRateDate);

        plazaService.openPlaza(admin, p102.getPlazaId());

        // ========================================================================================
        // 3. SETUP PLAZA 103: Chengalpattu Toll Plaza (NH-45, Chennai)
        // ========================================================================================
        TollPlaza p103 = plazaService.createPlaza(admin, "Chengalpattu Toll Plaza", "NH-45 (Grand Southern Trunk Road)", "Chengalpattu, Tamil Nadu");
        plazaService.addLane(admin, p103.getPlazaId(), new FASTagLane(1));
        plazaService.addLane(admin, p103.getPlazaId(), new CashLane(2, "Anbarasan M (POS Operator)"));
        plazaService.addLane(admin, p103.getPlazaId(), new EmergencyLane(3));

        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.CAR, TripType.SINGLE, 95.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.CAR, TripType.RETURN, 145.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.VAN, TripType.SINGLE, 155.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.VAN, TripType.RETURN, 230.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.BUS, TripType.SINGLE, 320.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.BUS, TripType.RETURN, 480.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.TRUCK, TripType.SINGLE, 320.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.TRUCK, TripType.RETURN, 480.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.MULTI_AXLE, TripType.SINGLE, 510.0, baseRateDate);
        plazaService.setTollRate(admin, p103.getPlazaId(), VehicleType.MULTI_AXLE, TripType.RETURN, 765.0, baseRateDate);

        plazaService.openPlaza(admin, p103.getPlazaId());

        // ========================================================================================
        // 4. REGISTER INITIAL VEHICLES & FASTAG ACCOUNTS
        // ========================================================================================
        Vehicle car1 = new Car("MH-12-DE-1433", "Rahul Sharma", "+91-9822011223");
        accountService.registerVehicle(car1);
        accountService.issueFASTag("IND-TAG-1001", "MH-12-DE-1433", 2500.0);

        Vehicle car2 = new Car("DL-01-AB-1234", "Priya Verma", "+91-9811099887");
        accountService.registerVehicle(car2);
        accountService.issueFASTag("IND-TAG-1002", "DL-01-AB-1234", 150.0); // Close to Rs. 100 auto-block threshold

        Vehicle van1 = new Van("KA-03-MG-5678", "Arvind Swamy", "+91-9845012345");
        accountService.registerVehicle(van1);
        accountService.issueFASTag("IND-TAG-1003", "KA-03-MG-5678", 80.0); // Starts auto-BLOCKED (< Rs. 100)

        Vehicle bus1 = new Bus("TN-09-CB-9999", "KPN Travels Logistics", "+91-9443011122");
        accountService.registerVehicle(bus1);
        accountService.issueFASTag("IND-TAG-1004", "TN-09-CB-9999", 15000.0);

        // Issue Monthly Pass for Bus at Chengalpattu (Plaza #103)
        pricingService.issueMonthlyPass(admin, "TN-09-CB-9999", p103.getPlazaId(), LocalDate.now().minusDays(5), 30, 11000.0);

        Vehicle truck1 = new Truck("HR-26-TR-8888", "Deep Freight Express", "+91-9812033445");
        accountService.registerVehicle(truck1);
        accountService.issueFASTag("IND-TAG-1005", "HR-26-TR-8888", 25000.0);

        Vehicle multiAxle1 = new MultiAxleVehicle("MH-14-AX-7777", "Bharat Heavy Haulage", "+91-9823055667", 4);
        accountService.registerVehicle(multiAxle1);
        accountService.issueFASTag("IND-TAG-1006", "MH-14-AX-7777", 40000.0);

        Vehicle stolenCar = new Car("DL-03-XX-0000", "Stolen Vehicle Suspect", "+91-9999900000");
        accountService.registerVehicle(stolenCar);
        FASTagAccount stolenTag = accountService.issueFASTag("IND-TAG-9999", "DL-03-XX-0000", 3000.0);
        accountService.blacklistTag(admin, "IND-TAG-9999", "FIR #889/2026: Reported Stolen to Delhi Police");

        Vehicle ambulance = new EmergencyVehicle("MH-12-EM-108", "Maharashtra EMS 108", "+91-108", "Ambulance");
        accountService.registerVehicle(ambulance);

        Vehicle fireEngine = new EmergencyVehicle("DL-01-FD-101", "Delhi Fire Services", "+91-101", "Fire Engine");
        accountService.registerVehicle(fireEngine);

        // ========================================================================================
        // 5. PRE-SEED HISTORICAL TRANSACTIONS TO POPULATE SQL ANALYTICAL QUERIES 1, 2, AND 3
        // ========================================================================================
        LocalDateTime now = LocalDateTime.now();

        // Account IND-TAG-1001 crosses Plaza 101, 102, AND 103 (Qualifies for Query 3: > 2 distinct plazas!)
        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-12-DE-1433").plazaId(p101.getPlazaId()).plazaName(p101.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1001").timestamp(now.minusDays(5))
                .tollAmount(110.0).registeredType(VehicleType.CAR).observedType(VehicleType.CAR).billedType(VehicleType.CAR)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-12-DE-1433").plazaId(p102.getPlazaId()).plazaName(p102.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1001").timestamp(now.minusDays(4))
                .tollAmount(80.0).registeredType(VehicleType.CAR).observedType(VehicleType.CAR).billedType(VehicleType.CAR)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-12-DE-1433").plazaId(p103.getPlazaId()).plazaName(p103.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1001").timestamp(now.minusDays(3))
                .tollAmount(95.0).registeredType(VehicleType.CAR).observedType(VehicleType.CAR).billedType(VehicleType.CAR)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-12-DE-1433").plazaId(p101.getPlazaId()).plazaName(p101.getName())
                .laneNumber(2).laneType(LaneType.FASTAG).tagId("IND-TAG-1001").timestamp(now.minusDays(1))
                .tollAmount(110.0).registeredType(VehicleType.CAR).observedType(VehicleType.CAR).billedType(VehicleType.CAR)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        // Account IND-TAG-1005 (Truck) crossings
        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("HR-26-TR-8888").plazaId(p101.getPlazaId()).plazaName(p101.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1005").timestamp(now.minusDays(4))
                .tollAmount(375.0).registeredType(VehicleType.TRUCK).observedType(VehicleType.TRUCK).billedType(VehicleType.TRUCK)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("HR-26-TR-8888").plazaId(p102.getPlazaId()).plazaName(p102.getName())
                .laneNumber(4).laneType(LaneType.FASTAG).tagId("IND-TAG-1005").timestamp(now.minusDays(2))
                .tollAmount(235.0).registeredType(VehicleType.TRUCK).observedType(VehicleType.TRUCK).billedType(VehicleType.TRUCK)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("HR-26-TR-8888").plazaId(p103.getPlazaId()).plazaName(p103.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1005").timestamp(now.minusDays(1))
                .tollAmount(320.0).registeredType(VehicleType.TRUCK).observedType(VehicleType.TRUCK).billedType(VehicleType.TRUCK)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        // Account IND-TAG-1006 (Multi-Axle) crossings
        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-14-AX-7777").plazaId(p101.getPlazaId()).plazaName(p101.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1006").timestamp(now.minusDays(3))
                .tollAmount(590.0).registeredType(VehicleType.MULTI_AXLE).observedType(VehicleType.MULTI_AXLE).billedType(VehicleType.MULTI_AXLE)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-14-AX-7777").plazaId(p102.getPlazaId()).plazaName(p102.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1006").timestamp(now.minusDays(2))
                .tollAmount(370.0).registeredType(VehicleType.MULTI_AXLE).observedType(VehicleType.MULTI_AXLE).billedType(VehicleType.MULTI_AXLE)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("MH-14-AX-7777").plazaId(p103.getPlazaId()).plazaName(p103.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1006").timestamp(now.minusDays(1))
                .tollAmount(510.0).registeredType(VehicleType.MULTI_AXLE).observedType(VehicleType.MULTI_AXLE).billedType(VehicleType.MULTI_AXLE)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.SUCCESS).build());

        // Some failed transactions to verify Plaza failure auditing in Query 2
        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("DL-03-XX-0000").plazaId(p101.getPlazaId()).plazaName(p101.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-9999").timestamp(now.minusDays(1))
                .tollAmount(0.0).registeredType(VehicleType.CAR).observedType(VehicleType.CAR).billedType(VehicleType.CAR)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.FAILED)
                .failureReason("TAG_BLACKLISTED").build());

        billingService.recordTransaction(TollTransaction.builder()
                .vehicleRegNo("KA-03-MG-5678").plazaId(p102.getPlazaId()).plazaName(p102.getName())
                .laneNumber(1).laneType(LaneType.FASTAG).tagId("IND-TAG-1003").timestamp(now.minusHours(12))
                .tollAmount(0.0).registeredType(VehicleType.VAN).observedType(VehicleType.VAN).billedType(VehicleType.VAN)
                .tripType(TripType.SINGLE).paymentType(PaymentType.FASTAG).status(TransactionStatus.FAILED)
                .failureReason("TAG_AUTO_BLOCKED_LOW_BALANCE").build());

        return new TollSystemContext(plazaService, accountService, pricingService, billingService, tollProcessingService, admin);
    }
}
