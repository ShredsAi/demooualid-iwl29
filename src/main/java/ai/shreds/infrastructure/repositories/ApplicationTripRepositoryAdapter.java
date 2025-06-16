package ai.shreds.infrastructure.repositories;

import ai.shreds.application.ports.ApplicationTripRepositoryOutputPort;
import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.ports.DomainTripRepositoryOutputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements ApplicationTripRepositoryOutputPort
 * and delegates to DomainTripRepositoryOutputPort
 */
@Component
@RequiredArgsConstructor
public class ApplicationTripRepositoryAdapter implements ApplicationTripRepositoryOutputPort {
    
    private final DomainTripRepositoryOutputPort domainTripRepository;
    
    @Override
    public DomainTripEntity save(DomainTripEntity trip) {
        return domainTripRepository.save(trip);
    }
    
    @Override
    public Optional<DomainTripEntity> findById(String tripId) {
        return domainTripRepository.findById(tripId);
    }
    
    @Override
    public DomainTripEntity update(DomainTripEntity trip) {
        return domainTripRepository.update(trip);
    }
}