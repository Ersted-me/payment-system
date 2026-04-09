CREATE TABLE wallet.inbox
(
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT now(),
    event_type   VARCHAR(255) NOT NULL
);
