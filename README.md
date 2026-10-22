# Marketplace API

## Build and run with Docker

From the project directory, run:

```sh
./build
```

The script builds the latest source into a Docker image, starts the API and PostgreSQL, and serves the API at `http://localhost:7001/api/v1`. Docker and Docker Compose must be installed and running.

If port 7001 is already in use on your computer, choose another host port while the app still listens on 7001 inside Docker:

```sh
HOST_PORT=7001 ./build
```

Useful commands:

```sh
docker compose logs -f api  # Follow API logs
docker compose down         # Stop the API and database
```

The database and uploaded files are stored in Docker volumes, so they persist when the containers stop. The Compose defaults are for local development. Set `POSTGRES_PASSWORD` and `JWT_SECRET` in the environment before running `./build` when using your own credentials.

See [the API reference](docs/API.md) for available endpoints.

## Deploy to Render

This is a Java service. In the Render service, open **Settings → Build → Source → Edit**, select the existing Git repository and branch, and set **Runtime** to **Docker**. Render will then build from the repository's `Dockerfile`; remove the Node `yarn` build and start commands. The Docker image defaults to Render's `PORT` (`10000`), while Compose overrides it for local development.

In the Render service's environment settings, configure `DB_URL` as a JDBC URL using the internal hostname, port, and database name shown on your Render Postgres service, for example `jdbc:postgresql://HOST:5432/DATABASE`. Also set `DB_USERNAME`, `DB_PASSWORD`, and a strong `JWT_SECRET`. Keep the API and database in the same region so the API can use the database's internal hostname. A free Render Postgres database expires after 30 days.
