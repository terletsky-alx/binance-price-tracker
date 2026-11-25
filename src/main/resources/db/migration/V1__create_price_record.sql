CREATE TABLE price_record (
    id         BIGSERIAL PRIMARY KEY,
    symbol     VARCHAR(20)    NOT NULL,
    price      NUMERIC(30, 8) NOT NULL,
    close_time TIMESTAMPTZ    NOT NULL,
    fetched_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_price_record_symbol_close_time UNIQUE (symbol, close_time)
);

CREATE INDEX idx_price_record_symbol_fetched_at ON price_record (symbol, fetched_at);
