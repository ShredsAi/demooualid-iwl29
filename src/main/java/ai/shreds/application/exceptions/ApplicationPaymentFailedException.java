package ai.shreds.application.exceptions;

import ai.shreds.shared.dtos.SharedMoneyDTO;

public class ApplicationPaymentFailedException extends ApplicationServiceException {

    private final String riderId;
    private final SharedMoneyDTO amount;
    private final String errorCode;

    public ApplicationPaymentFailedException(String riderId, SharedMoneyDTO amount, String errorCode) {
        super("Payment failed for rider " + riderId + " with error code " + errorCode, null);
        this.riderId = riderId;
        this.amount = amount;
        this.errorCode = errorCode;
    }

    public String getRiderId() {
        return riderId;
    }

    public SharedMoneyDTO getAmount() {
        return amount;
    }

    public String getErrorCode() {
        return errorCode;
    }
}