package com.toll.service.billing.payment;

import com.toll.model.billing.PaymentContext;
import com.toll.model.billing.PaymentResult;
import com.toll.model.enums.PaymentType;

/**
 * Polymorphic implementation for manual cash toll payments collected by booth operators.
 */
public class CashPaymentMethod implements PaymentMethod {

    @Override
    public PaymentResult processPayment(PaymentContext context) {
        String operator = context.getOperatorName();
        if (operator == null || operator.trim().isEmpty()) {
            operator = "Duty Cashier / Lane Operator";
        }

        double amount = context.getAmount();
        return new PaymentResult(
                true,
                PaymentType.CASH,
                amount,
                operator,
                String.format("Cash toll payment of Rs. %.2f collected by Operator: %s", amount, operator)
        );
    }
}
