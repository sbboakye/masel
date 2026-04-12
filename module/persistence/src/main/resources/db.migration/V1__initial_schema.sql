-- Enums
CREATE TYPE challenge_status AS ENUM ('draft', 'validated', 'active', 'archived');
CREATE TYPE difficulty AS ENUM ('easy', 'medium', 'hard');

-- Shared trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Challenge definitions
CREATE TABLE challenges (
    id               UUID PRIMARY KEY,
    title            VARCHAR NOT NULL,
    instructions     TEXT NOT NULL,
    status           challenge_status NOT NULL DEFAULT 'draft',
    expected_solution TEXT NOT NULL,
    output           JSONB,
    allotted_time    INT NOT NULL,
    difficulty       difficulty NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_challenges_status ON challenges (status);

-- Submission definitions
CREATE TABLE submissions (
    id                  UUID PRIMARY KEY,
    challenge_id        UUID NOT NULL REFERENCES challenges(id),
    candidate_solution  TEXT NOT NULL,
    output              JSONB,
    score               INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_submissions_challenge_id ON submissions (challenge_id);