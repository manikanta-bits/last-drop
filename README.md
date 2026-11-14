# LASTDROP

Save Food. Reduce Waste. Discover Deals.

A simple marketplace where local shops list food close to its expiry date.
The price drops automatically every hour (a "Dutch auction") until someone reserves it.

## Folders

- `backend/`  - Java Spring Boot API (port 8080)
- `frontend/` - React app built with Vite (port 5173)

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
