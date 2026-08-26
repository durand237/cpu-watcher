CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_process_metrics_process_name_trgm
    ON process_metrics USING GIN (process_name gin_trgm_ops);
