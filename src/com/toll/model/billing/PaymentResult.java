package com.toll.model.billing;

import com.toll.model.enums.PaymentType;
import com.toll.model.enums.TransactionStatus;

/**
 * Result object returned by polymorphic PaymentMethod implementations.
 */
public class PaymentResult {
    private final boolean success;
    private final PaymentType paymentType;
    private final double amountPaid;
    private final String operatorName;
    private final String message;

    public PaymentResult(boolean success, PaymentType paymentType, double amountPaid, String operatorName, String message) {
        this.success = success;
        this.paymentType = paymentType;
        this.amountPaid = amountPaid;
        this.operatorName = operatorName;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getMessage() {
        return message;
    }
}
