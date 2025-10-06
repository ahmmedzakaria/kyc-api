CREATE TABLE api_audit_log (
                           id SERIAL PRIMARY KEY,
                           username VARCHAR(255),
                           http_method VARCHAR(10),
                           request_uri TEXT,
                           client_ip VARCHAR(50),
                           timestamp TIMESTAMP
);

ALTER TABLE api_audit_log ADD COLUMN request_body TEXT;
ALTER TABLE api_audit_log ADD COLUMN response_body TEXT;

