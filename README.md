# Quiz Platform

## Run with Docker

The Docker Compose stack runs every service currently available in this
repository:

- API Gateway: `http://localhost:8080`
- Teacher Frontend: `http://localhost:3000`
- Auth Service: `http://localhost:8081`
- Question Service: `http://localhost:8082`
- Auth PostgreSQL: `localhost:5433`
- Question PostgreSQL: `localhost:5434`

### Prerequisites

- Docker Desktop or Docker Engine
- Docker Compose v2

### Configuration

Create the local environment file from the provided template:

```powershell
Copy-Item .env.example .env
```

Replace every example password and secret in `.env` before starting the stack.
The existing Spring configuration still supports running services directly on
the host, while Compose overrides database and service URLs for its internal
network. The auth database uses `postgres:5432` inside Docker and publishes
`5433` on the host so each service database can have its own local port.

### Start

```powershell
docker compose up --build -d
docker compose ps
```

Open the Teacher frontend at `http://localhost:3000`. Override the published
port with `FRONTEND_PORT` in `.env`. Nginx forwards frontend `/v1/api` requests
to the Gateway over the Compose network.

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

The `question-service` gateway route is included in Docker Compose. It uses a
separate PostgreSQL container and publishes that database on local port `5434`.
