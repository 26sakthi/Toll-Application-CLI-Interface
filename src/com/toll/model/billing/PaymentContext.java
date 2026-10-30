package com.toll.model.billing;

import com.toll.model.account.FASTagAccount;

/**
 * Context object holding all parameters required for polymorphic toll payment execution.
 */
public class PaymentContext {
    private final double amount;
    private final FASTagAccount account; // Used by FASTagPayment
    private final String operatorName;   // Used by CashPayment
    private final String description;

    public PaymentContext(double amount, FASTagAccount account, String operatorName, String description) {
        this.amount = amount;
        this.account = account;
        this.operatorName = operatorName;
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public FASTagAccount getAccount() {
        return account;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getDescription() {
        return description;
    }
}
