# AGENTS.md

**Project:** Dynamic Versioned Form Management System  
**Last Updated:** February 11, 2026

---

## 📋 PROJECT OVERVIEW

This is a **3-tier enterprise application** for managing dynamic forms with automatic versioning, schema evolution, and backward compatibility. Built for financial services with strict audit and compliance requirements.

**Key Features:**
- Dynamic form rendering with multi-valued fields, tables, and sub-forms
- Immutable version control (every save creates a new version)
- Schema evolution without data migration
- Integration with legacy dimensional tables
- Automated end-of-day purge of work-in-progress versions

**Use Case:** Order management system where users create/edit orders with complex nested data structures.

---

## 🛠️ TECH STACK

### Backend (Primary Focus)
- **Language:** Java 17+ (LTS) or Java 21
- **Framework:** Spring Boot 3.2+
- **Build Tool:** Maven 3.9+
- **Dependency Management:**
  - Spring Data MongoDB
  - Spring Data JPA
  - Spring Security 6+
  - Spring Batch 5+
  - Spring Cache (Redis)
- **Databases:**
  - MongoDB 7+ (JSON documents)
  - PostgreSQL 15+ (schemas + dimensional tables)
  - Redis 7+ (caching)
- **Testing:**
  - JUnit 5
  - Mockito
  - Testcontainers
- **Utilities:**
  - Lombok (reduce boilerplate)
  - MapStruct (object mapping)
  - Jackson (JSON processing)

### Frontend
- **Framework:** React 18+ with TypeScript
- **UI Library:** Material-UI (MUI) v5
- **State Management:** Redux Toolkit
- **Build Tool:** Vite

---

## 📁 PROJECT STRUCTURE

```
dynamic-form-system/
├── pom.xml                           # Parent POM
│
├── form-common/                      # Shared module
│   ├── src/main/java/com/dynamicform/form/common/
│   │   ├── model/                    # Domain models
│   │   ├── dto/                      # Data Transfer Objects
│   │   ├── enums/                    # Enums (OrderStatus, FieldType)
│   │   ├── exception/                # Custom exceptions
│   │   └── util/                     # Utility classes
│
├── form-service/                     # Main backend application
│   ├── src/main/java/com/dynamicform/form/
│   │   ├── FormApplication.java      # Main class
│   │   ├── config/                   # Configuration
│   │   │   ├── MongoConfig.java
│   │   │   ├── PostgresConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/               # REST Controllers
│   │   │   ├── OrderController.java
│   │   │   ├── SchemaController.java
│   │   │   └── VersionController.java
│   │   ├── service/                  # Business Logic
│   │   │   ├── VersionOrchestrationService.java
│   │   │   ├── SchemaManagementService.java
│   │   │   ├── ValidationService.java
│   │   │   ├── DataTransformationService.java
│   │   │   └── impl/
│   │   ├── repository/               # Data Access
│   │   │   ├── mongo/
│   │   │   │   ├── OrderVersionedRepository.java
│   │   │   │   └── OrderVersionIndexRepository.java
│   │   │   └── postgres/
│   │   │       ├── FormSchemaRepository.java
│   │   │       └── FieldMappingRepository.java
│   │   ├── entity/                   # Database entities
│   │   │   ├── mongo/
│   │   │   │   ├── OrderVersionedDocument.java
│   │   │   │   └── OrderVersionIndex.java
│   │   │   └── postgres/
│   │   │       ├── FormSchemaEntity.java
│   │   │       └── FieldMappingEntity.java
│   │   ├── mapper/                   # MapStruct mappers
│   │   ├── security/                 # Security components
│   │   └── exception/                # Exception handlers
│   │
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
│
├── form-batch/                       # Batch jobs
│   └── src/main/java/com/dynamicform/form/batch/
│       ├── BatchApplication.java
│       ├── config/
│       │   └── PurgeJobConfig.java
│       └── job/
│           ├── PurgeTasklet.java
│           └── PurgeJobScheduler.java
│
├── form-client/                      # Frontend (React)
│   ├── src/
│   │   ├── components/
│   │   ├── services/
│   │   └── store/
│   └── package.json
│
└── docs/
    ├── REQUIREMENTS.md
    ├── DATA-MODEL-DESIGN.md
    ├── ARCHITECTURE-DIAGRAM.md
    └── AGENTS.md (this file)
```

---

## 🎯 NAMING CONVENTIONS

### Java Package Naming
```
com.dynamicform.form.{layer}
```

**Examples:**
- `com.dynamicform.form.controller`
- `com.dynamicform.form.service`
- `com.dynamicform.form.repository.mongo`
- `com.dynamicform.form.entity.postgres`

### Class Naming

| Type | Pattern | Example |
|------|---------|---------|
| **Controller** | `{Entity}Controller` | `OrderController` |
| **Service** | `{Entity}Service` | `VersionOrchestrationService` |
| **Repository** | `{Entity}Repository` | `OrderVersionedRepository` |
| **Entity (MongoDB)** | `{Entity}Document` | `OrderVersionedDocument` |
| **Entity (JPA)** | `{Entity}Entity` | `FormSchemaEntity` |
| **DTO** | `{Entity}{Action}DTO` or `{Entity}Request/Response` | `CreateOrderRequest`, `OrderVersionResponse` |
| **Mapper** | `{Entity}Mapper` | `OrderMapper` |
| **Exception** | `{Description}Exception` | `SchemaNotFoundException` |
| **Config** | `{Technology}Config` | `MongoConfig`, `SecurityConfig` |

### Method Naming

| Action | Prefix | Example |
|--------|--------|---------|
| Create/Save | `create`, `save` | `createNewVersion()` |
| Retrieve | `get`, `find` | `getLatestVersion()`, `findByOrderId()` |
| Update | `update`, `modify` | `updateStatus()` |
| Delete | `delete`, `remove` | `deleteWipVersions()` |
| Boolean check | `is`, `has`, `can` | `isLatestVersion()`, `hasPermission()` |
| Validation | `validate` | `validateOrderData()` |
| Transformation | `transform`, `convert` | `transformToJson()` |

### Variable Naming
```java
// Use camelCase
String orderId;
Integer orderVersionNumber;
LocalDateTime timestamp;

// Collections: plural nouns
List<OrderVersionedDocument> orders;
Map<String, Object> fieldMappings;

// Boolean: is/has/can prefix
boolean isLatestVersion;
boolean hasChanges;
boolean canDelete;
```

### Constants
```java
// UPPER_SNAKE_CASE
public static final String ORDER_STATUS_WIP = "WIP";
public static final String ORDER_STATUS_COMMITTED = "COMMITTED";
public static final int MAX_VERSIONS_PER_ORDER = 1000;
public static final int PURGE_RETENTION_DAYS = 1;
```

---

## 🔨 CODING STANDARDS

### Use Lombok to Reduce Boilerplate

**✅ DO THIS:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVersionedDocument {
    private String orderId;
    private Integer orderVersionNumber;
    private String formVersionId;
    // ... more fields
}
```

**❌ NOT THIS:**
```java
public class OrderVersionedDocument {
    private String orderId;
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    // ... 50 lines of boilerplate
}
```

### Constructor Injection (Not Field Injection)

**✅ DO THIS:**
```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class VersionOrchestrationService {
    
    private final OrderVersionedRepository orderRepository;
    private final OrderVersionIndexRepository indexRepository;
    
    // Methods here
}
```

**❌ NOT THIS:**
```java
@Service
public class VersionOrchestrationService {
    
    @Autowired  // Field injection is bad practice
    private OrderVersionedRepository orderRepository;
}
```

### Use SLF4J for Logging

**✅ DO THIS:**
```java
@Service
@RequiredArgsConstructor
@Slf4j  // Lombok generates logger
public class VersionOrchestrationService {
    
    public Integer createNewVersion(String orderId, Map<String, Object> data) {
        log.info("Creating new version for orderId: {}", orderId);
        log.debug("Order data: {}", data);
        // ... logic
    }
}
```

**❌ NOT THIS:**
```java
System.out.println("Creating version for: " + orderId);  // Never use this!
```

### Use Optional for Nullable Results

**✅ DO THIS:**
```java
@Repository
public interface OrderVersionedRepository extends MongoRepository<OrderVersionedDocument, String> {
    
    Optional<OrderVersionedDocument> findByOrderIdAndIsLatestVersionTrue(String orderId);
}

// Usage in service:
public OrderVersionResponse getLatestVersion(String orderId) {
    return orderRepository.findByOrderIdAndIsLatestVersionTrue(orderId)
        .map(this::mapToResponse)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
}
```

### Use @Transactional for Multi-Step Operations

**✅ DO THIS:**
```java
@Transactional
public Integer createNewVersion(String orderId, Map<String, Object> data) {
    // Step 1: Get latest version
    Optional<OrderVersionIndex> latest = indexRepository.findLatest(orderId);
    
    // Step 2: Create new document
    OrderVersionedDocument newDoc = buildDocument(orderId, data, latest);
    orderRepository.save(newDoc);
    
    // Step 3: Update index
    updateIndex(newDoc);
    
    return newDoc.getOrderVersionNumber();
}
```

### Use Bean Validation

**✅ DO THIS:**
```java
@Data
@Builder
public class CreateOrderRequest {
    
    @NotBlank(message = "Order ID is required")
    @Pattern(regexp = "^ORD-[0-9]{5}$", message = "Order ID must be ORD-XXXXX format")
    private String orderId;
    
    @NotEmpty(message = "At least one delivery location required")
    @Size(min = 1, max = 10, message = "1-10 delivery locations allowed")
    private List<@NotBlank String> deliveryLocations;
    
    @Valid
    @NotNull
    private Map<String, Object> data;
}

// Controller:
@PostMapping("/orders")
public ResponseEntity<OrderVersionResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request) {
    // validation happens automatically
}
```

---

## ✅ WHAT AI AGENTS SHOULD DO

### 1. Generate Boilerplate Code

**AI can generate:**
- REST controller methods following existing patterns
- Service layer methods with business logic
- Repository interface methods (Spring Data)
- Entity classes with JPA/MongoDB annotations
- DTO classes with validation annotations
- Unit test templates with JUnit 5 + Mockito

**Example prompt:**
```
Create a REST endpoint to get version history for an order. 
Include controller, service method, and repository query.
Follow existing patterns in OrderController.
```

### 2. Write Unit Tests

**AI can generate:**
```java
@ExtendWith(MockitoExtension.class)
class VersionOrchestrationServiceTest {
    
    @Mock
    private OrderVersionedRepository orderRepository;
    
    @Mock
    private OrderVersionIndexRepository indexRepository;
    
    @InjectMocks
    private VersionOrchestrationService service;
    
    @Test
    @DisplayName("Should create version 1 for new order")
    void shouldCreateFirstVersion() {
        // Given
        String orderId = "ORD-12345";
        when(indexRepository.findByOrderIdAndIsLatestVersionTrue(orderId))
            .thenReturn(Optional.empty());
        
        // When
        Integer version = service.createNewVersion(orderId, Map.of());
        
        // Then
        assertThat(version).isEqualTo(1);
        verify(orderRepository).save(any());
    }
}
```

### 3. Refactor Code

**AI can help with:**
- Extracting repeated logic into utility methods
- Converting verbose code to Lombok
- Optimizing database queries
- Improving error handling

### 4. Generate Documentation

**AI can generate:**
- Javadoc for public methods
- Swagger/OpenAPI annotations
- README sections

**Example:**
```java
/**
 * Creates a new version of an order.
 * 
 * <p>This method determines the next version number, creates a new immutable
 * document, and updates the version index. If this is a final save, the status
 * is set to COMMITTED; otherwise, it's marked as WIP.</p>
 * 
 * @param orderId the business key of the order
 * @param formData the form field data
 * @param userName the user creating this version
 * @param isFinalSave true if this is a committed save, false for WIP
 * @return the new version number
 * @throws SchemaNotFoundException if no active schema exists
 */
@Transactional
public Integer createNewVersion(String orderId, 
                               Map<String, Object> formData,
                               String userName,
                               boolean isFinalSave) {
    // implementation
}
```

### 5. Implement CRUD Operations

**AI can generate standard patterns:**
```java
// Create
@PostMapping
public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request)

// Read
@GetMapping("/{id}")
public ResponseEntity<OrderResponse> getById(@PathVariable String id)

// Update
@PutMapping("/{id}")
public ResponseEntity<OrderResponse> update(@PathVariable String id, @Valid @RequestBody UpdateOrderRequest request)

// Delete
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id)

// List
@GetMapping
public ResponseEntity<Page<OrderResponse>> list(Pageable pageable)
```

---

## ❌ WHAT AI AGENTS SHOULD NOT DO

### 1. DO NOT Modify Core Version Logic Without Review

**❌ NEVER change without human review:**
- `VersionOrchestrationService.createNewVersion()` - Core versioning logic
- Composite key handling (`orderId` + `orderVersionNumber`)
- `isLatestVersion` flag management
- Purge logic in `PurgeTasklet`

**Reason:** These are critical business logic with audit/compliance implications.

### 2. DO NOT Change Database Indexes

**❌ NEVER add/remove indexes without:**
- Performance testing
- DBA review
- Understanding query patterns

**Example of dangerous change:**
```java
// DO NOT remove this index without analysis!
@CompoundIndex(name = "orderId_version_idx", 
               def = "{'orderId': 1, 'orderVersionNumber': 1}", 
               unique = true)
```

### 3. DO NOT Bypass Security

**❌ NEVER do this:**
```java
// DO NOT disable CSRF without security review
http.csrf().disable()

// DO NOT skip authentication
@PreAuthorize("permitAll()")  // Dangerous!

// DO NOT log sensitive data
log.info("User password: {}", password);  // NEVER!
```

### 4. DO NOT Use Deprecated Patterns

**❌ AVOID these patterns:**
```java
// Field injection (use constructor injection)
@Autowired
private SomeRepository repository;

// Old date types (use java.time.*)
Date timestamp = new Date();

// Raw types
List items = new ArrayList();

// System.out (use SLF4J)
System.out.println("Debug message");
```

### 5. DO NOT Commit Without Testing

**❌ NEVER commit code that:**
- Doesn't compile
- Fails existing tests
- Has no unit tests (for new methods)
- Contains hardcoded credentials
- Has `TODO` or `FIXME` comments unresolved

---

## 🧪 TESTING REQUIREMENTS

### Minimum Requirements
- ✅ **80% code coverage** minimum
- ✅ All public service methods must have unit tests
- ✅ Critical paths must have integration tests
- ✅ Use **Testcontainers** for integration tests

### Test Structure

**Unit Test Example:**
```java
@ExtendWith(MockitoExtension.class)
class VersionOrchestrationServiceTest {
    
    @Mock
    private OrderVersionedRepository orderRepository;
    
    @InjectMocks
    private VersionOrchestrationService service;
    
    @Test
    void shouldCreateNewVersion() {
        // Given - setup
        // When - execute
        // Then - verify
    }
}
```

**Integration Test Example:**
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderControllerIntegrationTest {
    
    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldCreateOrderWithAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderVersionNumber").value(1));
    }
}
```

### Running Tests

```bash
# All tests
mvn clean test

# Unit tests only
mvn test -Dgroups=unit

# Integration tests only
mvn test -Dgroups=integration

# With coverage report
mvn clean test jacoco:report

# View coverage
open target/site/jacoco/index.html
```

---

## 🔒 SECURITY GUIDELINES

### Authentication & Authorization

**Always check permissions:**
```java
@PreAuthorize("hasRole('ORDER_MANAGER')")
@PostMapping("/orders")
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request)

@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/schemas")
public ResponseEntity<SchemaResponse> createSchema(@Valid @RequestBody CreateSchemaRequest request)
```

### Input Validation

**Always validate user input:**
```java
// Use Bean Validation
@NotBlank(message = "Order ID required")
private String orderId;

// Use @Valid in controllers
public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest request)

// Additional validation in service layer
if (!isValidOrderId(orderId)) {
    throw new InvalidOrderIdException(orderId);
}
```

### Data Security

**Never log sensitive data:**
```java
// ❌ BAD
log.info("User credentials: {}", credentials);

// ✅ GOOD
log.info("User login attempt for userId: {}", userId);
```

**Use parameterized queries (Spring Data does this automatically):**
```java
// ✅ GOOD - prevents SQL injection
Optional<Order> findByOrderId(String orderId);

// ❌ BAD - vulnerable to injection
@Query("SELECT * FROM orders WHERE order_id = '" + orderId + "'")
```

---

## 📊 COMMON PATTERNS

### Repository Pattern

```java
@Repository
public interface OrderVersionedRepository extends MongoRepository<OrderVersionedDocument, String> {
    
    // Method name query derivation
    Optional<OrderVersionedDocument> findByOrderIdAndIsLatestVersionTrue(String orderId);
    
    List<OrderVersionedDocument> findByOrderIdOrderByOrderVersionNumberAsc(String orderId);
    
    // Custom query
    @Query("{ 'orderId': ?0, 'orderStatus': 'WIP' }")
    List<OrderVersionedDocument> findWipVersions(String orderId);
}
```

### Service Pattern

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionOrchestrationService {
    
    private final OrderVersionedRepository orderRepository;
    private final OrderVersionIndexRepository indexRepository;
    
    @Transactional
    public OrderVersionResponse createNewVersion(CreateOrderRequest request) {
        log.info("Creating version for orderId: {}", request.getOrderId());
        
        // Business logic here
        
        return mapToResponse(savedDocument);
    }
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    
    private final VersionOrchestrationService versionService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderVersionResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        OrderVersionResponse response = versionService.createNewVersion(request);
        return ResponseEntity.created(
            URI.create("/api/v1/orders/" + response.getOrderId())
        ).body(response);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderVersionResponse> getLatestVersion(
            @PathVariable String orderId) {
        return ResponseEntity.ok(versionService.getLatestVersion(orderId));
    }
}
```

### Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Order Not Found")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message(String.join(", ", errors))
            .build();
        return ResponseEntity.badRequest().body(error);
    }
}
```

---

## 🚀 GETTING STARTED FOR AI AGENTS

### Initial Setup

1. **Read these documents first:**
   - `docs/REQUIREMENTS.md` - Understand business requirements
   - `docs/DATA-MODEL-DESIGN.md` - Understand data structures
   - `docs/ARCHITECTURE-DIAGRAM.md` - Understand system architecture
   - `docs/AGENTS.md` - This file

2. **Understand the tech stack:**
   - Java 17+ with Spring Boot 3.2+
   - MongoDB for JSON documents
   - PostgreSQL for schemas
   - Maven for build

3. **Follow existing patterns:**
   - Look at existing controllers/services before creating new ones
   - Match naming conventions
   - Use same annotations and structures

### When Adding New Features

**Step-by-step approach:**

1. **Understand the requirement**
   - What entity/resource is involved?
   - What operation (CRUD)?
   - What business rules apply?

2. **Create the layers in order:**
   - DTO (request/response objects)
   - Entity (database model)
   - Repository (data access)
   - Service (business logic)
   - Controller (REST endpoint)
   - Tests (unit + integration)

3. **Follow the pattern:**
   ```
   Controller → Service → Repository → Database
   ```

4. **Write tests before marking complete:**
   - Unit tests for service logic
   - Integration tests for REST endpoints

---

## 📝 CODE REVIEW CHECKLIST

Before committing code, verify:

### Functionality
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New code has unit tests (80%+ coverage)
- [ ] Integration tests added for new endpoints

### Code Quality
- [ ] Follows naming conventions
- [ ] Uses Lombok to reduce boilerplate
- [ ] Constructor injection (not field injection)
- [ ] Logging uses SLF4J (not System.out)
- [ ] No hardcoded values (use application.yml)
- [ ] Javadoc on public methods

### Security
- [ ] Input validation with Bean Validation
- [ ] Authorization checks with @PreAuthorize
- [ ] No sensitive data logged
- [ ] No SQL injection vulnerabilities

### Spring Boot Best Practices
- [ ] @Transactional on multi-step operations
- [ ] Optional used for nullable results
- [ ] Exception handling with @ControllerAdvice
- [ ] REST endpoints follow RESTful conventions

### Git
- [ ] No sensitive data (passwords, keys)
- [ ] No IDE-specific files (.idea/, *.iml)
- [ ] No build artifacts (target/)
- [ ] Meaningful commit message

---

## 🤖 AI AGENT TASK EXAMPLES

### Example 1: Add New Endpoint

**Prompt:**
```
Add a REST endpoint to get version history for an order. 
- GET /api/v1/orders/{orderId}/versions
- Return list of all committed versions
- Include version number, timestamp, user, status
- Follow existing patterns in OrderController
```

**Expected output:**
- `VersionHistoryResponse` DTO
- `getVersionHistory()` method in `VersionOrchestrationService`
- `GET /api/v1/orders/{orderId}/versions` endpoint in `OrderController`
- Repository method in `OrderVersionedRepository`
- Unit tests
- Integration test

### Example 2: Add Validation

**Prompt:**
```
Add validation to CreateOrderRequest:
- orderId must match pattern ORD-XXXXX
- deliveryLocations must have 1-10 items
- items list must not be empty
- each item must have quantity > 0
```

**Expected output:**
```java
@Data
@Builder
public class CreateOrderRequest {
    
    @NotBlank(message = "Order ID is required")
    @Pattern(regexp = "^ORD-[0-9]{5}$", message = "Order ID must be ORD-XXXXX")
    private String orderId;
    
    @NotEmpty(message = "At least one delivery location required")
    @Size(min = 1, max = 10, message = "1-10 delivery locations allowed")
    private List<@NotBlank String> deliveryLocations;
    
    @Valid
    @NotEmpty(message = "At least one item required")
    private List<OrderItemDTO> items;
}

@Data
public class OrderItemDTO {
    
    @NotBlank
    private String itemNumber;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
```

### Example 3: Add Caching

**Prompt:**
```
Add Redis caching to FormSchemaRepository:
- Cache schema lookups by formVersionId
- Cache TTL: 1 hour
- Clear cache when new schema created
```

**Expected output:**
```java
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "schemas")
public class SchemaManagementService {
    
    @Cacheable(key = "#formVersionId")
    public FormSchemaEntity getSchemaByVersionId(String formVersionId) {
        return schemaRepository.findByFormVersionId(formVersionId)
            .orElseThrow(() -> new SchemaNotFoundException(formVersionId));
    }
    
    @CacheEvict(allEntries = true)
    public FormSchemaEntity createNewSchema(CreateSchemaRequest request) {
        // Create and save schema
        // Cache automatically cleared
    }
}
```

---

## 📚 REFERENCE DOCUMENTATION

### Spring Boot Documentation
- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Spring Batch](https://docs.spring.io/spring-batch/docs/current/reference/html/)

### Project-Specific Docs
- `docs/REQUIREMENTS.md` - Business requirements
- `docs/DATA-MODEL-DESIGN.md` - Database design and Java entities
- `docs/ARCHITECTURE-DIAGRAM.md` - System architecture

### Maven Dependencies
```xml
<!-- Core Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Data Access -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Batch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>

<!-- Caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Utilities -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

---

## ✅ SUCCESS CRITERIA

Code is ready to commit when:
- ✅ Compiles without errors
- ✅ All tests pass (80%+ coverage)
- ✅ Follows naming conventions
- ✅ Uses Spring Boot best practices
- ✅ Has proper logging (SLF4J)
- ✅ Has input validation
- ✅ Has exception handling
- ✅ Has Javadoc on public methods
- ✅ No security vulnerabilities
- ✅ No hardcoded credentials
- ✅ Passes code review checklist

---
