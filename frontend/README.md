# Sahara Quiz Frontend

Teacher-first React frontend for the Quiz Platform.

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
