package com.toll.service.billing.payment;

import com.toll.exception.AccountException;
import com.toll.model.account.FASTagAccount;
import com.toll.model.billing.PaymentContext;
import com.toll.model.billing.PaymentResult;
import com.toll.model.enums.PaymentType;

/**
 * Polymorphic implementation for electronic FASTag RFID wallet deductions.
 */
public class FASTagPaymentMethod implements PaymentMethod {

    @Override
    public PaymentResult processPayment(PaymentContext context) {
        FASTagAccount account = context.getAccount();
        if (account == null) {
            return new PaymentResult(false, PaymentType.FASTAG, 0.0, null, "No FASTag account provided.");
        }

        try {
            account.debit(context.getAmount());
            return new PaymentResult(true, PaymentType.FASTAG, context.getAmount(), null,
                    String.format("FASTag %s debited Rs. %.2f. Remaining Balance: Rs. %.2f",
                            account.getTagId(), context.getAmount(), account.getBalance()));
        } catch (AccountException e) {
            return new PaymentResult(false, PaymentType.FASTAG, 0.0, null, e.getMessage());
        }
    }
}
