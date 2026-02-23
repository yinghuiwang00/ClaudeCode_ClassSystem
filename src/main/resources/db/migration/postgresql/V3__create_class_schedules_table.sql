CREATE SEQUENCE IF NOT EXISTS class_schedules_id_seq;

CREATE TABLE class_schedules (
    id BIGINT PRIMARY KEY DEFAULT nextval('class_schedules_id_seq'),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    instructor_id BIGINT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    current_bookings INT DEFAULT 0 CHECK (current_bookings >= 0),
    location VARCHAR(200),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE SET NULL,
    CONSTRAINT check_time CHECK (end_time > start_time),
    CONSTRAINT check_capacity CHECK (current_bookings <= capacity)
);

CREATE INDEX idx_class_start_time ON class_schedules(start_time);
CREATE INDEX idx_class_status ON class_schedules(status);
CREATE INDEX idx_class_instructor ON class_schedules(instructor_id);
