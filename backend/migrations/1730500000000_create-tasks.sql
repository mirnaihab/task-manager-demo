-- Up Migration
CREATE TABLE tasks (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title       VARCHAR(100) NOT NULL CHECK (length(btrim(title)) > 0),
  description TEXT NULL,
  priority    TEXT NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH')),
  status      TEXT NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO','IN_PROGRESS','DONE')),
  due_date    DATE NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX tasks_status_priority_idx ON tasks (status, priority);

-- Down Migration
DROP INDEX tasks_status_priority_idx;
DROP TABLE tasks;
