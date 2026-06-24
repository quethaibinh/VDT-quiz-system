# Quiz Platform

## Run with Docker

The Docker Compose stack runs every service currently available in this
repository:

- API Gateway: `http://localhost:8080`
- Role-based Frontend: `http://localhost:3000`
- Auth PostgreSQL: `localhost:5433`
- Question PostgreSQL: `localhost:5434`
- Exam PostgreSQL: `localhost:5435`
- Exam Runtime PostgreSQL: `localhost:5436`

### Prerequisites

- Docker Desktop or Docker Engine
- Docker Compose v2

### Configuration

Create the local environment file from the provided template:

```powershell
Copy-Item .env.example .env
```

Replace every example password and secret in `.env` before starting the stack.
Compose keeps Auth, Question, and Exam Service ports private because these
services trust identity headers created by Gateway. Only Gateway is published
as the backend entry point. Gateway signs those forwarded headers with
`GATEWAY_TRUSTED_SECRET`; internal service calls use the separate required
`INTERNAL_API_KEY`. The existing Spring configuration still supports running a
service directly from the IDE for local debugging when the same environment
variables are available.

### Start

```powershell
docker compose up --build -d
docker compose ps
```

Open the frontend at `http://localhost:3000`. Override the published
port with `FRONTEND_PORT` in `.env`. Nginx forwards frontend `/v1/api` requests
to the Gateway over the Compose network.

### Admin workspace

Administrators sign in through the same frontend and are routed to
`/admin/dashboard`. The released Admin workspace includes:

- Real dashboard counts from Auth Service and Question Service.
- Teacher/student search, profile updates, and activation/deactivation.
- Partial-success `.xlsx` account import.
- Subject create, edit, archive, restore, and verified teacher assignment.

All Admin browser requests use Gateway routes under `/v1/api/admin/**`. The
frontend never calls Auth Service, Question Service, or internal endpoints
directly. Admin business data is not supplied by MSW or other runtime fixtures.
Teacher assignment is selected by teacher name/code from real Auth data; users
never enter teacher UUIDs manually.

Follow logs with:

```powershell
docker compose logs -f
```

Open a PostgreSQL shell inside the container:

```powershell
docker compose exec postgres psql -U postgres -d auth-service-db
```

Stop containers without deleting database data:

```powershell
docker compose down
```

Stop containers and delete the PostgreSQL volume:

```powershell
docker compose down -v
```

Question, Exam, and Exam Runtime services each use a separate PostgreSQL
container. Exam Runtime consumes `ExamActivated` from Kafka and stores
readiness metadata in Redis under `runtime:exam:{examId}:activation`; it does
not create student sessions until a student joins. Exam draft APIs are routed
through Gateway; internal validation and snapshot fallback endpoints are
service-network only. See `docs/EXAM_DRAFT_MANAGEMENT.md` and
`docs/EXAM_ACTIVATION_RUNTIME_READINESS.md`.
