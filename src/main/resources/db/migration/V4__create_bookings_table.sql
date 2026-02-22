CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    class_schedule_id BIGINT NOT NULL,
    booking_status VARCHAR(20) DEFAULT 'CONFIRMED',
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancellation_date TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (class_schedule_id) REFERENCES class_schedules(id) ON DELETE CASCADE,
    CONSTRAINT unique_booking UNIQUE (user_id, class_schedule_id)
);

CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_booking_class ON bookings(class_schedule_id);
CREATE INDEX idx_booking_status ON bookings(booking_status);
