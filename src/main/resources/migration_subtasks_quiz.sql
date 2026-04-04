-- ============================================================
-- Migration: Add subtasks and quiz tables
-- Run this against your goalplanner database:
--   psql -U postgres -d goalplanner -f migration_subtasks_quiz.sql
-- ============================================================

-- ============================================================
-- SUBTASKS
-- ============================================================
CREATE TABLE IF NOT EXISTS subtasks (
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT      NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    description  TEXT        NOT NULL,
    order_index  INT         NOT NULL DEFAULT 1,
    completed    BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subtasks_task ON subtasks(task_id);
CREATE INDEX IF NOT EXISTS idx_subtasks_completed ON subtasks(task_id, completed);

-- ============================================================
-- QUIZZES
-- ============================================================
CREATE TABLE IF NOT EXISTS quizzes (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT    NOT NULL UNIQUE REFERENCES tasks(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quizzes_task ON quizzes(task_id);

-- ============================================================
-- QUIZ QUESTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS quiz_questions (
    id             BIGSERIAL PRIMARY KEY,
    quiz_id        BIGINT       NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question       TEXT         NOT NULL,
    option_a       TEXT         NOT NULL,
    option_b       TEXT         NOT NULL,
    option_c       TEXT         NOT NULL,
    option_d       TEXT         NOT NULL,
    correct_answer VARCHAR(1)   NOT NULL,
    explanation    TEXT,
    order_index    INT          NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_quiz_questions_quiz ON quiz_questions(quiz_id);
