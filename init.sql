-- Schema for Project Management System

-- Users
CREATE TABLE IF NOT EXISTS users (
    login VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Projects
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    manager_login VARCHAR(100) NOT NULL REFERENCES users(login)
);

-- Project members (role per project)
CREATE TABLE IF NOT EXISTS project_members (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_login VARCHAR(100) NOT NULL REFERENCES users(login),
    role VARCHAR(50) NOT NULL CHECK (role IN ('TEAM_LEADER', 'DEVELOPER', 'TESTER')),
    PRIMARY KEY (project_id, user_login)
);

-- Milestones
CREATE TABLE IF NOT EXISTS milestones (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACTIVE', 'CLOSED'))
);

-- Tickets
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    milestone_id UUID NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'DONE'))
);

-- Ticket assignees (many-to-many)
CREATE TABLE IF NOT EXISTS ticket_assignees (
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_login VARCHAR(100) NOT NULL REFERENCES users(login),
    PRIMARY KEY (ticket_id, user_login)
);

-- Bug reports
CREATE TABLE IF NOT EXISTS bug_reports (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    reporter_login VARCHAR(100) NOT NULL REFERENCES users(login),
    assignee_login VARCHAR(100) REFERENCES users(login),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'FIXED', 'TESTED', 'CLOSED'))
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_project_members_user ON project_members(user_login);
CREATE INDEX IF NOT EXISTS idx_milestones_project ON milestones(project_id);
CREATE INDEX IF NOT EXISTS idx_tickets_milestone ON tickets(milestone_id);
CREATE INDEX IF NOT EXISTS idx_tickets_project ON tickets(project_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignees_user ON ticket_assignees(user_login);
CREATE INDEX IF NOT EXISTS idx_bug_reports_project ON bug_reports(project_id);
CREATE INDEX IF NOT EXISTS idx_bug_reports_assignee ON bug_reports(assignee_login);


