# Figma User Story Slice Map

Source documents:

- `spec.md`
- `user-story-priority-and-blockers.md`
- Frontend handoff: `PROJECT_HANDOFF.md`
- Frontend Figma update prompt: `src/imports/pasted_text/supavisor-figma-update.md`

This map keeps Figma and frontend prototype work sliced by the same user-story headings that drive the GitHub Project. Each implementation branch should point at one slice group, close the issue numbers it implements, and update the PR column before merge.

## Slice Rules

- A slice must be tied to at least one numbered user story from `spec.md`.
- Branches should use `codex/issue-<issue-number>-<short-slice-name>` when one issue owns the slice.
- Branches covering several tightly coupled stories should use `codex/issues-<numbers>-<short-slice-name>`.
- The pull request body must list the user stories and Figma surface it implements.
- Prototype-only Figma/UI work should say what still needs backend integration.
- P0 and P1 slices should be completed before P2 slices unless the team explicitly pulls a P2 issue forward.

## Figma Surfaces

| Figma surface | User stories | Sprint | Priority | Intended branch | PR tracking |
| --- | --- | --- | --- | --- | --- |
| Login landing, admin login, employee login | 1, 2 | Sprint 0 | P0 | `codex/issues-1-2-auth-login` | PR required when the connected login UI changes |
| Backend-driven session validation and refresh UX | 65 | Sprint 0 | P0 | `codex/issue-65-session-refresh` | PR required when refresh/redirect behavior changes |
| User-facing API error states | 64 | Sprint 0 | P0 | `codex/issue-64-api-error-ui` | PR required when validation or authorization messaging changes |
| Admin role and permission administration | 4, 5, 6, 7 | Sprint 0 | P0 | `codex/issues-4-5-6-7-roles-permissions` | PR required before expanding Admin Hub role controls |
| Admin Hub shell, sidebar behavior, personal settings placement | 3, 4, 5 | Sprint 1 | P1 | `codex/issues-3-4-5-admin-hub-shell` | PR required when Admin Hub navigation changes |
| Employee management in Admin Hub | 3, 62, 63 | Sprint 1 | P1 | `codex/issues-3-62-63-employee-lifecycle` | PR required when local employee storage is replaced by backend APIs |
| Employee categories and qualifications | 8, 9, 10 | Sprint 1 | P1 | `codex/issues-8-9-10-categories-qualifications` | PR required when category/qualification UI becomes connected |
| Assignment type administration | 11 | Sprint 1 | P1 | `codex/issue-11-assignment-types` | PR required when assignment type UI/API is added |
| Project creation and lifecycle | 12, 13 | Sprint 2 | P1 | `codex/issues-12-13-project-lifecycle` | PR required when project screens become connected |
| Task creation, details, requirements, responsible person | 14, 15, 16, 17 | Sprint 2 | P1 | `codex/issues-14-15-16-17-task-details` | PR required when task detail screens are added |
| Assignment creation, scheduling, and delegation | 18, 19, 20, 21 | Sprint 2 | P1 | `codex/issues-18-19-20-21-assignment-scheduling` | PR required when assignment creation is connected |
| Employee calendar views | 26, 27 | Sprint 3 | P1 | `codex/issues-26-27-employee-calendar` | PR required when week/day/month behavior changes |
| Assignment detail for employees | 29 | Sprint 3 | P1 | `codex/issue-29-assignment-detail` | PR required when assignment detail is connected |
| Check-in, check-out, and server timestamp presentation | 37, 38, 39 | Sprint 3 | P1 | `codex/issues-37-38-39-attendance` | PR required when attendance actions are connected |
| Overlap, eligibility, override, and stale schedule conflict states | 22, 23, 24, 25, 67 | Sprint 4 | P2 | `codex/issues-22-23-24-25-67-schedule-exceptions` | PR required when warning/conflict UI is implemented |
| Acknowledge, decline, auto-acceptance, and response history | 32, 33, 34, 35, 36 | Sprint 4 | P2 | `codex/issues-32-33-34-35-36-assignment-response` | PR required when employee response workflow is added |
| Category colleague schedule visibility and privacy filtering | 30, 31 | Sprint 4 | P2 | `codex/issues-30-31-category-schedule-privacy` | PR required when category schedule views are implemented |
| Assignment state model and permitted progress updates | 41, 42, 43 | Sprint 4 | P2 | `codex/issues-41-42-43-assignment-states` | PR required when progress/status controls are connected |
| Attendance corrections and user-facing history | 40, 60, 61 | Sprint 5 | P2 | `codex/issues-40-60-61-attendance-history` | PR required when correction/history UI is added |
| Custom task and assignment statuses | 44, 45, 46, 47 | Sprint 5 | P2 | `codex/issues-44-45-46-47-custom-statuses` | PR required when configurable status controls are added |
| Resource catalogue and assignment resource requirements | 48, 49 | Sprint 6 | P2 | `codex/issues-48-49-resources` | PR required when resource screens become connected |
| Admin overview at scale, pagination/filtering, soft deletion | 59, 66, 68 | Sprint 6 | P3 | `codex/issues-59-66-68-admin-scale` | PR required when large-list controls are implemented |
| Advanced schedule filtering and calendar export | 28, 69 | Sprint 6 | P3 | `codex/issues-28-69-schedule-filter-export` | PR required when filters/export are implemented |
| Expense templates, scoped expenses, copying, totals, states, employee visibility | 50, 51, 52, 53, 54, 55, 56, 57, 58 | Sprint 6 | P3 | `codex/issues-50-58-expenses` | PR required when expense surfaces are designed or connected |
| CI, Docker Compose, and Hibernate production validation surfaces | 70, 71, 72 | Sprint 0 | P0 | `codex/issues-70-71-72-platform-checks` | PR required when developer/platform docs or checks change |
| Board color customization | 73 | Later enhancement | P4 | `codex/issue-73-board-colors` | PR required when color customization enters scope |

## Current Figma-Prototype Status

| Current area | Status | Connected stories | Next branch |
| --- | --- | --- | --- |
| Admin and employee login presentation | Backend authentication is connected; form choice affects presentation only. | 1, 2 | `codex/issues-1-2-auth-login` if login UI changes again |
| Session storage and refresh-token handling | Frontend stores the JWT and replaces it from `X-Refresh-Token`; redirect/error states still need broader workflow coverage. | 65 | `codex/issue-65-session-refresh` |
| Admin Hub | Visual shell exists with Employee Management, Roles & Permissions, Locations & Venues, and Appearance; most data is local-only. | 3, 4, 5, 8, 9, 10 | `codex/issues-3-62-63-employee-lifecycle` first |
| Sidebar collapse and personal settings placement | Prototype behavior exists and should remain style-preserving. | 3, 64 | Match the branch of the functional change that touches it |
| Employee workspace | Overview, Schedule, and Settings exist with responsive layouts; scheduling data is placeholder content. | 26, 27, 29, 37, 38, 39 | `codex/issues-26-27-employee-calendar` |
| Schedule, assignments, locations, availability, time off, reports | Navigation exists, but domain data is mostly placeholder/prototype. | 18-31, 37-49, 59, 66, 69 | Follow the sprint slice table |
| Expenses | Not yet a connected first-release surface. | 50-58 | `codex/issues-50-58-expenses` after core scheduling |

## PR Checklist For Figma Slices

- PR title names the user-story slice and issue numbers.
- PR body includes `Closes #<issue>` for every completed issue.
- PR body states the Figma surface changed.
- PR body states whether the slice is prototype-only or backend-connected.
- PR body lists backend endpoints consumed or explicitly says none were added.
- Screens affected by role visibility are checked as both Admin and Employee.
- Responsive behavior is checked for desktop, tablet, and phone when layout changes.
