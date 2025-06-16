package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainTripEntity;
import ai.shreds.domain.enums.DomainTripStatusEnum;
import ai.shreds.domain.ports.DomainTripRepositoryOutputPort;
import ai.shreds.infrastructure.exceptions.InfrastructureRepositoryException;
import ai.shreds.infrastructure.mappers.InfrastructureTripEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InfrastructureTripRepositoryImpl implements DomainTripRepositoryOutputPort {

    private final InfrastructureTripJpaRepository tripJpaRepository;
    private final InfrastructureTripEntityMapper entityMapper;

    @Override
    public DomainTripEntity save(DomainTripEntity trip) {
        try {
            log.debug("Saving trip entity with ID: {}", trip.getId().getValue());
            var jpaEntity = entityMapper.toJpaEntity(trip);
            var savedJpaEntity = tripJpaRepository.save(jpaEntity);
            var savedDomainEntity = entityMapper.toDomainEntity(savedJpaEntity);
            log.debug("Successfully saved trip entity with ID: {}", savedDomainEntity.getId().getValue());
            return savedDomainEntity;
        } catch (Exception e) {
            log.error("Failed to save trip entity with ID: {}", trip.getId().getValue(), e);
            throw new InfrastructureRepositoryException("save", "Trip", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DomainTripEntity> findById(String tripId) {
        try {
            log.debug("Finding trip entity by ID: {}", tripId);
            UUID uuid = UUID.fromString(tripId);
            Optional<DomainTripEntity> result = tripJpaRepository.findById(uuid)
                    .map(entityMapper::toDomainEntity);
            log.debug("Trip entity with ID {} {}", tripId, result.isPresent() ? "found" : "not found");
            return result;
        } catch (Exception e) {
            log.error("Failed to find trip entity by ID: {}", tripId, e);
            throw new InfrastructureRepositoryException("findById", "Trip", e);
        }
    }

    @Override
    public DomainTripEntity update(DomainTripEntity trip) {
        try {
            log.debug("Updating trip entity with ID: {}", trip.getId().getValue());
            
            // Verify entity exists before update
            if (!tripJpaRepository.existsById(UUID.fromString(trip.getId().getValue()))) {
                throw new InfrastructureRepositoryException("update", "Trip", 
                    new IllegalArgumentException("Trip not found for update: " + trip.getId().getValue()));
            }
            
            var jpaEntity = entityMapper.toJpaEntity(trip);
            var updatedJpaEntity = tripJpaRepository.save(jpaEntity);
            var updatedDomainEntity = entityMapper.toDomainEntity(updatedJpaEntity);
            log.debug("Successfully updated trip entity with ID: {}", updatedDomainEntity.getId().getValue());
            return updatedDomainEntity;
        } catch (Exception e) {
            log.error("Failed to update trip entity with ID: {}", trip.getId().getValue(), e);
            throw new InfrastructureRepositoryException("update", "Trip", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainTripEntity> findByStatus(DomainTripStatusEnum status) {
        try {
            log.debug("Finding trip entities by status: {}", status.name());
            List<DomainTripEntity> result = tripJpaRepository.findByStatus(status.name())
                    .stream()
                    .map(entityMapper::toDomainEntity)
                    .collect(Collectors.toList());
            log.debug("Found {} trip entities with status: {}", result.size(), status.name());
            return result;
        } catch (Exception e) {
            log.error("Failed to find trip entities by status: {}", status.name(), e);
            throw new InfrastructureRepositoryException("findByStatus", "Trip", e);
        }
    }
}