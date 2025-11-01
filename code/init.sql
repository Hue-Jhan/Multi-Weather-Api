CREATE TABLE IF NOT EXISTS request_logs (
    id SERIAL PRIMARY KEY,
    ip VARCHAR(45),
    city VARCHAR(100),
    endpoint VARCHAR(255),
    status_code INT,
    timestamp TIMESTAMP
);