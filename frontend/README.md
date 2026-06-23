# Sahara Quiz Frontend

Role-based React frontend for the Quiz Platform.

## Stack

- React, TypeScript, Vite
- React Router, TanStack Query
- Tailwind CSS
- React Hook Form, Zod
- Axios, MSW
- Vitest, Testing Library

## Run

```powershell
Copy-Item .env.example .env
npm install
npm run dev
```

Default Gateway: `http://localhost:8080`.

Set `VITE_ENABLE_MOCKS=true` in development to enable local API fixtures.
Subject, question, import, collection, and implemented Exam requests can still
pass through to the real Gateway because unhandled MSW requests are bypassed.
The MSW worker is versioned at `public/mockServiceWorker.js`.

Admin routes are always real-data routes. Enabling MSW does not register Admin
user, dashboard, import, subject, or teacher-directory handlers.

## Run with Docker Compose

From the repository root:

```powershell
docker compose up --build -d
docker compose ps
```

Open `http://localhost:3000`. The production container serves the Vite build
with Nginx, supports React Router deep links, and proxies `/v1/api` to the
Gateway service on the Docker network.

## Verification

```powershell
npm run lint
npm run typecheck
npm run test
npm run build
```

## Backend prerequisites

- Auth Service, Question Service, and Exam Service must be reachable through Gateway.
- Admin pages use real Auth and Question Service data; there are no Admin MSW
  business handlers.
- Docker Compose exposes only Gateway and frontend to the host. Auth and
  Question Service ports stay internal because those services trust identity
  headers created by Gateway.
- Login returns a plain JWT string.
- Collection restore needs Gateway CORS to allow `PATCH`.
- Exam list, detail, draft, assignment, cancellation, and scheduling use the
  implemented subject-scoped APIs.
- Exam Runtime and Result contracts remain planned and may be exercised through MSW.

## Architecture

```text
src/components/ui       domain-free primitives
src/components/shared   proven cross-feature behavior
src/features            feature-owned API, model, components, pages
src/lib                 infrastructure
src/mocks               planned API fixtures
```

Do not import another feature's internal files. Promote a component to shared
only after two real consumers require the same behavior.

## Teacher navigation

The Teacher sidebar has three stable journeys:

- `/teacher/subjects`: card grid, then question bank and collections for the
  selected subject.
- `/teacher/exams`: compact subject list, then subject-scoped exam management.
- `/teacher/results`: result-oriented subject list, then completed exams and
  result detail.

The Sahara logo always returns to `/teacher/subjects`.

## Admin navigation

An `ADMIN` session is routed to `/admin/dashboard`; a `TEACHER` session remains
in the Teacher workspace. Cross-role access goes to `/forbidden`, and
unauthenticated navigation preserves the requested pathname and query string.

- `/admin/dashboard`: real Auth user statistics and real-derived subject counts.
- `/admin/users`: URL-backed keyword, role, status, page, and sort filters.
- `/admin/users/:userId`: safe profile fields and confirmed status changes.
- `/admin/users/import`: `.xlsx` upload with partial-success row errors.
- `/admin/subjects`: subject search, status filter, create, and edit.
- `/admin/subjects/:subjectId`: archive/restore and teacher assignment.

Teacher assignment searches active teachers through the Admin Auth user API.
The UI sends the selected record's real ID; it has no UUID input or fallback
teacher directory. Inactive and broken existing assignments remain visible and
removable.
