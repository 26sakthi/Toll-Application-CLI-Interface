package com.toll.service.billing.payment;

import com.toll.model.billing.PaymentContext;
import com.toll.model.billing.PaymentResult;
import com.toll.model.enums.PaymentType;

/**
 * Polymorphic implementation for Toll Exempt crossings (Emergency priority / Active Pass).
 */
public class ExemptPaymentMethod implements PaymentMethod {

    @Override
    public PaymentResult processPayment(PaymentContext context) {
        return new PaymentResult(
                true,
                PaymentType.EXEMPT,
                0.0,
                null,
                "Toll Exempt (Priority Bypass / Monthly Pass Active)"
        );
    }
}
