# SmartLoan Backend

Spring Boot backend for SmartLoan - Peer-to-Peer Informal Loan Tracker.

## Requirements

- Java 21+
- PostgreSQL (Supabase)

## Local Development

### Option 1: Using PowerShell script (Recommended)

```powershell
# Copy .env.example to .env and fill in your values
cp .env.example .env

# Run the backend
powershell -ExecutionPolicy Bypass -File run.ps1
```

### Option 2: Using an IDE

1. Open this folder in **IntelliJ IDEA** or **VS Code**
2. Set environment variables from `.env`
3. Run `SmartLoanApplication.java`

### Option 3: Using Docker

```bash
docker build -t smartloan-backend .
docker run -p 8080:8080 \
  -e DB_URL="your_db_url" \
  -e DB_USERNAME="your_username" \
  -e DB_PASSWORD="your_password" \
  -e JWT_SECRET="your_jwt_secret" \
  smartloan-backend
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

| Variable | Description |
|----------|-------------|
| `SERVER_PORT` | Server port (default: 8080) |
| `JWT_SECRET` | JWT signing secret (min 64 chars) |
| `DB_URL` | Supabase connection URL with `?prepareThreshold=0` |
| `DB_USERNAME` | Supabase username (e.g., `postgres.xxxx`) |
| `DB_PASSWORD` | Supabase password |
| `ANTHROPIC_API_KEY` | Optional - for AI features |

## Deploy to Render

### Step 1: Push to GitHub
Make sure your code is pushed to GitHub.

### Step 2: Create Web Service on Render
1. Go to [render.com](https://render.com) and sign up/login
2. Click **New +** → **Web Service**
3. Connect your GitHub repository (`SmartLoan-backend`)
4. Configure:
   - **Name**: `smartloan-backend`
   - **Region**: Singapore (closest to Supabase)
   - **Runtime**: Docker
   - **Plan**: Free (or Starter for better performance)

### Step 3: Set Environment Variables
In Render dashboard, add these environment variables:

```
JWT_SECRET=SmartLoan2024SecureSecretKey_VeryLongSecretForHS512Algorithm_AtLeast64BytesRequired!
DB_URL=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres?prepareThreshold=0
DB_USERNAME=postgres.wxjvohtdhacqqqyrlvra
DB_PASSWORD=your_password_here
```

### Step 4: Deploy
Click **Create Web Service** and wait for deployment.

Your API will be available at: `https://smartloan-backend.onrender.com`

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register user |
| `/api/auth/login` | POST | Login |
| `/api/auth/me` | GET | Get current user |
| `/api/loans` | POST | Create loan |
| `/api/loans/mine` | GET | Get my loans |
| `/api/loans/{id}` | GET | Get loan details |
| `/api/ai/parse-loan` | POST | Parse natural language loan |
| `/api/wallet/me` | GET | Get wallet |
| `/actuator/health` | GET | Health check |

## AI Feature

The **Natural Language Loan Parser** parses input like:
> "lend Channy $20, pay back in 2 weeks, no interest"

Into structured loan data.

Located in:
- `src/main/java/com/smartloan/service/AIService.java`
- `src/main/java/com/smartloan/controller/AIController.java`
