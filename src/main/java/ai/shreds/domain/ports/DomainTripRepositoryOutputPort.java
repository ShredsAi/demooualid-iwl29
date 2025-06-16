package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import java.util.List;
import java.util.Optional;

public interface DomainTripRepositoryOutputPort {
    DomainTripEntity save(DomainTripEntity trip);
    Optional<DomainTripEntity> findById(String tripId);
    DomainTripEntity update(DomainTripEntity trip);
    List<DomainTripEntity> findByStatus(DomainTripStatusEnum status);
}
