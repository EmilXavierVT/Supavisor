# Project Management and Employee Scheduling

Status: ready-for-agent

## Problem Statement

The organization needs one system for planning and coordinating employee work across projects. Administrators currently need a reliable way to define employee categories and roles, organize projects and tasks, create detailed scheduled assignments, delegate work, and retain visibility into operational progress.

Employees need to know what they will do in the foreseeable future, when they will do it, which resources they need, and where the work belongs. They also need to see relevant schedules for colleagues in the same category and record when they start and finish an assignment. Administrators need complete access to project information, assignment history, expenses, statuses, and exceptions such as overlaps, declined assignments, missing acknowledgements, and check-in issues.

The system must support both a credible academic/demo delivery and a production-shaped foundation. It must provide authenticated access, role-based authorization, a React frontend, a Java/Javalin backend, a REST API, PostgreSQL persistence, Docker-based local deployment, and automated quality checks.

## Solution

Build a project management and employee scheduling application with a React frontend and Java/Javalin REST backend backed by PostgreSQL. Hibernate will provide entity mapping and persistence, with JPQL used for application queries. The backend will expose a versioned `/api/v1` API and remain the source of truth for authorization, scheduling rules, status transitions, timestamps, and audit history.

The application will model organizations, users, admin-defined roles and permissions, employee categories, projects, tasks, assignments, resources, expenses, custom statuses, notifications, check-in/check-out records, and audit events. A project contains tasks. A task describes work and has one optional responsible person. An assignment represents a scheduled allocation of a task to one employee and contains its own time, status, resources, and attendance data. A task may have multiple assignments, including assignments at different or overlapping times.

Administrators delegate assignments and can override warnings with a required reason. Employees can acknowledge or decline assignments before check-in. Silence becomes automatic acceptance at the assignment start time. Employees can check in and out of valid assignments, while the server records authoritative timestamps. Employees in the same category can see the schedule information needed for coordination without seeing private or sensitive data.

Expenses support reusable templates and editable entries connected to projects, tasks, or assignments. Administrators can create, edit, copy, approve, reject, and delete expenses. Resources remain separate from expenses and are informational in the first release.

## User Stories

1. As an administrator, I want to log in securely, so that only authorized users can access organizational data.
2. As an employee, I want to log in securely, so that I can access my schedule and assigned work.
3. As an administrator, I want to create, edit, deactivate, and manage users, so that the organization can keep its workforce current.
4. As an administrator, I want to create custom roles from a fixed permission catalogue, so that access rules match organizational responsibilities without arbitrary executable rules.
5. As an administrator, I want to assign roles to users, so that each user receives the correct capabilities.
6. As an administrator, I want role permission changes to apply immediately, so that revoked access does not remain active until a long-lived token expires.
7. As an administrator, I want the system to protect the last administrator account, so that the organization cannot lose administrative control.
8. As an administrator, I want to create and manage employee categories, so that employees can be grouped according to their work area.
9. As an administrator, I want to assign an employee a primary category, so that category-based schedule visibility is predictable.
10. As an administrator, I want to record additional employee qualifications separately, so that mandatory capabilities can be checked during delegation.
11. As an administrator, I want to create and manage assignment types, so that recurring kinds of work are consistently classified.
12. As an administrator, I want to create projects, so that related tasks and scheduled work have a shared operational context.
13. As an administrator, I want to move projects through draft, active, on-hold, completed, and archived states, so that project lifecycle is visible and controlled.
14. As an administrator, I want to create tasks within a project, so that the work required by the project is explicit.
15. As an administrator, I want to define task details, requirements, resources, and dependencies, so that employees have enough information to perform the work.
16. As an administrator, I want to assign one responsible person to a task, so that ownership and coordination are clear.
17. As an administrator, I want to change the responsible person after work has started, so that ownership can follow organizational changes while preserving history.
18. As an administrator, I want to create multiple assignments for one task, so that work can be shared across employees or scheduled across multiple time periods.
19. As an administrator, I want to assign employees across multiple projects, so that the schedule reflects the actual workforce.
20. As an administrator, I want to schedule an assignment with detailed task information, start and end times, location, assignment type, notes, and required resources, so that the employee knows exactly what to do.
21. As an administrator, I want to delegate assignments to employees, so that planned work becomes actionable.
22. As an administrator, I want the system to warn me about overlapping employee assignments, so that I can make an informed scheduling decision.
23. As an administrator, I want to override an overlap warning with a reason, so that exceptional scheduling decisions remain possible and traceable.
24. As an administrator, I want category and qualification mismatches to produce warnings or blocks according to task requirements, so that mandatory capability requirements are respected.
25. As an administrator, I want to override eligible delegation warnings with a reason, so that operational exceptions are explicit.
26. As an employee, I want to see my upcoming assignments in a calendar, so that I can plan my foreseeable work.
27. As an employee, I want to switch between week, day, and month calendar views, so that I can inspect detailed work or obtain a broader planning overview.
28. As an employee, I want to filter my schedule by project, assignment type, status, category, and responsible person when permitted, so that I can focus on relevant work.
29. As an employee, I want to see the tasks, time, location, status, notes, and resources for my assignments, so that I can arrive prepared.
30. As an employee, I want to see the schedules of employees in my category, so that I can coordinate related work.
31. As an employee, I want category schedule views to hide private notes, expenses, and sensitive project details, so that operational visibility does not expose unnecessary information.
32. As an employee, I want to acknowledge an assignment, so that the administrator knows I have seen it.
33. As an employee, I want to decline an assignment before check-in with a mandatory reason, so that the administrator can resolve an assignment I cannot perform.
34. As an administrator, I want declined assignments to remain in history and become available for reassignment, so that no work disappears silently.
35. As an administrator, I want silence to become automatic acceptance at the assignment start time, so that an assignment does not remain indefinitely unacknowledged.
36. As an administrator, I want explicit acknowledgement and automatic acceptance recorded separately, so that acceptance history is transparent.
37. As an employee, I want to check in to a valid assignment, so that my attendance is recorded.
38. As an employee, I want to check out of an active assignment, so that the system records when I finished.
39. As an employee, I want the server to record authoritative check-in and check-out times, so that attendance data cannot be fabricated by client clocks.
40. As an administrator, I want to correct check-in and check-out records with a reason, so that mistakes can be fixed without erasing the original history.
41. As an administrator, I want to prevent simultaneous active check-ins for an employee, so that attendance records remain meaningful.
42. As an administrator, I want assignment states such as planned, acknowledged, auto-accepted, in-progress, completed, cancelled, missed, and declined, so that assignment progress is clear.
43. As an employee or responsible person, I want to update permitted assignment or task statuses, so that progress is visible to the organization.
44. As an administrator, I want to define custom task and assignment statuses with names, colors, descriptions, ordering, and transitions, so that workflows can reflect organizational needs.
45. As an administrator, I want used statuses to be archived rather than deleted, so that historical records remain understandable.
46. As a responsible person, I want to update task progress and apply permitted task statuses, so that I can coordinate work without receiving full administrative authority.
47. As an administrator, I want to override protected status transitions, so that exceptional operational cases can be resolved and audited.
48. As an administrator, I want to create reusable resources such as equipment, vehicles, materials, and locations, so that assignment requirements are consistent.
49. As an administrator, I want to mark resources as required, recommended, or informational for an assignment, so that employees know what they must bring or consider.
50. As an administrator, I want to create reusable expense templates, so that common task or assignment costs do not need to be entered from scratch.
51. As an administrator, I want to add expenses to a project, task, or assignment, so that financial context is connected to the work that caused it.
52. As an administrator, I want to edit expenses after copying them, so that copied entries reflect the current work.
53. As an administrator, I want expense totals calculated from quantity and unit price, so that arithmetic is consistent.
54. As an administrator, I want expenses to include category, currency, estimated or actual state, and optional receipt or reference data, so that costs are understandable.
55. As an administrator, I want to copy expenses between compatible projects, tasks, and assignments, so that repeated costs can be reused efficiently.
56. As an administrator, I want copied expenses to reset receipts, approval state, and transaction-specific references, so that copied data does not pretend to be a completed transaction.
57. As an administrator, I want expense states such as estimated, submitted, approved, rejected, and actual, so that expense handling can support operational and production workflows.
58. As an employee, I want to view expenses relevant to my assignments when permitted, so that I understand the work context without being able to modify financial records by default.
59. As an administrator, I want to view all project, task, assignment, user, resource, expense, and audit information, so that I can manage the organization.
60. As an administrator, I want audit history for delegation, role changes, schedule changes, overrides, status transitions, expense changes, and attendance corrections, so that important decisions are traceable.
61. As an employee, I want to see user-facing history for assignments and attendance corrections that affect me, so that I can understand changes to my work record.
62. As an administrator, I want deactivated employees prevented from receiving new assignments while their history is preserved, so that account lifecycle does not corrupt operational records.
63. As an administrator, I want future assignments for deactivated employees flagged for review, so that work is explicitly reassigned or cancelled.
64. As a user, I want clear validation and authorization errors, so that I understand why an action was rejected.
65. As a user, I want expired sessions to refresh once and then redirect to login if refresh fails, so that normal session expiry is handled predictably.
66. As an administrator, I want paginated and filterable collections, so that the application remains usable as organizational data grows.
67. As a user, I want schedule conflicts returned clearly when another change has occurred since I loaded the data, so that I do not accidentally overwrite someone else's schedule change.
68. As an administrator, I want soft deletion for records with operational history, so that reporting and audit trails remain intact.
69. As an administrator, I want downloadable calendar events, so that planned work can be viewed in an external calendar without requiring two-way synchronization.
70. As a maintainer, I want automated backend, frontend, integration, and end-to-end checks in GitHub Actions, so that changes are evaluated consistently before merging.
71. As a maintainer, I want local development services to run through Docker Compose while production connects to the existing PostgreSQL server, so that environments match deployment constraints.
72. As a maintainer, I want Hibernate to validate the production schema without modifying it, so that accidental runtime schema changes cannot damage operational data.
73. As a administrator, I wish to be able to change the colors on the board.

## Implementation Decisions

- Build a React frontend and a Java/Javalin backend connected through a REST API.
- Version REST endpoints under `/api/v1` and use consistent representations for validation errors, authorization failures, pagination, filtering, date/time values, and concurrency conflicts.
- Use PostgreSQL as the persistence store. The production database already runs on a server; production Docker Compose must not start a PostgreSQL container.
- Use Docker Compose for the React delivery service, Java backend, and supporting local development services. Keep PostgreSQL data external in production and require persistent backups owned by the infrastructure operator.
- Use Hibernate ORM for entity mapping and persistence and JPQL for application queries. Keep persistence concerns behind repository/service boundaries rather than placing queries in HTTP handlers.
- Configure Hibernate to validate the production schema rather than create or update it. Schema evolution requires explicit, versioned database changes in the deployment process; JPQL is not a schema migration mechanism.
- Store all persisted timestamps in UTC and retain the relevant project or assignment time zone for display.
- Model every record inside an organization boundary even though the first release targets one organization. Admins have organization-level access; a protected system administrator may manage platform-level operations.
- Represent users, roles, and permissions separately. Admins compose roles from a fixed permission catalogue. Role changes apply immediately and invalidate active sessions through a token-version mechanism.
- Issue short-lived JWT access tokens and rotating refresh tokens from the Javalin backend. Store refresh tokens in secure, HTTP-only, same-site cookies, keep access tokens in memory where possible, and protect cookie-authenticated refresh/logout flows against CSRF.
- Support password reset with expiring single-use tokens, rate-limit authentication and recovery attempts, and prevent user enumeration.
- Use organization-level roles initially. Project visibility is controlled by assignment connection and explicit authorization; project-specific memberships may be added later if needed.
- Let admins manage employee categories, with one primary category per employee and qualifications modeled separately.
- Model a project as a lifecycle-managed container for tasks. Use `draft`, `active`, `on_hold`, `completed`, and `archived` project states.
- Model a task as reusable work within a project. A task has one optional responsible person, task details, requirements, progress, dependencies, custom task status, and related assignments.
- Model an assignment as a scheduled employee allocation of a task. It owns scheduled start/end time, assignment type, location, notes, status, acknowledgement outcome, resources, and check-in/out records.
- Allow multiple assignments per task and employees across multiple projects. Permit overlapping assignments, but warn the administrator and require an override reason where the warning is overridden.
- Use one-time assignments in the first release. Defer recurrence until the ordinary assignment lifecycle, conflict behavior, calendar, and attendance workflows are stable.
- Require valid project/task access and category or qualification compatibility for delegation. Warn on non-mandatory mismatches and block mandatory mismatches unless the domain explicitly permits an audited admin override.
- Let employees acknowledge or decline assignments before check-in. Decline requires a reason, does not delete history, and leaves reassignment to an authorized administrator. After check-in, use a separate issue or cancellation request rather than decline.
- Treat silence as acceptance at the assignment start time. Preserve `acknowledged` and `auto_accepted` as distinct outcomes.
- Prevent check-in for cancelled, declined, or otherwise invalid assignments. Prevent simultaneous active check-ins for one employee unless an authorized administrator explicitly overrides the rule.
- Treat check-in/out as operational attendance records, not payroll calculations. Use server timestamps and preserve corrections in audit history.
- Define separate task and assignment status sets. Admins manage custom status definitions and configurable transitions; protected lifecycle concepts such as active, completed, and cancelled remain available. Used statuses are archived, never deleted.
- Keep resources distinct from expenses. Resources are informational in the first release and support required, recommended, and informational usage modes. Formal reservation and resource conflict handling are deferred.
- Support reusable expense templates and editable expense entries at project, task, and assignment scope. Store exact decimal monetary amounts, quantity, unit price, category, currency, description, and estimated/actual or approval state. Use one project currency initially.
- Permit expense creation, editing, copying, approval, rejection, and deletion to administrators by default. Employees may view relevant expenses only when permitted.
- When copying an expense, copy descriptive and pricing fields but reset receipts, approval state, actual status, and transaction-specific references. Require confirmation when copying across projects.
- Use soft deletion for users, projects, tasks, assignments, expenses, and resources once operational history exists. Permanent deletion is limited to unused drafts or protected administrative procedures.
- Provide in-app notifications for assignment creation, changes, cancellation, decline, missed acknowledgement exceptions, and relevant overrides. Defer email and push delivery.
- Provide week, day, and month calendar views, with week as the default planning view and day as the detailed inspection view. Allow downloadable `.ics` events and defer two-way external calendar synchronization.
- Show category colleagues assignment time, status, project/site, assignment type, and employee name as needed for coordination. Hide private notes, expenses, and sensitive project details.
- Allow administrators to modify schedules and task ownership after work starts, but preserve previous values and reasons in audit history.
- Add optimistic concurrency checks for schedule edits so a stale client receives a conflict response rather than overwriting a newer change.
- Paginate potentially large collections and support filtering for projects, tasks, assignments, employees, expenses, and audit records with stable ordering.
- Organize the backend around HTTP resources, application services, domain validation, repositories, and persistence entities. Keep the highest behavioral seam at the REST API and service boundary.
- Organize the frontend around authenticated application flows, role-aware navigation, calendar and schedule views, project/task detail, assignment detail, expense management, notifications, and shared API error/session handling.
- Use GitHub as the source-control and collaboration platform, GitHub Projects for planning, and GitHub Actions for pull-request tests and checks. The project team owns the GitHub configuration and workflow.

## Testing Decisions

- The primary test seam is the versioned REST API backed by an isolated PostgreSQL test database. Tests should exercise externally observable behavior through HTTP requests and responses, authorization context, persisted state, and returned domain data.
- Since the repository currently has no application code or test suite, there is no existing prior art to follow. Establish backend integration tests and frontend workflow tests as the initial testing conventions.
- Test authentication and authorization through real API calls, including role composition, permission changes, deactivated users, organization boundaries, JWT expiry/refresh, and unauthorized access.
- Test project, task, responsible-person, category, qualification, and assignment workflows through API-level scenarios.
- Test overlap warnings, explicit admin overrides with reasons, mandatory qualification blocks, stale schedule edits, cancellation, reassignment, and soft deletion.
- Test acknowledgement, decline, auto-acceptance at assignment start, explicit versus automatic acceptance, and the inability to decline after check-in.
- Test check-in/check-out state transitions, server timestamps, simultaneous check-in prevention, invalid assignment rejection, and administrative corrections with audit records.
- Test custom task and assignment statuses, configurable transitions, protected lifecycle concepts, permitted responsible-person transitions, and archiving of used statuses.
- Test category schedule visibility and privacy boundaries, including what employees can see about colleagues and what remains hidden.
- Test expense creation, exact monetary calculations, copying, reset of transaction-specific fields, scope inheritance, approval states, and administrator-only mutation.
- Test pagination, filtering, consistent error payloads, refresh handling, and optimistic concurrency responses.
- Test Hibernate mappings and JPQL repository queries through integration tests against PostgreSQL rather than mocking persistence behavior.
- Test database startup/schema validation and representative versioned schema changes before deployment.
- Test React user workflows for login, calendar navigation, assignment detail, acknowledge/decline, check-in/out, status changes, expense administration, and API error/session states. Prefer user-visible behavior and accessible interaction outcomes over component implementation details.
- Add at least one end-to-end scenario covering login, viewing a schedule, inspecting assignment resources, checking in, and checking out.
- Run backend, frontend, integration, and end-to-end checks in GitHub Actions for pull requests.

## Out of Scope

- Multi-organization administration beyond the organization boundary needed for safe data isolation.
- Recurring assignments and unlimited future occurrence generation.
- Formal resource reservations, resource availability calculations, and resource conflict resolution.
- Payroll calculations, billable hours, rounding rules, and accounting-system integration.
- Employee-created or employee-approved expenses in the first release.
- Email, SMS, and push notification delivery.
- Two-way Google Calendar or Microsoft Calendar synchronization.
- Offline schedule access and offline check-in queues.
- GPS or geofenced check-in.
- Automatic reassignment after an employee declines an assignment.
- Arbitrary user-defined executable permission rules.
- Project-specific roles and memberships unless later requirements make organization-level roles insufficient.
- Permanent deletion of records with operational history.

## Further Notes

- The product baseline was confirmed through the grilling process and includes the decisions above. New behavior should be treated as an explicit scope change.
- The initial repository contains no application implementation, domain glossary, ADRs, or test prior art. The implementation should establish those project conventions as the system is scaffolded.
- The existing PostgreSQL server's backup, upgrade, monitoring, TLS, firewall, and recovery ownership must be documented by the infrastructure owner before production use.
- The implementation should introduce domain terminology consistently: project, task, assignment, responsible person, employee category, qualification, resource, expense, role, permission, custom status, acknowledgement, auto-acceptance, check-in, check-out, and audit event.
