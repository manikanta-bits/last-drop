# LASTDROP

Save Food. Reduce Waste. Discover Deals.

A simple marketplace where local shops list food close to its expiry date.
The price drops automatically every hour (a "Dutch auction") until someone reserves it.

## Folders

- `backend/`  - Java Spring Boot API (port 8080)
- `frontend/` - React app built with Vite (port 5173)
- `database/` - MySQL scripts: create database, tables, demo data

## Set up the database (once)

In PowerShell:

    powershell -ExecutionPolicy Bypass -File database\setup-database.ps1

It asks for your MySQL root password once, then prints
`SUCCESS: database 'lastdrop' is ready with 24 products.`
Run it again any time to reset the demo data.

## Demo accounts (password for all: Demo@123)

| Role | Email |
|---|---|
| Admin | admin@lastdrop.demo |
| Buyer | buyer@lastdrop.demo |
| NGO | ngo@lastdrop.demo |
| Business (approved) | business1@lastdrop.demo ... business10@lastdrop.demo |
| Business (waiting for approval) | business11@lastdrop.demo |

## Run it

Backend (terminal 1):

    cd backend
    .\mvnw.cmd spring-boot:run

Frontend (terminal 2):

    cd frontend
    npm install
    npm run dev

Open http://localhost:5173. The page shows "Connected" when both parts are running.

## What it does

- **Buyers / NGOs:** browse nearby deals (list or map, 2/5/10/20 km), watch prices fall live, reserve, cancel, see savings.
- **Businesses:** create a shop (admin approval), list products as Dutch auctions, pause/resume/cancel, mark pickups collected.
- **Admin:** approve shops, manage users, remove listings, monitor auctions, see impact estimates, switch **demo mode** (prices drop every 10 s).
- Prices are calculated only by the server (scheduler every 2 s) and pushed to every browser over WebSocket.

## Run everything with Docker (easiest)

Needs only Docker Desktop. No Java, Node or MySQL installation is required.

    docker compose up --build

Open http://localhost:3000. The database, demo data, backend and frontend all start together.
Use `docker compose down` to stop, or `docker compose down -v` to also reset the demo data.
To change passwords, copy `.env.example` to `.env` and edit it.

## Deploy

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Test the backend

    cd backend
    .\mvnw.cmd test
