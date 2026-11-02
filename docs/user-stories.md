# Task Manager: User Stories

## US-01: Create a Task

As a user, I want to create a task with a title, description, priority, and due date so I can organize my work.

### Acceptance Criteria
- Title is required and limited to 100 characters.
- Description is optional.
- Priority must be Low, Medium, or High.
- Due date cannot be in the past.
- After successful creation, a confirmation is displayed and the task appears in the task list.

## US-02: View and Filter Tasks

As a user, I want to view all tasks and filter them by status and priority so I can focus on relevant work.

### Acceptance Criteria
- Display each task's title, priority, status, and due date.
- Support status filters: To Do, In Progress, and Done.
- Support priority filters: Low, Medium, and High.
- Allow status and priority filters to be combined.
- Show a clear empty state when no tasks match the selected filters.

## US-03: Update Task Status

As a user, I want to update a task's status so I can track its progress.

### Acceptance Criteria
- Allow status changes between To Do, In Progress, and Done.
- Save the updated status in the database.
- Reflect status changes in the UI without a full-page reload.
- Preserve the updated status after a page refresh.

## US-04: Edit and Delete a Task

As a user, I want to edit or delete tasks so I can keep my task list accurate.

### Acceptance Criteria
- Allow users to edit the title, description, priority, and due date.
- Apply the same validation rules used when creating a task.
- Require confirmation before deleting a task.
- Canceling deletion leaves the task unchanged.
- Confirming deletion removes the task from both the list and database.

## US-05: Dashboard and Overdue Tasks

As a user, I want a task summary and visibility into overdue items so I can prioritize my work.

### Acceptance Criteria
- Display counts for total tasks, To Do, In Progress, and Done.
- Mark as overdue any task with a due date before today and a status other than Done.
- Update dashboard counts after task creation, status changes, and deletion.
- Make overdue tasks visually distinct.
- Show zero counts without errors when the task list is empty.

## Technical Constraints

- Mobile: Native (Android & iOS)
- Backend: REST API using Java Spring Boot or Node.js
- Database: PostgreSQL or SQLite
- Testing: Unit tests, API integration tests, and basic UI tests
- Delivery: README, setup instructions, and a working applications
- Scope: Single user, with no authentication or external integrations

## Stack Decisions

| Area | Decision |
|---|---|
| Android app | Native, Kotlin + Jetpack Compose, in `android/` |
| iOS app | Native, Swift + SwiftUI, in `ios/` |
| Backend | Node.js + TypeScript REST API, in `backend/` |
| Database | PostgreSQL |
| Local development | PostgreSQL 17 on the developer machine, database `taskmanager` |

## Deployment Requirement

- The apps must work when installed on a different device, not only on the developer machine.
- The backend API and the PostgreSQL database must be hosted online.
- The apps call the hosted backend over HTTPS. The apps never connect to the database directly.
- The backend URL must be configurable per environment (local and hosted).
- Open decision: which hosting provider to use for the backend and the database.
