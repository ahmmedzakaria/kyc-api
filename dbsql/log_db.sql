CREATE TABLE api_audit_log (
                               id SERIAL PRIMARY KEY,
                               username VARCHAR(255),
                               http_method VARCHAR(10),
                               request_uri TEXT,
                               client_ip VARCHAR(50),
                               timestamp TIMESTAMP,
                               request_body TEXT,
                               response_body TEXT
);