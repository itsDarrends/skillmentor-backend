# SkillMentor Backend

A full-stack mentorship platform backend built with Spring Boot. Connects learners with mentors for skills-based learning with role-based access control and comprehensive error handling.

## 🎯 Features

- **Role-Based Access Control** — Student and Mentor roles with distinct permissions
- **Mentor Management** — Create, update, search mentors
- **Enrollment System** — Students can enroll in mentorship sessions
- **Double-Booking Prevention** — Validates time slot conflicts
- **Date Range Filtering** — Query mentors/sessions by date
- **Pagination & Sorting** — Efficient data retrieval
- **Comprehensive Error Handling** — Custom exceptions with proper HTTP status codes
- **API Documentation** — Swagger UI integration

## 🛠️ Tech Stack

```
Spring Boot 3.x | Java 21 | Maven
PostgreSQL | JPA/Hibernate
Clerk OAuth2 | Spring Security
Swagger UI | JUnit 5 | Mockito
```

## 📋 Database Schema

### Mentors Table
```sql
CREATE TABLE mentors (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    bio TEXT,
    hourly_rate DECIMAL(10,2),
    availability_start TIME,
    availability_end TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Enrollments Table
```sql
CREATE TABLE enrollments (
    id SERIAL PRIMARY KEY,
    student_id UUID NOT NULL,
    mentor_id BIGINT NOT NULL REFERENCES mentors(id),
    session_date DATE NOT NULL,
    session_time TIME NOT NULL,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_session UNIQUE(mentor_id, session_date, session_time)
);
```

## 🚀 Getting Started

```bash
# Clone repository
git clone https://github.com/itsDarrends/skillmentor-backend.git
cd skillmentor-backend

# Configure environment
cp .env.example .env
# Update DATABASE_URL, CLERK_SECRET_KEY

# Build and run
mvn clean install
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

## 📚 API Endpoints

### Mentor Endpoints

```
GET    /api/mentors                      — List all mentors (paginated)
GET    /api/mentors/{id}                 — Get mentor details
POST   /api/mentors                      — Create new mentor (MENTOR role)
PUT    /api/mentors/{id}                 — Update mentor (MENTOR role)
DELETE /api/mentors/{id}                 — Delete mentor (MENTOR role)
GET    /api/mentors/search?subject=Java  — Search mentors by subject
GET    /api/mentors/available?date=2025-02-01  — Get available mentors
```

### Enrollment Endpoints

```
POST   /api/enrollments                  — Create enrollment (STUDENT role)
GET    /api/enrollments/{id}             — Get enrollment details
GET    /api/enrollments/user             — Get user's enrollments
PUT    /api/enrollments/{id}             — Update enrollment status
DELETE /api/enrollments/{id}             — Cancel enrollment
```

## 🔐 Security

- **Role-Based Access:** `@PreAuthorize` annotations on endpoints
- **JWT Authentication:** Clerk OAuth2 integration
- **CORS:** Configured for frontend domain
- **Input Validation:** `@Valid` with custom validators

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MentorServiceTest

# With coverage report
mvn test jacoco:report
```

## 📊 Error Handling

Custom exception hierarchy:

```java
BusinessException (base)
├── ResourceNotFoundException (404)
├── InvalidEnrollmentException (400)
├── TimeSlotUnavailableException (409)
└── AuthorizationException (403)
```

## 🌐 Deployment

**Render Configuration:**
```
Build: mvn clean install
Start: java -jar target/skillmentor-backend.jar
Env: DATABASE_URL, CLERK_SECRET_KEY
```

## 📝 License

MIT License

## 💬 Contact

[itsdarrendsilva@gmail.com](mailto:itsdarrendsilva@gmail.com)

---

*Connecting learners with mentors* 🎓
