package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.dtos.SharedPaymentAuthorizationDTO;

/**
 * Output port for payment service operations.
 * This port defines the contract for payment authorization and refund operations.
 */
public interface ApplicationPaymentServiceOutputPort {
    
    /**
     * Pre-authorizes payment for a trip.
     * 
     * @param riderId The rider ID
     * @param amount The amount to authorize
     * @param tripCorrelationId The trip correlation ID
     * @return SharedPaymentAuthorizationDTO containing authorization details
     * @throws ai.shreds.shared.exceptions.SharedPaymentException if payment authorization fails
     */
    SharedPaymentAuthorizationDTO preAuthorize(String riderId, 
                                               SharedMoneyDTO amount, 
                                               String tripCorrelationId);
    
    /**
     * Refunds a pre-authorized payment.
     * 
     * @param authorizationId The authorization ID to refund
     * @return true if refund was successful, false otherwise
     * @throws ai.shreds.shared.exceptions.SharedPaymentException if refund fails
     */
    Boolean refund(String authorizationId);
}