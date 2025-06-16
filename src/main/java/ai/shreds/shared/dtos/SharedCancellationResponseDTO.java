package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationCancellationResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after a trip cancellation. Contains the cancellation status, timestamp,
 * and refund information if applicable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedCancellationResponseDTO {

    /** The ID of the cancelled trip. */
    private String tripId;

    /** Updated status after cancellation (typically CANCELLED or MATCH_FAILED). */
    private String status;

    /** ISO-8601 timestamp when the cancellation was processed. */
    private String cancelledAt;

    /** Amount being refunded to the rider, if any. */
    private SharedMoneyDTO refundAmount;

    /** Whether the refund has been successfully processed. */
    private Boolean refundProcessed;

    /**
     * Factory method to convert from application-layer cancellation result to this shared response DTO.
     *
     * @param dto the application-layer cancellation result DTO
     * @return populated SharedCancellationResponseDTO ready for JSON serialisation
     */
    public static SharedCancellationResponseDTO fromApplicationDTO(ApplicationCancellationResultDTO dto) {
        if (dto == null) {
            return null;
        }
        return SharedCancellationResponseDTO.builder()
                .tripId(dto.getTripId())
                .status(dto.getStatus())
                .cancelledAt(dto.getCancelledAt())
                .refundAmount(dto.getRefundAmount())
                .refundProcessed(dto.getRefundProcessed())
                .build();
    }
}
