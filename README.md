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
