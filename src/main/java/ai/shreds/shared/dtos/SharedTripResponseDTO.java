package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationTripDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO sent back to clients after trip creation or status queries. This represents the
 * authoritative state of a trip as known by this shred before handoff to Trip Execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedTripResponseDTO {

    /** Unique identifier of the trip. */
    private String tripId;

    /** Current status: REQUESTED, MATCHING, MATCHED, CANCELLED, MATCH_FAILED. */
    private String status;

    /** Estimated fare calculated during trip creation. */
    private SharedMoneyDTO estimatedFare;

    /** ISO-8601 timestamp when the trip was initially requested. */
    private String requestedAt;

    /**
     * Factory method to convert from application-layer DTO to this shared response DTO.
     * The Application layer uses this to build REST API responses.
     *
     * @param dto the application-layer trip DTO
     * @return populated SharedTripResponseDTO ready for JSON serialisation
     */
    public static SharedTripResponseDTO fromApplicationDTO(ApplicationTripDTO dto) {
        if (dto == null) {
            return null;
        }
        return SharedTripResponseDTO.builder()
                .tripId(dto.getTripId())
                .status(dto.getStatus())
                .estimatedFare(dto.getEstimatedFare())
                .requestedAt(dto.getRequestedAt())
                .build();
    }
}
