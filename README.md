# Local database setup

1. Copy `.env.example` to `.env` in the project root and replace the password placeholder with your local database password. The `.env` file is ignored by Git.
2. Start Docker Desktop. Open `docker-compose.yaml` in IntelliJ and click the Run icon beside the `mysql` service. Alternatively, run `docker compose up -d` from the project root.
3. Wait for the service to become healthy. MySQL is available at `127.0.0.1:3306` and the `expense_splitter` database is created automatically on the first start.
4. In IntelliJ, open Run > Edit Configurations and select the configuration for `ExpenseSplitterApplication`. Set `DB_USERNAME=root` and `DB_PASSWORD` to the same password as in `.env`, then run the application. Docker Compose reads `.env`; the Spring application needs its own environment variables.
5. For the frontend, run `npm.cmd install` and `npm.cmd run dev` from `frontend`, then open the URL printed by Vite.

The named volume `mysql_data` keeps database data when the container is stopped or recreated. Use `docker compose stop` to stop MySQL. Changing the password in `.env` does not change the password in an already initialized database.

## Upgrading an existing database

Stop the application and apply [001-group-roles.sql](docs/migrations/001-group-roles.sql) once to the existing MySQL schema before starting the updated version. It assigns the administrator role to each existing group's creator. A fresh database gets the new schema from Hibernate on its first start.
