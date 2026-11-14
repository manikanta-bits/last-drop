# Deploying LASTDROP (public link for judges)

Three free services: **database** (Aiven MySQL), **backend** (Render), **frontend** (Vercel).
Never put passwords in the code - they go into each service's "Environment variables" screen.

## 1. Put the code on GitHub

```powershell
cd "C:\Users\Mani\OneDrive\Desktop\riverse aunction"
git remote add origin https://github.com/<your-username>/lastdrop.git
git branch -M main
git push -u origin main
```

## 2. Database - Aiven MySQL (free plan)

1. Sign up at aiven.io and create a **MySQL** service (Free plan).
2. Open the service, go to **Databases**, and add a database named `lastdrop`.
3. On the **Overview** page, note the **Host**, **Port**, **User** (avnadmin) and **Password**.
4. Load the tables and demo data from your PC (replace HOST, PORT and PASSWORD):

```powershell
cd database
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
& $mysql -h HOST -P PORT -u avnadmin -pPASSWORD --ssl-mode=REQUIRED --default-character-set=utf8mb4 -e "source 02-schema.sql"
& $mysql -h HOST -P PORT -u avnadmin -pPASSWORD --ssl-mode=REQUIRED --default-character-set=utf8mb4 -e "source 03-sample-data.sql"
```

## 3. Backend - Render

1. Sign up at render.com, then choose **New > Web Service** and connect your GitHub repo.
2. Set **Root Directory** to `backend` and **Runtime** to **Docker**. The `Dockerfile` is already there.
3. Add these environment variables:

| Name | Value |
|---|---|
| `DB_HOST` | Aiven host |
| `DB_PORT` | Aiven port |
| `DB_NAME` | `lastdrop` |
| `DB_USERNAME` | `avnadmin` |
| `DB_PASSWORD` | Aiven password |
| `JWT_SECRET` | any random text of 40+ characters |
| `CORS_ALLOWED_ORIGINS` | your Vercel address from step 4, e.g. `https://lastdrop.vercel.app` |
| `DEMO_MODE` | `false` (the admin can switch it on in the app) |

4. Deploy. Check that `https://<your-backend>.onrender.com/api/health` shows `"status":"UP"`.

## 4. Frontend - Vercel

1. Sign up at vercel.com, choose **Add New > Project**, and import the repo.
2. Set **Root Directory** to `frontend`. Vercel detects Vite automatically.
3. Add the environment variable `VITE_API_BASE_URL` = `https://<your-backend>.onrender.com` (no slash at the end).
4. Deploy. You get a link like `https://lastdrop.vercel.app`.
5. Go back to Render and make sure `CORS_ALLOWED_ORIGINS` is exactly this Vercel link, then redeploy the backend.

The frontend opens the WebSocket at `wss://<your-backend>.onrender.com/ws` automatically. HTTPS and WSS are provided by Render and Vercel.

## 5. Check it on two phones

1. Open the Vercel link on two phones and log in on one of them as `admin@lastdrop.demo` / `Demo@123`.
2. In the admin dashboard, click **Start demo mode**.
3. On both phones, open the same product. Every 10 seconds the price drops at the same moment on both.

Note: Render's free plan sleeps after 15 minutes without visitors. Open the backend `/api/health` link about a minute before you present.
