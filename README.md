# Service Connect Hub — Spring Boot Backend

A full REST API backend for the **Service Connect Hub** React frontend.  
Stack: **Java 17 · Spring Boot 3.2 · Spring Security · JWT · PostgreSQL · JPA/Hibernate**

---

## Quick Start

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 2. Create the database
```sql
CREATE DATABASE service_connect_hub;
```

### 3. Configure environment variables
Minimum required values:
```properties
DB_URL=jdbc:postgresql://localhost:5432/service_connect_hub
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your-long-random-secret
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

On Railway, if you attach a Railway PostgreSQL service, set:
```properties
DATABASE_URL=postgresql://username:password@host:port/database
JWT_SECRET=your-long-random-secret
APP_CORS_ALLOWED_ORIGINS=https://your-vercel-app.vercel.app
```

### 4. Run
```bash
mvn spring-boot:run
```

The server starts on **http://localhost:8080** by default.  
Health check: **GET /health**
On first startup the admin account is auto-seeded:
- **Phone:** `1111111111`  **Password:** `admin123`

## Deployment on Railway

1. Push this repository to GitHub.
2. Create a new project on Railway and connect your GitHub repository.
3. Add a PostgreSQL database to your Railway project.
4. Set the following environment variables in Railway:
   - `DATABASE_URL` (provided by Railway's PostgreSQL service)
   - `JWT_SECRET` (generate a long random string)
   - `APP_CORS_ALLOWED_ORIGINS` (your frontend URL, e.g., https://your-app.vercel.app)
   - Optionally, adjust other variables like `JWT_EXPIRATION`, `APP_ADMIN_*`
5. Railway will automatically build and deploy using the Dockerfile.

For deployment steps, see:

[`DEPLOYMENT.md`](../../DEPLOYMENT.md)

---

## Project Structure

```
src/main/java/com/serviceconnect/
├── config/
│   ├── SecurityConfig.java       # Spring Security + CORS
│   └── DataInitializer.java      # Seeds admin on startup
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── AdminController.java
│   ├── ServiceRequestController.java
│   └── TechnicianController.java
├── dto/
│   ├── request/                  # Incoming payloads
│   └── response/                 # Outgoing payloads
├── entity/
│   ├── User.java                 # Base (JOINED inheritance)
│   ├── Client.java
│   ├── Technician.java
│   ├── ServiceRequest.java
│   ├── JobNotification.java
│   └── ActivityLog.java
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/                   # Spring Data JPA repos
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserPrincipal.java
│   └── CustomUserDetailsService.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── AdminService.java
│   ├── ServiceRequestService.java
│   └── ActivityLogService.java
└── util/
    └── UserMapper.java
```

---

## Authentication

All protected endpoints require:
```
Authorization: Bearer <token>
```

Tokens are JWTs signed with HS256. They carry `userId` and `role` claims.

---

## API Reference

### Auth

#### `POST /auth/login`
```json
// Request
{ "phone": "0712345678", "password": "secret", "role": "client" }

// Response 200
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJ...",
    "tokenType": "Bearer",
    "user": { "id": 1, "name": "John", "role": "client", ... }
  }
}
```
> **Note:** Admin can log in using `role: "technician"` or `role: "admin"`.

---

#### `POST /auth/register`

**Client:**
```json
{
  "name": "Jane Doe",
  "phone": "0712345678",
  "password": "secret123",
  "role": "client"
}
```

**Technician:**
```json
{
  "name": "Bob Fix",
  "phone": "0798765432",
  "password": "secret123",
  "role": "technician",
  "serviceTypes": ["Plumbing", "Electrical"],
  "location": { "lat": -6.7924, "lng": 39.2083, "address": "Dar es Salaam" }
}
```
> Technicians are created with `status: "pending"` and cannot log in until approved by admin.

---

### Users

| Method | Endpoint     | Description                     |
|--------|--------------|---------------------------------|
| GET    | `/users/me`  | Get current user profile        |
| PUT    | `/users/me`  | Update current user profile     |

**PUT /users/me body (technician example):**
```json
{
  "availability": "available",
  "location": { "lat": -6.80, "lng": 39.28, "address": "Kariakoo" }
}
```

---

### Service Requests

| Method | Endpoint                        | Description                          |
|--------|---------------------------------|--------------------------------------|
| POST   | `/requests`                     | Create request (triggers matching)   |
| GET    | `/requests/:id`                 | Get request by ID                    |
| GET    | `/requests?clientId=1`          | All requests for a client            |
| GET    | `/requests?technicianId=2`      | All jobs for a technician            |
| GET    | `/requests/:id/status`          | Get just the status                  |
| POST   | `/requests/:id/assign`          | Manually assign technician           |
| POST   | `/requests/:id/cancel`          | Cancel (client only)                 |
| POST   | `/requests/:id/status`          | Update status (technician/admin)     |
| GET    | `/requests/:id/tracking`        | Live tracking + ETA                  |
| GET    | `/requests/notifications/pending` | Pending job notifications for tech  |

**POST /requests body:**
```json
{
  "serviceType": "Plumbing",
  "description": "Leaking pipe under the sink",
  "location": { "lat": -6.7924, "lng": 39.2083, "address": "Upanga, Dar es Salaam" }
}
```

**POST /requests/:id/status body:**
```json
{ "status": "in_progress" }
```
Valid values: `pending | searching | matched | in_progress | completed | cancelled`

**GET /requests/:id/tracking response:**
```json
{
  "success": true,
  "data": {
    "requestId": 5,
    "requestStatus": "matched",
    "estimatedArrival": "2024-03-15T14:30:00",
    "technicianLocation": {
      "technicianId": 3,
      "technicianName": "Bob Fix",
      "lat": -6.80,
      "lng": 39.28,
      "address": "Kariakoo",
      "availability": "busy"
    },
    "clientLocation": {
      "lat": -6.7924,
      "lng": 39.2083,
      "address": "Upanga, Dar es Salaam"
    }
  }
}
```

---

### Technicians

| Method | Endpoint                   | Description                    |
|--------|----------------------------|--------------------------------|
| GET    | `/technicians/:id`         | Public technician profile      |
| GET    | `/technicians/:id/location`| Technician current location    |

---

### Admin (requires `ROLE_ADMIN`)

| Method | Endpoint                            | Description                   |
|--------|-------------------------------------|-------------------------------|
| GET    | `/admin/dashboard`                  | Platform stats summary        |
| GET    | `/admin/technicians?status=pending` | List technicians (filterable) |
| GET    | `/admin/technicians/:id`            | Technician detail             |
| POST   | `/admin/technicians/:id/approve`    | Approve a technician          |
| POST   | `/admin/technicians/:id/reject`     | Reject a technician           |
| GET    | `/admin/requests`                   | All service requests          |
| GET    | `/admin/logs`                       | Activity logs                 |

**POST /admin/technicians/:id/reject body:**
```json
{ "reason": "Incomplete documentation submitted" }
```

---

## Matching Flow

When `POST /requests` is called:

1. Request is saved with status `pending`
2. Status changes to `searching`
3. Backend queries for approved + available technicians offering that `serviceType`
4. Nearest technician is selected via **Haversine distance formula**
5. A `JobNotification` is created for that technician
6. Technician is auto-assigned → request status → `matched`
7. Technician availability → `busy`
8. ETA is estimated: `max(10, min(30, distanceKm × 3))` minutes

---

## Technician Approval Workflow

```
Register → status: pending
              │
    Admin reviews via GET /admin/technicians?status=pending
              │
    ┌─────────┴─────────┐
    ▼                   ▼
  Approve             Reject
  status: approved    status: rejected
  availability: available
```

---

## Error Responses

All errors follow this shape:
```json
{
  "success": false,
  "message": "Detailed error message"
}
```

| HTTP Code | Meaning                     |
|-----------|-----------------------------|
| 400       | Validation / bad request    |
| 401       | Unauthenticated             |
| 403       | Forbidden (wrong role)      |
| 404       | Resource not found          |
| 500       | Unexpected server error     |

---

## Frontend Integration

Update your React axios base URL:
```js
// src/lib/api.js or similar
const api = axios.create({
  baseURL: 'http://localhost:8080',
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### Login flow
```js
const { data } = await api.post('/auth/login', { phone, password, role });
localStorage.setItem('token', data.data.token);
localStorage.setItem('user',  JSON.stringify(data.data.user));
// Redirect based on data.data.user.role
```

---

## CORS

By default, the following origins are allowed:
```
http://localhost:5173   (Vite)
http://localhost:3000   (CRA)
```

To add more, edit `app.cors.allowed-origins` in `application.properties`.
