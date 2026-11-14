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

## Test the backend

    cd backend
    .\mvnw.cmd test
