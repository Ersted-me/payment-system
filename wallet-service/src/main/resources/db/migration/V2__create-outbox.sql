CREATE TABLE wallet.outbox
(
    id           UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    processed_at TIMESTAMP,
    topic        VARCHAR(255) NOT NULL,
    key          VARCHAR(255),
    payload      TEXT         NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status ON wallet.outbox (status);
