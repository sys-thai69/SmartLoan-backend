# WingLoan Backend

Spring Boot backend for WingLoan - Peer-to-Peer Informal Loan Tracker.

## Requirements

- Java 21+
- PostgreSQL (Supabase) for production, H2 for development

## Running the Backend

### Option 1: Using the start script (Windows)

Double-click `start-backend.bat` - it will automatically download Maven and start the server.

### Option 2: Using an IDE (Recommended)

1. Open this folder in **IntelliJ IDEA** or **VS Code with Java extensions**
2. Run `SmartLoanApplication.java`

### Option 3: Using Maven (if installed)

```bash
mvn spring-boot:run
```

### Option 4: Using Docker

```bash
docker build -t wingloan-backend .
docker run -p 8080:8080 wingloan-backend
```

## Database Configuration

### Development (H2 - Default)
The app uses H2 in-memory database by default for development.
Access H2 Console at: http://localhost:8080/h2-console

### Production (Supabase PostgreSQL)

1. Copy `.env.example` to `.env`
2. Fill in your Supabase database credentials:

```properties
DB_HOST=db.YOUR_PROJECT_REF.supabase.co
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=your_database_password
SPRING_PROFILES_ACTIVE=prod
```

3. Run with production profile:
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Or set the environment variables and run normally.

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

## AI Feature

The **Natural Language Loan Parser** is located in:
- `src/main/java/com/smartloan/service/AIService.java`
- `src/main/java/com/smartloan/controller/AIController.java`

It parses input like:
> "lend Channy $20, pay back in 2 weeks, no interest"

Into structured loan data.
