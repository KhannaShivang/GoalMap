-- ============================================================
-- AI Goal Planner - PostgreSQL Schema
-- Run this ONCE against your goalplanner database:
--   psql -U postgres -d goalplanner -f schema.sql
-- ============================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    current_skills   TEXT,
    experience_level VARCHAR(50)  NOT NULL DEFAULT 'BEGINNER',
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- GOALS
-- ============================================================
CREATE TABLE IF NOT EXISTS goals (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_description       TEXT        NOT NULL,
    target_duration_months INT         NOT NULL DEFAULT 12,
    status                 VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SKILLS
-- ============================================================
CREATE TABLE IF NOT EXISTS skills (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category    VARCHAR(80)
);

-- ============================================================
-- SKILL DEPENDENCIES  (DAG edges)
-- ============================================================
CREATE TABLE IF NOT EXISTS skill_dependencies (
    id              BIGSERIAL PRIMARY KEY,
    skill_id        BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    depends_on_id   BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE (skill_id, depends_on_id)
);

-- ============================================================
-- ROADMAPS
-- ============================================================
CREATE TABLE IF NOT EXISTS roadmaps (
    id         BIGSERIAL PRIMARY KEY,
    goal_id    BIGINT    NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TASKS
-- ============================================================
CREATE TABLE IF NOT EXISTS tasks (
    id          BIGSERIAL PRIMARY KEY,
    roadmap_id  BIGINT      NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
    skill_id    BIGINT               REFERENCES skills(id)   ON DELETE SET NULL,
    description TEXT        NOT NULL,
    priority    INT         NOT NULL DEFAULT 1,
    completed   BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- RESOURCES  (with pgvector embedding)
-- ============================================================
CREATE TABLE IF NOT EXISTS resources (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    link       VARCHAR(500),
    type       VARCHAR(50)  NOT NULL DEFAULT 'ARTICLE',
    skill_id   BIGINT               REFERENCES skills(id) ON DELETE SET NULL,
    difficulty VARCHAR(30)  NOT NULL DEFAULT 'BEGINNER',
    embedding  vector(768),          -- nomic-embed-text dimensions
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Spring AI vector store table (used by VectorStore bean)
-- ============================================================
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content   TEXT,
    metadata  JSONB,
    embedding vector(768)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_goals_user        ON goals(user_id);
CREATE INDEX IF NOT EXISTS idx_roadmaps_user     ON roadmaps(user_id);
CREATE INDEX IF NOT EXISTS idx_roadmaps_goal     ON roadmaps(goal_id);
CREATE INDEX IF NOT EXISTS idx_tasks_roadmap     ON tasks(roadmap_id);
CREATE INDEX IF NOT EXISTS idx_tasks_completed   ON tasks(completed);
CREATE INDEX IF NOT EXISTS idx_resources_skill   ON resources(skill_id);
CREATE INDEX IF NOT EXISTS idx_resources_embed   ON resources USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- ============================================================
-- SEED DATA — skills + dependency graph
-- ============================================================
INSERT INTO skills (name, description, category) VALUES
('Java Basics',        'Core Java syntax, OOP, collections',         'Programming'),
('Data Structures',    'Arrays, lists, trees, graphs',               'CS Fundamentals'),
('Algorithms',         'Sorting, searching, complexity analysis',    'CS Fundamentals'),
('Spring Boot',        'Spring Boot framework fundamentals',         'Backend'),
('REST APIs',          'HTTP, REST principles, status codes',        'Backend'),
('Spring Data JPA',    'ORM, repositories, JPQL',                   'Backend'),
('SQL & PostgreSQL',   'Relational databases, queries, indexes',     'Database'),
('Docker',             'Containerisation, Dockerfile, Compose',      'DevOps'),
('Git',                'Version control, branching strategies',      'Tools'),
('System Design',      'Scalability, microservices, architecture',   'Advanced'),
('Testing (JUnit)',    'Unit tests, mocks, TDD basics',              'Quality'),
('Spring Security',   'Auth, JWT, OAuth2 basics',                   'Security'),
('Message Queues',     'Kafka/RabbitMQ basics',                      'Advanced'),
('CI/CD',              'GitHub Actions, pipelines',                  'DevOps'),
('Cloud Basics',       'AWS/GCP fundamentals, deployment',           'DevOps')
ON CONFLICT (name) DO NOTHING;

-- Skill dependency edges
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Spring Boot'      AND d.name='Java Basics'       ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='REST APIs'        AND d.name='Spring Boot'       ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Spring Data JPA'  AND d.name='Spring Boot'       ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Spring Data JPA'  AND d.name='SQL & PostgreSQL'  ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='System Design'    AND d.name='REST APIs'         ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='System Design'    AND d.name='Spring Data JPA'   ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Spring Security'  AND d.name='Spring Boot'       ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Message Queues'   AND d.name='System Design'     ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='CI/CD'            AND d.name='Docker'            ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Cloud Basics'     AND d.name='Docker'            ON CONFLICT DO NOTHING;
INSERT INTO skill_dependencies (skill_id, depends_on_id)
SELECT s.id, d.id FROM skills s, skills d WHERE s.name='Algorithms'       AND d.name='Data Structures'   ON CONFLICT DO NOTHING;

-- Sample resources (embeddings left NULL — populated at runtime via ResourceRecommendationService)
INSERT INTO resources (title, link, type, skill_id, difficulty) VALUES
('Spring Boot Official Docs',             'https://spring.io/projects/spring-boot',            'DOCUMENTATION', (SELECT id FROM skills WHERE name='Spring Boot'),     'BEGINNER'),
('Baeldung Spring Boot Tutorial',         'https://www.baeldung.com/spring-boot',              'ARTICLE',       (SELECT id FROM skills WHERE name='Spring Boot'),     'BEGINNER'),
('Spring Data JPA Guide',                 'https://spring.io/projects/spring-data-jpa',        'DOCUMENTATION', (SELECT id FROM skills WHERE name='Spring Data JPA'), 'INTERMEDIATE'),
('REST API Design Best Practices',        'https://restfulapi.net/',                           'ARTICLE',       (SELECT id FROM skills WHERE name='REST APIs'),       'BEGINNER'),
('Docker Getting Started',                'https://docs.docker.com/get-started/',              'DOCUMENTATION', (SELECT id FROM skills WHERE name='Docker'),          'BEGINNER'),
('System Design Primer (GitHub)',         'https://github.com/donnemartin/system-design-primer','ARTICLE',      (SELECT id FROM skills WHERE name='System Design'),   'ADVANCED'),
('PostgreSQL Tutorial',                   'https://www.postgresqltutorial.com/',               'ARTICLE',       (SELECT id FROM skills WHERE name='SQL & PostgreSQL'),'BEGINNER'),
('Java Brains Spring Boot Course',        'https://javabrains.io/courses/springboot_quickstart','COURSE',       (SELECT id FROM skills WHERE name='Spring Boot'),     'BEGINNER'),
('Testing Spring Boot Apps - Baeldung',   'https://www.baeldung.com/spring-boot-testing',      'ARTICLE',       (SELECT id FROM skills WHERE name='Testing (JUnit)'), 'INTERMEDIATE'),
('GitHub Actions for Java',               'https://docs.github.com/en/actions',               'DOCUMENTATION', (SELECT id FROM skills WHERE name='CI/CD'),           'INTERMEDIATE')
ON CONFLICT DO NOTHING;
