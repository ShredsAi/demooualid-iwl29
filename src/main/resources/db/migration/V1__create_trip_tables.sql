-- Trip Request and Matching Coordination Shred
-- Database Migration V1 - Create core tables

-- Create trip table
CREATE TABLE trip (
    trip_id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'MATCHING', 'MATCHED', 'CANCELLED', 'MATCH_FAILED')),
    rider_id UUID NOT NULL,
    driver_id UUID,
    pickup_latitude DECIMAL(9,6) NOT NULL,
    pickup_longitude DECIMAL(9,6) NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    pickup_city VARCHAR(100),
    pickup_postal_code VARCHAR(20),
    dropoff_latitude DECIMAL(9,6) NOT NULL,
    dropoff_longitude DECIMAL(9,6) NOT NULL,
    dropoff_address VARCHAR(255) NOT NULL,
    dropoff_city VARCHAR(100),
    dropoff_postal_code VARCHAR(20),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_for TIMESTAMP WITH TIME ZONE,
    estimated_fare_amount NUMERIC(10,2) NOT NULL,
    estimated_fare_currency CHAR(3) NOT NULL DEFAULT 'USD',
    final_fare_amount NUMERIC(10,2),
    final_fare_currency CHAR(3),
    cancellation_reason VARCHAR(50),
    cancelled_by VARCHAR(100),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    refund_amount NUMERIC(10,2),
    metadata JSONB,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create trip_timeline table
CREATE TABLE trip_timeline (
    timeline_id UUID PRIMARY KEY,
    trip_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (trip_id) REFERENCES trip(trip_id) ON DELETE CASCADE
);

-- Create trip_event table
CREATE TABLE trip_event (
    event_id BIGSERIAL PRIMARY KEY,
    timeline_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN (
        'REQUESTED', 'FARE_ESTIMATED', 'MATCHING_STARTED', 'DRIVER_MATCHED', 'MATCH_TIMEOUT', 'CANCELLED'
    )),
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB,
    source VARCHAR(100) NOT NULL DEFAULT 'trip-request-matching-shred',
    FOREIGN KEY (timeline_id) REFERENCES trip_timeline(timeline_id) ON DELETE CASCADE
);

-- Create outbox_event table for reliable message publishing
CREATE TABLE outbox_event (
    outbox_id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT
);

-- Create indexes for performance optimization

-- Trip table indexes
CREATE INDEX idx_trip_status ON trip(status);
CREATE INDEX idx_trip_rider_status ON trip(rider_id, status);
CREATE INDEX idx_trip_driver ON trip(driver_id) WHERE driver_id IS NOT NULL;
CREATE INDEX idx_trip_requested_at ON trip(requested_at);
CREATE INDEX idx_trip_scheduled_for ON trip(scheduled_for) WHERE scheduled_for IS NOT NULL;
CREATE INDEX idx_trip_created_at ON trip(created_at);

-- Trip timeline indexes
CREATE INDEX idx_trip_timeline_trip_id ON trip_timeline(trip_id);

-- Trip event indexes
CREATE INDEX idx_trip_event_timeline ON trip_event(timeline_id);
CREATE INDEX idx_trip_event_type ON trip_event(event_type);
CREATE INDEX idx_trip_event_timestamp ON trip_event(event_timestamp);
CREATE INDEX idx_trip_event_timeline_timestamp ON trip_event(timeline_id, event_timestamp);

-- Outbox event indexes
CREATE INDEX idx_outbox_published ON outbox_event(published) WHERE published = false;
CREATE INDEX idx_outbox_aggregate ON outbox_event(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_created_at ON outbox_event(created_at);

-- Create trigger function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for trip table
CREATE TRIGGER update_trip_updated_at 
    BEFORE UPDATE ON trip 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE trip IS 'Core trip aggregate storing trip lifecycle from request to completion';
COMMENT ON TABLE trip_timeline IS 'One-to-one relationship with trip for event sourcing';
COMMENT ON TABLE trip_event IS 'Event store for trip domain events';
COMMENT ON TABLE outbox_event IS 'Transactional outbox pattern for reliable message publishing';

COMMENT ON COLUMN trip.trip_id IS 'Unique identifier for the trip';
COMMENT ON COLUMN trip.status IS 'Current status of the trip in the state machine';
COMMENT ON COLUMN trip.rider_id IS 'Logical foreign key to Rider Service';
COMMENT ON COLUMN trip.driver_id IS 'Logical foreign key to Driver Service, populated when matched';
COMMENT ON COLUMN trip.version IS 'Optimistic locking version for JPA @Version';
COMMENT ON COLUMN trip.metadata IS 'Arbitrary key-value pairs for trip-specific requirements';

COMMENT ON COLUMN outbox_event.published IS 'Flag indicating if the event has been published to Kafka';
COMMENT ON COLUMN outbox_event.retry_count IS 'Number of publication attempts for failed events';
COMMENT ON COLUMN outbox_event.last_error IS 'Error message from last failed publication attempt';
