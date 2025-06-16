package ai.shreds.shared.dtos;

import ai.shreds.domain.value_objects.DomainLocationValue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Geographical location value shared across layers. The DTO can be safely serialised to JSON while
 * still being convertible to the domain-value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedLocationDTO {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private BigDecimal longitude;

    @NotBlank(message = "Address is required")
    private String address;

    private String city;

    private String postalCode;

    /* ===================== Domain Conversions ===================== */

    /**
     * Builds a DomainLocationValue from this DTO so that the Domain layer can work exclusively with its
     * own value objects.
     */
    public DomainLocationValue toDomainValue() {
        return new DomainLocationValue(latitude, longitude, address, city, postalCode);
    }

    /**
     * Factory method to reverse-map a domain value into a shared DTO for outbound payloads.
     */
    public static SharedLocationDTO fromDomainValue(DomainLocationValue value) {
        if (value == null) {
            return null;
        }
        return SharedLocationDTO.builder()
                .latitude(value.getLatitude())
                .longitude(value.getLongitude())
                .address(value.getAddress())
                .city(value.getCity())
                .postalCode(value.getPostalCode())
                .build();
    }
}
