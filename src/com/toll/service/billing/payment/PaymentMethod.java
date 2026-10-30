package com.toll.service.billing.payment;

import com.toll.model.billing.PaymentContext;
import com.toll.model.billing.PaymentResult;

/**
 * Strategy interface demonstrating Abstraction and Polymorphism for payment processing.
 * Both FASTag and Cash payments process toll payments polymorphically in their own way.
 */
public interface PaymentMethod {

    /**
     * Executes payment processing according to the specific payment instrument.
     */
    PaymentResult processPayment(PaymentContext context);
}
