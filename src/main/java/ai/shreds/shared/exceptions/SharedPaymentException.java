package ai.shreds.shared.exceptions;

import lombok.Getter;

/**
 * Exception thrown when payment-related operations fail.
 * Contains payment error details and rider information.
 */
@Getter
public class SharedPaymentException extends RuntimeException {

    /** Specific payment error code or type. */
    private final String paymentError;

    /** ID of the rider associated with the payment failure. */
    private final String riderId;

    /**
     * Creates a payment exception with error details.
     *
     * @param message general error message
     * @param paymentError specific payment error code or description
     * @param riderId ID of the rider associated with the failure
     */
    public SharedPaymentException(String message, String paymentError, String riderId) {
        super(message);
        this.paymentError = paymentError;
        this.riderId = riderId;
    }

    /**
     * Gets the payment error code or description.
     *
     * @return payment error
     */
    public String getPaymentError() {
        return paymentError;
    }

    /**
     * Gets the rider ID associated with the payment failure.
     *
     * @return rider ID
     */
    public String getRiderId() {
        return riderId;
    }
}