CREATE SEQUENCE IF NOT EXISTS instructors_id_seq;

CREATE TABLE instructors (
    id BIGINT PRIMARY KEY DEFAULT nextval('instructors_id_seq'),
    user_id BIGINT UNIQUE,
    bio TEXT,
    specialization VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
