 CREATE TABLE IF NOT EXISTS email_verification (
      id          BIGSERIAL PRIMARY KEY,
      email       TEXT      NOT NULL,
      code        TEXT      NOT NULL,
      purpose     TEXT      NOT NULL,
      payload     TEXT      NOT NULL,
      attempts    INTEGER   NOT NULL DEFAULT 0,
      created_at  TIMESTAMP NOT NULL,
      expires_at  TIMESTAMP NOT NULL
  );

  CREATE INDEX IF NOT EXISTS idx_email_verification_lookup
      ON email_verification (email, purpose);

  CREATE INDEX IF NOT EXISTS idx_email_verification_expires_at
      ON email_verification (expires_at);