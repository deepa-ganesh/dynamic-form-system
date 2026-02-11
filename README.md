# Dynamic Versioned Form Management System

A comprehensive 3-tier enterprise application for managing dynamic forms with automatic versioning, schema evolution, and backward compatibility. Built with Spring Boot for financial services with strict audit and compliance requirements.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

This system enables organizations to manage complex order forms with:
- **Dynamic field types** (multi-valued fields, nested sub-forms, inline tables)
- **Immutable versioning** (every save creates a new version)
- **Schema evolution** (change form structure without data migration)
- **Backward compatibility** (historical data rendered with original schemas)
- **Audit compliance** (complete change history)

**Use Case:** Order management system where users create and modify orders with complex nested data structures, requiring full audit trails for regulatory compliance.

---

## ✨ Key Features

### Version Management
- ✅ **Automatic versioning** - Every save creates a new immutable version
- ✅ **Version history** - View and compare all versions of an order
- ✅ **Work-in-progress tracking** - Draft versions auto-saved separately
- ✅ **Daily purge** - Automatic cleanup of old WIP versions

### Schema Evolution
- ✅ **Dynamic schemas** - Forms defined by JSON schemas stored in database
- ✅ **Backward compatibility** - Old data rendered with original schema
- ✅ **No data migration** - Schema changes don't require data updates
- ✅ **Multi-version support** - Multiple schema versions active simultaneously

### Data Integration
- ✅ **Dimensional table integration** - Populate forms from legacy SQL tables
- ✅ **Denormalized snapshots** - Preserve data as it was at creation time
- ✅ **Field mapping registry** - Configure transformations declaratively

### Enterprise Features
- ✅ **Spring Security** - JWT authentication with role-based access
- ✅ **Redis caching** - High-performance schema and lookup caching
- ✅ **Spring Batch** - Scheduled purge jobs
- ✅ **REST API** - Full CRUD operations with OpenAPI documentation
- ✅ **Comprehensive testing** - Unit and integration tests with Testcontainers

---

## 🛠️ Tech Stack

### Backend
- **Java 17** (LTS)
- **Spring Boot 3.2.2**
- **Spring Data MongoDB** - JSON document storage
- **Spring Data JPA** - Schema and dimensional table management
- **Spring Security 6** - Authentication and authorization
- **Spring Batch 5** - Scheduled purge jobs
- **Spring Cache** - Redis caching layer

### Databases
- **MongoDB 7+** - Versioned order documents
- **PostgreSQL 15+** - Form schemas and dimensional tables
- **Redis 7+** - Caching layer

### Build & Testing
- **Maven 3.9+** - Build and dependency management
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Testcontainers** - Integration testing
- **Lombok** - Reduce boilerplate
- **MapStruct** - Object mapping

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│      PRESENTATION LAYER (React)         │
│   Dynamic Form Renderer | Version Viewer│
└──────────────────┬──────────────────────┘
                   │ REST API
┌──────────────────▼──────────────────────┐
│         SERVICE LAYER (Spring Boot)     │
│  Version Orchestration | Schema Mgmt    │
│  Validation | Transformation | Purge    │
└──────────────────┬──────────────────────┘
                   │
       ┌───────────┴──────────┐
       ▼                      ▼
┌─────────────┐        ┌─────────────┐
│  MongoDB    │        │ PostgreSQL  │
│  (Orders)   │        │ (Schemas)   │
└─────────────┘        └─────────────┘
```

**For detailed architecture diagrams, see [docs/ARCHITECTURE-DIAGRAM.md](docs/ARCHITECTURE-DIAGRAM.md)**

---

## 📦 Prerequisites

### Required Software
- **JDK 17 or 21** - [Download](https://adoptium.net/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop/)
- **Git** - [Download](https://git-scm.com/downloads)

### Optional
- **IntelliJ IDEA** (recommended IDE) - [Download](https://www.jetbrains.com/idea/)
- **Postman** (API testing) - [Download](https://www.postman.com/downloads/)

### Verify Installation
```bash
# Check Java version
java -version
# Output: openjdk version "17.0.x" or "21.0.x"

# Check Maven version
mvn -version
# Output: Apache Maven 3.9.x

# Check Docker version
docker --version
# Output: Docker version 24.x or higher
```

---

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/dynamic-form-system.git
cd dynamic-form-system
```

### 2. Start Databases with Docker
```bash
# Start MongoDB
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  mongo:7

# Start PostgreSQL
docker run -d \
  --name postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=form_schema \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15

# Start Redis
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis:7

# Verify all containers are running
docker ps
```

### 3. Build the Project
```bash
# Build all modules
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests
```

### 4. Run the Application
```bash
# Run main service
cd form-service
mvn spring-boot:run

# Or run the JAR directly
java -jar target/form-service-1.0.0.jar
```

### 5. Verify Application is Running
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected output:
# {"status":"UP"}
```

---

## 📂 Project Structure

```
dynamic-form-system/
├── AGENTS.md                    # AI agent coding guidelines
├── README.md                    # This file
├── pom.xml                      # Parent Maven POM
│
├── docs/                        # Documentation
│   ├── REQUIREMENTS.md          # Business requirements
│   ├── DATA-MODEL-DESIGN.md     # Database design
│   └── ARCHITECTURE-DIAGRAM.md  # Architecture diagrams
│
├── form-common/                 # Shared module
│   ├── pom.xml
│   └── src/main/java/com/morganstanley/form/common/
│       ├── dto/                 # Data Transfer Objects
│       ├── enums/               # Enums (OrderStatus, etc.)
│       ├── exception/           # Custom exceptions
│       └── util/                # Utility classes
│
├── form-service/                # Main application
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/morganstanley/form/
│       │   │   ├── FormApplication.java      # Main class
│       │   │   ├── config/                   # Configuration
│       │   │   ├── controller/               # REST Controllers
│       │   │   ├── service/                  # Business Logic
│       │   │   ├── repository/               # Data Access
│       │   │   │   ├── mongo/
│       │   │   │   └── postgres/
│       │   │   ├── entity/                   # Domain entities
│       │   │   │   ├── mongo/
│       │   │   │   └── postgres/
│       │   │   ├── mapper/                   # MapStruct mappers
│       │   │   ├── security/                 # Security components
│       │   │   └── exception/                # Exception handlers
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── application-prod.yml
│       └── test/                             # Tests
│           └── java/com/morganstanley/form/
│
└── form-batch/                  # Batch jobs
    ├── pom.xml
    └── src/main/java/com/morganstanley/form/batch/
        ├── BatchApplication.java
        ├── config/              # Batch configuration
        └── job/                 # Purge job
```

---

## 🎮 Running the Application

### Development Mode

```bash
# Terminal 1: Run main service
cd form-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2: Run batch service (optional)
cd form-batch
mvn spring-boot:run

# Application will start on http://localhost:8080
```

### Production Mode

```bash
# Build production JAR
mvn clean package -Pprod

# Run with production profile
java -jar form-service/target/form-service-1.0.0.jar \
  --spring.profiles.active=prod
```

### Using Docker Compose (Future Enhancement)

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down
```

---

## 📚 API Documentation

### Swagger UI (OpenAPI)
Once the application is running, access interactive API documentation at:

**http://localhost:8080/swagger-ui.html**

### Key Endpoints

#### Order Management
```bash
# Create new order (auto-save as WIP)
POST /api/v1/orders
Content-Type: application/json
{
  "orderId": "ORD-12345",
  "deliveryLocations": ["Location A", "Location B"],
  "data": { ... },
  "finalSave": false
}

# Get latest version of order
GET /api/v1/orders/{orderId}

# Get all versions of order
GET /api/v1/orders/{orderId}/versions

# Get specific version
GET /api/v1/orders/{orderId}/versions/{versionNumber}
```

#### Schema Management
```bash
# Get active schema
GET /api/v1/schemas/active

# Get schema by version
GET /api/v1/schemas/{formVersionId}

# Create new schema (Admin only)
POST /api/v1/schemas

# Activate schema version (Admin only)
PUT /api/v1/schemas/{formVersionId}/activate
```

### Example Request/Response

**Create Order Request:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "orderId": "ORD-12345",
    "deliveryLocations": ["New York", "Boston"],
    "data": {
      "deliveryCompany": {
        "companyId": "DC-789",
        "name": "FastShip Logistics"
      },
      "items": [
        {
          "itemNumber": "ITEM-001",
          "itemName": "Widget A",
          "quantity": 10,
          "price": 25.50
        }
      ]
    },
    "finalSave": false
  }'
```

**Response:**
```json
{
  "orderId": "ORD-12345",
  "orderVersionNumber": 1,
  "formVersionId": "v2.1.0",
  "orderStatus": "WIP",
  "userName": "deepa.ganesh@morganstanley.com",
  "timestamp": "2026-02-11T10:30:00Z",
  "isLatestVersion": true,
  "data": { ... }
}
```

---

## 🧪 Testing

### Run All Tests
```bash
# Run all tests with coverage
mvn clean test

# View coverage report
open target/site/jacoco/index.html
```

### Run Unit Tests Only
```bash
mvn test -Dgroups=unit
```

### Run Integration Tests Only
```bash
mvn test -Dgroups=integration
```

### Run Specific Test Class
```bash
mvn test -Dtest=VersionOrchestrationServiceTest
```

### Test Coverage Requirements
- ✅ Minimum **80% code coverage**
- ✅ All public service methods must have unit tests
- ✅ Critical paths must have integration tests
- ✅ Use Testcontainers for real database testing

---

## 📖 Documentation

### Core Documentation
- **[REQUIREMENTS.md](docs/REQUIREMENTS.md)** - Business requirements and functional specifications
- **[DATA-MODEL-DESIGN.md](docs/DATA-MODEL-DESIGN.md)** - Database design, entities, and data flows
- **[ARCHITECTURE-DIAGRAM.md](docs/ARCHITECTURE-DIAGRAM.md)** - System architecture and component diagrams
- **[AGENTS.md](AGENTS.md)** - Coding standards and guidelines for AI agents

### Additional Resources
- **Swagger/OpenAPI** - http://localhost:8080/swagger-ui.html (when running)
- **Actuator Endpoints** - http://localhost:8080/actuator (when running)

---

## 🤝 Contributing

### For AI Agents
If you are an AI coding agent (Cursor, Copilot, etc.), please read **[AGENTS.md](AGENTS.md)** for coding standards and conventions.

### Code Style
- Follow Spring Boot best practices
- Use Lombok to reduce boilerplate
- Constructor injection (not field injection)
- Write tests for all new features
- 80%+ code coverage required

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** feat, fix, docs, style, refactor, test, chore

**Example:**
```
feat(order): add version comparison endpoint

Implement REST endpoint to compare two versions of an order.
Returns field-level differences.

Closes #123
```

---

## 📄 License

This project is proprietary software developed for Morgan Stanley.

**Copyright © 2026 Morgan Stanley. All rights reserved.**

---

## 👥 Team

**Project Owner:** Deepa Ganesh  
**Email:** deepa.ganesh@morganstanley.com  
**Interview Date:** February 11, 2026, 7:00 PM

---

## 🔗 Quick Links

- **Local API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics

---

## 📝 Notes

### Version Management
- Every save creates a new version (immutable)
- WIP versions are auto-saved drafts
- Committed versions are final and permanent
- Daily purge job keeps only latest WIP per order

### Schema Evolution
- Form schemas are versioned separately from data
- Old data is always rendered with its original schema
- No data migration required when schema changes
- Multiple schema versions can coexist

### Performance
- Redis caching for schemas and lookups
- MongoDB indexes on composite keys
- Lightweight version index for fast queries
- Connection pooling for all databases

---