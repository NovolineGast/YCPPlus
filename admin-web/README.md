# YCPPlus Admin Web

Modern web-based administration panel for YCPPlus license key management.

## Architecture

- **Backend**: Spring Boot 3.2 + JWT Authentication
- **Frontend**: React 18 + Vite
- **Database**: SQLite
- **API**: RESTful

## Quick Start

### Backend

```bash
cd backend
mvn clean package
java -jar target/admin-api-1.0.0.jar
```

Server runs on `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Web UI available at `http://localhost:5173`

## First-Time Setup

1. Start both backend and frontend
2. Open `http://localhost:5173` in your browser
3. Click "First time? Initialize admin"
4. Enter your application name and password
5. Sign in with the credentials

## Features

✅ JWT-based authentication  
✅ Dashboard with real-time statistics  
✅ Generate license keys (1-50 keys, 1-9999 days)  
✅ Ban/Unban keys  
✅ Delete keys  
✅ View key details (created, expires, login count)  
✅ Responsive design (mobile-friendly)  
✅ Dark theme optimized for data-heavy work  

## API Endpoints

### Authentication
- `POST /api/init` - Initialize admin account (first time)
- `POST /api/login` - Login and receive JWT token

### Dashboard
- `GET /api/dashboard/stats` - Get statistics

### Key Management
- `GET /api/keys` - List all keys
- `POST /api/keys/generate` - Generate new keys
- `POST /api/keys/{key}/ban` - Ban a key
- `POST /api/keys/{key}/unban` - Unban a key
- `DELETE /api/keys/{key}` - Delete a key
- `GET /api/keys/{key}/fingerprint` - Get SHA256 fingerprint

## Design Philosophy

This admin panel follows a **vault aesthetic**:
- Deep blue color scheme (not generic black or cream)
- Monospace fonts for keys and technical data
- Minimal but deliberate animations
- Data-first layout with clear hierarchy
- Professional tone without being cold

## Development

### Backend
```bash
# Run with hot reload
mvn spring-boot:run

# Build JAR
mvn clean package
```

### Frontend
```bash
# Development server
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

## Production Deployment

### Option 1: Separate Deployment

Deploy backend and frontend separately (recommended for scale).

### Option 2: Single JAR

Build frontend and include in Spring Boot:

```bash
cd frontend
npm run build

# Copy dist/ to backend/src/main/resources/static/
cp -r dist/* ../backend/src/main/resources/static/

cd ../backend
mvn clean package
```

Then run the single JAR:
```bash
java -jar target/admin-api-1.0.0.jar
```

Access at `http://localhost:8080`

## Environment Variables

### Backend
- `SERVER_PORT` - Server port (default: 8080)
- `DB_PATH` - SQLite database path (default: data/ycp_auth.db)

### Frontend
- `VITE_API_URL` - Backend API URL for production builds

## Security Notes

- JWT tokens expire after 24 hours
- Passwords are hashed with BCrypt
- CORS is configured for localhost (update for production)
- Database is SQLite (consider PostgreSQL for production)

## License

MIT
