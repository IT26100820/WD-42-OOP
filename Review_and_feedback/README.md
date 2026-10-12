# Online Bookstore Management System — Review and Feedback Module

> **Component:** Review and Feedback Management  
> **Course:** Object-Oriented Programming (OOP)  
> **Framework & Language:** Java 11 (LTS) &bull; Spring Boot 2.7.18 &bull; Embedded Tomcat &bull; HTML5 / CSS3 / JavaScript  
> **Persistence:** Thread-safe, atomic JSON file persistence (`data/reviews.json`)  
> **Testing Suite:** JUnit 5 Jupiter, Mockito, AssertJ, Spring MockMvc (29 automated unit & integration tests)

---

## Table of Contents
1. [Module Overview & Features](#1-module-overview--features)
2. [Architecture & Layered Design](#2-architecture--layered-design)
3. [OOP Concepts Demonstration (Viva Core)](#3-oop-concepts-demonstration-viva-core)
4. [File Handling & Concurrency Safety](#4-file-handling--concurrency-safety)
5. [Data Model & JSON Format](#5-data-model--json-format)
6. [REST API Documentation & HTTP Status Codes](#6-rest-api-documentation--http-status-codes)
7. [Validation Rules & Error Handling](#7-validation-rules--error-handling)
8. [Frontend Interfaces & Dedicated Backend Testing UI](#8-frontend-interfaces--dedicated-backend-testing-ui)
9. [Step-by-Step Viva Demonstration Checklist](#9-step-by-step-viva-demonstration-checklist)
10. [How to Run, Build, and Test](#10-how-to-run-build-and-test)
11. [Teammate Integration Guide (Book & Customer Modules)](#11-teammate-integration-guide)
12. [Suggested Git Commit History](#12-suggested-git-commit-history)
13. [Viva Questions & Beginner-Friendly Answers](#13-viva-questions--beginner-friendly-answers)

---

## 1. Module Overview & Features

The **Review and Feedback Management Module** allows bookstore customers to rate and review purchased books, browse community feedback, filter and search reviews, and view aggregated book statistics. It can run as a **completely independent standalone application** with sample books and customers for testing, and is architected to seamlessly plug into teammates' Book and User modules upon final integration.

### Key Capabilities:
- **Complete CRUD Operations:**
  - **Create:** Submit ratings (1–5 stars) and comments (up to 1,000 characters). Auto-generates unique IDs (`REV-1001`, `REV-1002`, etc.).
  - **Read:** Display all reviews, reviews by book ID, single review by ID, and calculate book rating summaries (average score and star distribution).
  - **Update:** Modify ratings and comments while preserving review ID, book ID, customer identity, and original creation timestamp.
  - **Delete:** Remove reviews with modal confirmation in UI and permission verification.
- **Search, Filter & Sort:**
  - Filter reviews by Book ID or specific Star Rating (e.g. 5 Stars only).
  - Real-time keyword search inside comments (case-insensitive).
  - Sort by Newest First, Oldest First, Highest Rating, or Lowest Rating.
- **Dual User Interfaces:**
  - **Customer Bookstore UI (`http://localhost:8080/`):** Responsive online bookstore page with dynamic star rating picker, character counter, live summary cards, and user switcher.
  - **Dedicated Backend Testing Dashboard (`http://localhost:8080/review-test`):** Interactive developer workbench displaying live server health, raw HTTP status badges, latency telemetry, raw JSON payload viewer, and **one-click deliberate validation failure scenarios** for viva defense.
- **Thread-Safe, Resilient Persistence:**
  - Saves records to `data/reviews.json`.
  - Employs `ReentrantReadWriteLock` for multi-threaded read/write safety.
  - Uses atomic temporary file writes to prevent data loss or corruption during unexpected system crashes.

---

## 2. Architecture & Layered Design

The application follows the industry-standard **Layered Architecture (Separation of Concerns)**:

```
[ Browser / Customer UI ]       [ Backend Tester Dashboard (/review-test) ]
            │                                      │
            └──────────────────┬───────────────────┘
                               │ HTTP / JSON REST Requests
                               ▼
                    [ ReviewController ]
                    (Handles HTTP requests, status codes, query params)
                               │
                               ▼
                    [ ReviewService ] (Interface)
                    [ ReviewServiceImpl ] (Class)
                    (Enforces business rules, duplicate prevention, ownership)
                               │
                               ▼
                    [ ReviewRepository ] (Interface)
                    [ JsonFileReviewRepository ] (Class)
                    (Thread-safe atomic read/write against data/reviews.json)
                               │
                               ▼
                     [ data/reviews.json ]
```

### Class Responsibilities:

| Layer | Class / Interface | Responsibility |
|---|---|---|
| **Model** | `Review.java` | Encapsulates review state (`reviewId`, `bookId`, `customerId`, `rating`, `comment`, `createdAt`, `updatedAt`). Enforces domain validation. |
| **DTO** | `ReviewRequest.java` | Validates incoming payloads using Jakarta Bean Validation (`@NotBlank`, `@Min`, `@Max`, `@Size`). |
| **DTO** | `ApiResponse.java` | Uniform JSON response envelope containing `success`, `message`, `data`, `fieldErrors`, and `timestamp`. |
| **DTO** | `BookReviewSummary.java` | Carries aggregated statistics (average rating, total count, star breakdown). |
| **Repository** | `ReviewRepository.java` | Interface defining data access abstraction (`findAll`, `findById`, `save`, `deleteById`, etc.). |
| **Repository** | `JsonFileReviewRepository.java` | Concrete implementation managing `data/reviews.json` with file locking and atomic file replacement. |
| **Service** | `ReviewService.java` | Interface defining business logic contracts. |
| **Service** | `ReviewServiceImpl.java` | Enforces business rules: 1 review per customer per book, authorization checks, search, and sorting. |
| **Controller** | `ReviewController.java` | REST API endpoints under `/api/reviews`. Converts HTTP requests and returns standardized `ResponseEntity`. |
| **Controller** | `PageController.java` | Routes root URL `/` and `/review-test` to static frontend HTML pages. |
| **Exception** | `GlobalExceptionHandler.java` | Intercepts all exceptions (`@RestControllerAdvice`) and transforms them into secure, readable JSON errors without leaking stack traces. |

---

## 3. OOP Concepts Demonstration (Viva Core)

During your viva examination, use these concrete code references to defend your understanding of OOP principles:

### A. Encapsulation
- **What it is:** Bundling data (state) and methods (behavior) together while restricting direct access to object internals.
- **Where in code:** In `Review.java`, all fields (`reviewId`, `rating`, `comment`, etc.) are declared `private`. They cannot be modified arbitrarily from the outside.
- **Invariant Protection:** Mutating review state is controlled via `review.updateContent(newRating, newComment)`, which validates boundaries before assigning values and automatically updates `updatedAt`. Invalid objects can never exist in memory.

### B. Abstraction
- **What it is:** Exposing essential contracts while hiding implementation complexity.
- **Where in code:** 
  - `ReviewRepository` exposes high-level CRUD methods (`save`, `findById`, `deleteById`). The service layer does **not know or care** whether reviews are stored in a JSON file, a text file, MySQL, or MongoDB.
  - `ReviewService` abstracts business operations from HTTP web controllers.

### C. Polymorphism
- **What it is:** The ability of different classes to be treated as instances of the same interface or superclass.
- **Where in code:**
  - `JsonFileReviewRepository implements ReviewRepository`. If the group decides to switch from JSON file storage to a relational database later, a new class `JpaReviewRepository implements ReviewRepository` can be plugged in with **zero modifications** to `ReviewServiceImpl`.
  - Spring's dependency injection injects the `ReviewRepository` interface dynamically at runtime.

### D. Information Hiding
- **What it is:** Hiding internal mechanisms, file structures, and error internals from outside consumers.
- **Where in code:**
  - Concurrency locks (`ReentrantReadWriteLock`), atomic file swapping (`Files.move`), and Jackson serialization are private details tucked away inside `JsonFileReviewRepository`.
  - `GlobalExceptionHandler` hides sensitive server stack traces, database details, and server file paths, returning only user-safe JSON error messages.

### E. Inheritance
- **What it is:** Deriving specialized classes from a common base class.
- **Where in code:** Custom exception hierarchy:
  - `ReviewException extends RuntimeException` (Base)
    - `ResourceNotFoundException extends ReviewException` (HTTP 404)
    - `DuplicateReviewException extends ReviewException` (HTTP 409)
    - `UnauthorizedReviewException extends ReviewException` (HTTP 403)
    - `StorageException extends ReviewException` (HTTP 500)

---

## 4. File Handling & Concurrency Safety

A major assessment requirement is robust file persistence. `JsonFileReviewRepository` implements industry-grade file handling techniques:

1. **Automatic Directory and File Initialization:**
   - On startup (`@PostConstruct`), checks if `data/` exists. If missing, it creates the folder and `data/reviews.json` initialized with `[]`.
2. **Concurrency Protection (`ReentrantReadWriteLock`):**
   - Multiple threads can read simultaneously using `rwLock.readLock()`.
   - Write and delete operations obtain `rwLock.writeLock()`, ensuring no dirty reads or corrupted state during modifications.
3. **Atomic File Writes (Crash Resilience):**
   - Writing directly to an active file risks file truncation if power is lost mid-write.
   - Instead, the repository writes data to a temporary file (`data/reviews.json.tmp`) first, flushes it, and then executes an **atomic file move** (`StandardCopyOption.ATOMIC_MOVE`, `StandardCopyOption.REPLACE_EXISTING`).
4. **Collision-Free ID Generation:**
   - Evaluates the highest existing numeric ID in the dataset (e.g. `REV-1005` &rarr; next is `REV-1006`), guaranteeing that IDs are monotonically increasing and never collide across restarts.

---

## 5. Data Model & JSON Format

Records are stored in `data/reviews.json`:

```json
[
  {
    "reviewId": "REV-1001",
    "bookId": "BK-101",
    "customerId": "CUST-001",
    "rating": 5,
    "comment": "An absolute masterpiece for any software engineer. It completely changed the way I write methods and structure classes.",
    "createdAt": "2026-10-01T09:30:00",
    "updatedAt": "2026-10-01T09:30:00"
  }
]
```

### Field Definitions:
- `reviewId`: Unique identifier (String, e.g. `REV-1001`). Generated by backend; cannot be supplied or modified by client.
- `bookId`: Identifier of the reviewed book (String, e.g. `BK-101`).
- `customerId`: Identifier of the customer author (String, e.g. `CUST-001`).
- `rating`: Whole number integer between 1 and 5 (inclusive).
- `comment`: Feedback text (String, 1 to 1000 characters, trimmed of whitespace).
- `createdAt`: ISO-8601 timestamp representing creation time (immutable on update).
- `updatedAt`: ISO-8601 timestamp representing last edit time.

---

## 6. REST API Documentation & HTTP Status Codes

All responses follow the unified envelope:
```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "fieldErrors": { "fieldName": "Error explanation" },
  "timestamp": "2026-10-10T13:30:00"
}
```

### Endpoints Table:

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/reviews` | List all reviews with optional `bookId`, `rating`, `search`, and `sortBy` query params | `200 OK` |
| `GET` | `/api/reviews/{id}` | Retrieve single review by unique ID | `200 OK` / `404 NOT FOUND` |
| `GET` | `/api/reviews/book/{bookId}` | Retrieve all reviews for a specific book | `200 OK` |
| `GET` | `/api/reviews/book/{bookId}/summary` | Retrieve average rating, count, and star distribution | `200 OK` |
| `POST` | `/api/reviews` | Create a new review (Validates body & duplicate rules) | `201 CREATED` / `400 BAD REQUEST` / `409 CONFLICT` |
| `PUT` | `/api/reviews/{id}` | Update review rating and comment (Enforces ownership) | `200 OK` / `400 BAD REQUEST` / `403 FORBIDDEN` / `404 NOT FOUND` |
| `DELETE` | `/api/reviews/{id}` | Delete review by ID (Enforces ownership) | `200 OK` / `403 FORBIDDEN` / `404 NOT FOUND` |
| `GET` | `/api/reviews/health` | Health check endpoint returning `{ status: "UP" }` | `200 OK` |
| `GET` | `/api/reviews/sample-data` | Retrieves predefined sample books and customers | `200 OK` |

---

## 7. Validation Rules & Error Handling

### Backend Validation Enforcements:
1. **Rating:**
   - Must not be null.
   - Must be an integer between 1 and 5.
   - Rejects decimals, 0, negative values, and strings with HTTP 400.
2. **Comment:**
   - Must not be blank or only whitespace.
   - Automatically trimmed of leading and trailing whitespace.
   - Enforces a maximum length of 1,000 characters.
3. **Duplicate Review Rule:**
   - Enforces **one review per customer per book**.
   - If customer `CUST-001` attempts to review `BK-101` a second time, the service throws `DuplicateReviewException`, returning HTTP 409 Conflict.
4. **Ownership & Permissions:**
   - A customer can only edit or delete their own review.
   - If `CUST-002` attempts to modify a review created by `CUST-001`, the service throws `UnauthorizedReviewException`, returning HTTP 403 Forbidden.
   - System administrator (`admin`) is granted moderation override.

---

## 8. Frontend Interfaces & Dedicated Backend Testing UI

### 1. Customer Bookstore UI (`http://localhost:8080/`)
- Accessible 5-star interactive rating component with hover preview and keyboard accessibility.
- Comment textarea with real-time character counter (`0 / 1000`).
- Book selector showing book title, author, ID, and price.
- Live rating breakdown bars displaying score percentages.
- Edit review mode and delete confirmation modal.
- Active search bar with debounced query execution.

### 2. Dedicated Backend Testing Dashboard (`http://localhost:8080/review-test`)
Designed specifically for your viva demonstration:
- **Backend Connection Badge:** Live ping status indicator (`🟢 Backend Live (HTTP 200)`).
- **Interactive CRUD Control Panels:** Dedicated tabs to execute POST, GET, PUT, and DELETE.
- **Live Response Inspector:** Displays the actual HTTP method, URL, roundtrip latency (ms), HTTP status badge, and formatted JSON response returned by the backend.
- **Persistent Storage Records Table:** Displays all items currently in `data/reviews.json` with direct "Inspect", "Edit", and "Delete" actions.
- **🧪 Deliberate Validation Failure Buttons:** Pre-wired buttons that trigger deliberate invalid requests (Rating 6, Duplicate review, Nonexistent ID, etc.) to prove that backend validation operates securely.

---

## 9. Step-by-Step Viva Demonstration Checklist

Follow this exact sequence to demonstrate your module to examiners:

1. **Start the Application:**
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
2. **Open the Testing Dashboard:**
   - Open browser to `http://localhost:8080/review-test`.
   - Point out the green **"Backend Live (HTTP 200)"** badge.
3. **Step 1 — Create a Review:**
   - On Tab 1 ("Create Review"), select `BK-102`, `CUST-004`, Rating `5`, enter a comment, and click **Send POST /api/reviews**.
   - Highlight the green **`HTTP 201 CREATED`** status code in the Live Response Inspector.
4. **Step 2 — Verify File Persistence:**
   - Show that the new review immediately appears in the persistent table below with a newly generated ID (e.g. `REV-1006`).
   - Open `data/reviews.json` in IntelliJ to prove the record was written to disk.
5. **Step 3 — Retrieve by ID:**
   - On Tab 2 ("Read & Filter"), enter the new ID and click **Send GET /api/reviews/{id}**.
   - Point out the **`HTTP 200 OK`** response and matching fields.
6. **Step 4 — Update Review:**
   - On Tab 3 ("Update"), change rating to `4` and modify the comment text. Click **Send PUT /api/reviews/{id}**.
   - Show that the rating updated, `updatedAt` refreshed, and `createdAt` was preserved.
7. **Step 5 — Delete Review:**
   - On Tab 4 ("Delete"), enter the ID and click **Send DELETE /api/reviews/{id}**.
   - Show that it disappears from the table and attempting to fetch it returns **`HTTP 404 NOT FOUND`**.
8. **Step 6 — Demonstrate Validation (Viva Highlight):**
   - In the purple **Deliberate Validation Test Scenarios** box, click:
     - **Rating = 6:** Point out **`HTTP 400 BAD REQUEST`** with field error `"Rating must be a whole number between 1 and 5."`
     - **Duplicate Review:** Point out **`HTTP 409 CONFLICT`** with message `"Customer has already submitted a review for book..."`
     - **Unauthorized Edit:** Point out **`HTTP 403 FORBIDDEN`** with message `"You do not have permission to edit this review."`
9. **Step 7 — Demonstrate Customer UI:**
   - Click "Open Customer UI" (`http://localhost:8080/`) to show that reviews, star filters, and statistics match the persistent backend.

---

## 10. How to Run, Build, and Test

### Prerequisites:
- Java 11 JDK or higher installed.
- (Maven is **not required** on your system; the included `mvnw.cmd` wrapper handles everything automatically).

### Run Application:
```powershell
.\mvnw.cmd spring-boot:run
```
Or run `ReviewApplication.java` directly in IntelliJ IDEA.

### Run Automated Tests:
```powershell
.\mvnw.cmd test
```
*Executes all 29 unit and integration tests (model invariants, repository isolation, service rules, and MockMvc endpoints).*

### Package Executable JAR:
```powershell
.\mvnw.cmd clean package
java -jar target/review-management-1.0.0.jar
```

---

## 11. Teammate Integration Guide

When merging into the shared group repository:

### 1. Connecting with Book Management Module:
- Teammate's Book Module provides `bookId` (e.g. `BK-101`).
- The Review Module currently exposes `GET /api/reviews/book/{bookId}` and `GET /api/reviews/book/{bookId}/summary`.
- When a user views a book on the catalog page, teammates can make a fetch request to:
  ```javascript
  const res = await fetch(`/api/reviews/book/${bookId}/summary`);
  const data = await res.json();
  // data.data.averageRating -> 4.8
  // data.data.totalReviews  -> 12
  ```

### 2. Connecting with Customer / User Management Module:
- When a user logs in, teammate's authentication module provides the authenticated user session.
- Pass the logged-in customer's ID in the `X-Customer-Id` HTTP header or in the request body `customerId`.
- The Review Module automatically enforces that the customer can only edit or delete reviews matching their ID.

---

## 12. Suggested Git Commit History

Use these clean, descriptive commit messages when committing your work to GitHub:

1. `feat(project): initialize Spring Boot 2.7 with Maven Wrapper on Java 11`
2. `feat(model): implement Review domain model with encapsulation and boundary validation`
3. `feat(persistence): add thread-safe JsonFileReviewRepository with atomic file writes`
4. `feat(service): implement ReviewService with duplicate review check and sorting`
5. `feat(controller): expose REST API endpoints with GlobalExceptionHandler and DTOs`
6. `feat(frontend): create bookstore customer review UI with interactive 5-star rating`
7. `feat(testing-ui): build dedicated backend testing dashboard with live inspector and validation triggers`
8. `test: add comprehensive JUnit 5 and MockMvc test suite (29 tests passing)`
9. `docs: add comprehensive README with viva defense guide and architectural diagrams`

---

## 13. Viva Questions & Beginner-Friendly Answers

### Q1: Why did you choose Spring Boot over traditional Servlets & JSP?
> **Answer:** "Spring Boot provides an industry-standard, clean layered architecture with embedded Tomcat, eliminating the need to manually configure external servlet containers. It offers native JSON serialization with Jackson, automated input validation via Jakarta annotations, and allows a completely decoupled frontend (HTML/CSS/JS) to communicate over standard REST APIs. Furthermore, using interfaces ensures that if our group switches from file storage to a database, only the repository implementation changes while the service and controllers remain untouched."

### Q2: How did you implement Encapsulation in your Review class?
> **Answer:** "I declared all fields (`reviewId`, `rating`, `comment`, timestamps) as `private`. External classes cannot directly assign illegal values. Mutations are governed by controlled methods like `updateContent(int rating, String comment)`, which validate that the rating is between 1 and 5 and that the comment is not blank before modifying state, while automatically maintaining the `updatedAt` timestamp."

### Q3: How do you prevent file corruption if the application crashes during a write?
> **Answer:** "I implemented two safety mechanisms in `JsonFileReviewRepository`: first, a `ReentrantReadWriteLock` to prevent simultaneous thread collisions. Second, **atomic file replacement**: rather than writing directly into `data/reviews.json`, the data is written to a temporary file (`reviews.json.tmp`) first. Once completely flushed to disk, `Files.move()` atomically replaces the target file using `StandardCopyOption.ATOMIC_MOVE`. If a crash occurs during writing, the original file remains uncorrupted."

### Q4: How is duplicate review prevention implemented?
> **Answer:** "In `ReviewServiceImpl.java`, the `createReview` method queries `reviewRepository.existsByBookIdAndCustomerId(bookId, customerId)`. If a match is found, it throws a custom `DuplicateReviewException`. The `GlobalExceptionHandler` intercepts this exception and translates it into an `HTTP 409 Conflict` status code with a descriptive error message."

### Q5: What is the purpose of the `/review-test` page?
> **Answer:** "The `/review-test` dashboard is an independent verification workbench. It directly calls the Spring Boot REST API and displays genuine HTTP status codes (`201`, `400`, `404`, `409`), request latencies, and raw JSON payloads. It also includes deliberate test buttons to demonstrate to examiners that backend validation properly rejects out-of-range ratings, empty comments, duplicate submissions, and unauthorized modifications."
