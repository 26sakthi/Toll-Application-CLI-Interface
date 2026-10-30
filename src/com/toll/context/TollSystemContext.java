package com.toll.context;

import com.toll.model.admin.Admin;
import com.toll.service.account.IAccountService;
import com.toll.service.billing.IBillingReportingService;
import com.toll.service.plaza.IPlazaService;
import com.toll.service.pricing.IPricingService;
import com.toll.service.tollengine.ITollProcessingService;

/**
 * Encapsulates the integrated dependency container for the Toll Management System.
 */
public class TollSystemContext {
    private final IPlazaService plazaService;
    private final IAccountService accountService;
    private final IPricingService pricingService;
    private final IBillingReportingService billingService;
    private final ITollProcessingService tollProcessingService;
    private final Admin defaultAdmin;

    public TollSystemContext(IPlazaService plazaService,
                             IAccountService accountService,
                             IPricingService pricingService,
                             IBillingReportingService billingService,
                             ITollProcessingService tollProcessingService,
                             Admin defaultAdmin) {
        this.plazaService = plazaService;
        this.accountService = accountService;
        this.pricingService = pricingService;
        this.billingService = billingService;
        this.tollProcessingService = tollProcessingService;
        this.defaultAdmin = defaultAdmin;
    }

    public IPlazaService getPlazaService() {
        return plazaService;
    }

    public IAccountService getAccountService() {
        return accountService;
    }

    public IPricingService getPricingService() {
        return pricingService;
    }

    public IBillingReportingService getBillingService() {
        return billingService;
    }

    public ITollProcessingService getTollProcessingService() {
        return tollProcessingService;
    }

    public Admin getDefaultAdmin() {
        return defaultAdmin;
    }
}
