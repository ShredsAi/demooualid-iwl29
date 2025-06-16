package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import ai.shreds.shared.dtos.SharedLocationDTO;
import java.math.BigDecimal;
import java.util.Objects;

public class DomainLocationValue {
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String address;
    private final String city;
    private final String postalCode;

    public DomainLocationValue(BigDecimal latitude, BigDecimal longitude, String address, String city, String postalCode) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        validateCoordinates();
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void validateCoordinates() {
        if (latitude == null || longitude == null) {
            throw new DomainBusinessRuleViolationException("Coordinates cannot be null", null);
        }
        if (latitude.compareTo(new BigDecimal("90")) > 0 || latitude.compareTo(new BigDecimal("-90")) < 0) {
            throw new DomainBusinessRuleViolationException("Latitude must be between -90 and 90 degrees", null);
        }
        if (longitude.compareTo(new BigDecimal("180")) > 0 || longitude.compareTo(new BigDecimal("-180")) < 0) {
            throw new DomainBusinessRuleViolationException("Longitude must be between -180 and 180 degrees", null);
        }
        if (address == null || address.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("Address cannot be null or empty", null);
        }
        if (address.length() > 500) {
            throw new DomainBusinessRuleViolationException("Address cannot exceed 500 characters", null);
        }
        if (city == null || city.trim().isEmpty()) {
            throw new DomainBusinessRuleViolationException("City cannot be null or empty", null);
        }
        if (city.length() > 100) {
            throw new DomainBusinessRuleViolationException("City cannot exceed 100 characters", null);
        }
    }

    public double distanceTo(DomainLocationValue other) {
        final int R = 6371; // Earth's radius in kilometers

        double lat1 = latitude.doubleValue();
        double lon1 = longitude.doubleValue();
        double lat2 = other.getLatitude().doubleValue();
        double lon2 = other.getLongitude().doubleValue();

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in kilometers
    }

    /**
     * Converts this domain location value to a SharedLocationDTO for cross-layer communication.
     * 
     * @return SharedLocationDTO containing the location data
     */
    public SharedLocationDTO toSharedDTO() {
        return SharedLocationDTO.fromDomainValue(this);
    }

    /**
     * Factory method to create a DomainLocationValue from a SharedLocationDTO.
     * 
     * @param dto the SharedLocationDTO to convert
     * @return DomainLocationValue created from the DTO
     */
    public static DomainLocationValue fromSharedDTO(SharedLocationDTO dto) {
        if (dto == null) {
            return null;
        }
        return new DomainLocationValue(
            dto.getLatitude(),
            dto.getLongitude(),
            dto.getAddress(),
            dto.getCity(),
            dto.getPostalCode()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainLocationValue that = (DomainLocationValue) o;
        return Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude) &&
                Objects.equals(address, that.address) &&
                Objects.equals(city, that.city) &&
                Objects.equals(postalCode, that.postalCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, address, city, postalCode);
    }
}
