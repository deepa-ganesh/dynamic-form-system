# DATA MODEL DESIGN SPECIFICATION

**Project:** Dynamic Versioned Form Management System  
**Document Version:** 1.0  
**Date:** February 11, 2026  
**Prepared by:** Deepa Ganesh

---

## 1. DATA MODEL OVERVIEW

The data model uses a **JSON document store** (MongoDB/PostgreSQL with JSONB) with an **immutable, append-only** architecture. Each form submission creates a new versioned document, enabling complete audit history and backward compatibility.

---

## 2. CORE DATA STRUCTURES

### 2.1 Main Order Document (JSON Store)

**Collection/Table Name:** `orders_versioned`

**Document Structure:**

```json
{
  "_id": "ObjectId or UUID",
  "orderId": "ORD-12345",
  "orderVersionNumber": 5,
  "formVersionId": "v2.1.0",
  "orderStatus": "Committed | WIP | Draft | Submitted | Approved | Cancelled",
  "userName": "deepa.ganesh@morganstanley.com",
  "timestamp": "2026-02-11T09:45:23.456Z",
  "isLatestVersion": true,
  "previousVersionNumber": 4,
  "changeDescription": "Updated delivery locations and item quantities",
  "data": {
    "deliveryLocations": ["Location A", "Location B", "Location C"],
    "deliveryCompany": {
      "companyId": "DC-789",
      "name": "FastShip Logistics",
      "contactPerson": "John Doe",
      "phone": "+1-555-0100",
      "email": "john.doe@fastship.com",
      "address": {
        "street": "123 Logistics Way",
        "city": "New York",
        "state": "NY",
        "zipCode": "10001",
        "country": "USA"
      }
    },
    "items": [
      {
        "itemNumber": "ITEM-001",
        "itemName": "Widget A",
        "quantity": 10,
        "price": 25.50,
        "totalAmount": 255.00,
        "taxRate": 0.08,
        "taxAmount": 20.40
      },
      {
        "itemNumber": "ITEM-002",
        "itemName": "Widget B",
        "quantity": 5,
        "price": 45.00,
        "totalAmount": 225.00,
        "taxRate": 0.08,
        "taxAmount": 18.00
      }
    ],
    "orderTotal": 480.00,
    "totalTax": 38.40,
    "grandTotal": 518.40,
    "notes": "Urgent delivery required",
    "requestedDeliveryDate": "2026-02-15"
  }
}
```

**Field Definitions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `_id` | String | Yes | System-generated unique identifier |
| `orderId` | String | Yes | Business key (e.g., ORD-12345) |
| `orderVersionNumber` | Integer | Yes | Sequential version number (1, 2, 3...) |
| `formVersionId` | String | Yes | Schema version identifier (e.g., v2.1.0) |
| `orderStatus` | String | Yes | Current workflow state |
| `userName` | String | Yes | User who created this version |
| `timestamp` | ISO 8601 | Yes | When this version was created |
| `isLatestVersion` | Boolean | Yes | Flag for quick latest version queries |
| `previousVersionNumber` | Integer | No | Link to previous version |
| `changeDescription` | String | No | User-entered change notes |
| `data` | JSON Object | Yes | Dynamic form data |

**Composite Primary Key:** (`orderId`, `orderVersionNumber`)

**Indexes:**
```javascript
// Primary lookup
db.orders_versioned.createIndex({ "orderId": 1, "orderVersionNumber": 1 }, { unique: true })

// Latest version queries
db.orders_versioned.createIndex({ "orderId": 1, "isLatestVersion": 1 })

// Status-based queries (for purge)
db.orders_versioned.createIndex({ "orderStatus": 1, "timestamp": 1 })

// User activity queries
db.orders_versioned.createIndex({ "userName": 1, "timestamp": -1 })

// Full-text search on data
db.orders_versioned.createIndex({ "data": "text" })
```

---

### 2.2 Form Schema Repository

**Collection/Table Name:** `form_schemas`

```json
{
  "formVersionId": "v2.1.0",
  "formName": "OrderForm",
  "description": "Enhanced order form with tax calculations",
  "createdDate": "2026-02-01T10:00:00Z",
  "createdBy": "system.admin@morganstanley.com",
  "isActive": true,
  "deprecatedDate": null,
  "fields": [
    {
      "fieldId": "orderId",
      "fieldName": "Order ID",
      "fieldType": "text",
      "dataType": "string",
      "required": true,
      "readOnly": false,
      "defaultValue": null,
      "validation": {
        "pattern": "^ORD-[0-9]{5}$",
        "errorMessage": "Order ID must be in format ORD-12345"
      },
      "uiProperties": {
        "row": 1,
        "column": 1,
        "width": "50%",
        "placeholder": "ORD-XXXXX"
      }
    },
    {
      "fieldId": "deliveryLocations",
      "fieldName": "Delivery Locations",
      "fieldType": "multivalue",
      "dataType": "array<string>",
      "required": true,
      "minItems": 1,
      "maxItems": 10,
      "uiProperties": {
        "row": 2,
        "column": 1,
        "width": "100%",
        "addButtonLabel": "Add Location",
        "removeButtonLabel": "Remove"
      }
    },
    {
      "fieldId": "deliveryCompany",
      "fieldName": "Delivery Company",
      "fieldType": "subform",
      "dataType": "object",
      "required": true,
      "nestedFields": [
        {
          "fieldId": "companyId",
          "fieldName": "Company ID",
          "fieldType": "lookup",
          "dataType": "string",
          "required": true,
          "lookupSource": "delivery_companies_table",
          "lookupKeyField": "company_id",
          "lookupDisplayField": "company_name"
        },
        {
          "fieldId": "name",
          "fieldName": "Company Name",
          "fieldType": "text",
          "dataType": "string",
          "required": true
        },
        {
          "fieldId": "contactPerson",
          "fieldName": "Contact Person",
          "fieldType": "text",
          "dataType": "string",
          "required": false
        },
        {
          "fieldId": "address",
          "fieldName": "Address",
          "fieldType": "subform",
          "dataType": "object",
          "nestedFields": [
            {"fieldId": "street", "fieldName": "Street", "fieldType": "text", "dataType": "string"},
            {"fieldId": "city", "fieldName": "City", "fieldType": "text", "dataType": "string"},
            {"fieldId": "state", "fieldName": "State", "fieldType": "dropdown", "dataType": "string"},
            {"fieldId": "zipCode", "fieldName": "Zip Code", "fieldType": "text", "dataType": "string"}
          ]
        }
      ],
      "uiProperties": {
        "row": 3,
        "column": 1,
        "width": "100%",
        "displayType": "panel"
      }
    },
    {
      "fieldId": "items",
      "fieldName": "Order Items",
      "fieldType": "table",
      "dataType": "array<object>",
      "required": true,
      "minRows": 1,
      "maxRows": 100,
      "columns": [
        {
          "fieldId": "itemNumber",
          "fieldName": "Item #",
          "fieldType": "lookup",
          "dataType": "string",
          "required": true,
          "lookupSource": "product_catalog_table",
          "lookupKeyField": "item_number",
          "lookupDisplayField": "item_name",
          "width": "15%"
        },
        {
          "fieldId": "itemName",
          "fieldName": "Item Name",
          "fieldType": "text",
          "dataType": "string",
          "required": true,
          "readOnly": true,
          "width": "30%"
        },
        {
          "fieldId": "quantity",
          "fieldName": "Quantity",
          "fieldType": "number",
          "dataType": "integer",
          "required": true,
          "validation": {"min": 1, "max": 10000},
          "width": "15%"
        },
        {
          "fieldId": "price",
          "fieldName": "Unit Price",
          "fieldType": "number",
          "dataType": "decimal",
          "required": true,
          "validation": {"min": 0.01},
          "format": "currency",
          "width": "15%"
        },
        {
          "fieldId": "totalAmount",
          "fieldName": "Total",
          "fieldType": "calculated",
          "dataType": "decimal",
          "formula": "quantity * price",
          "readOnly": true,
          "format": "currency",
          "width": "15%"
        }
      ],
      "uiProperties": {
        "row": 4,
        "column": 1,
        "width": "100%",
        "addRowButtonLabel": "Add Item",
        "removeRowButtonLabel": "Delete"
      }
    }
  ],
  "calculatedFields": [
    {
      "fieldId": "orderTotal",
      "formula": "SUM(items[].totalAmount)"
    },
    {
      "fieldId": "totalTax",
      "formula": "SUM(items[].taxAmount)"
    },
    {
      "fieldId": "grandTotal",
      "formula": "orderTotal + totalTax"
    }
  ]
}
```

**Indexes:**
```javascript
db.form_schemas.createIndex({ "formVersionId": 1 }, { unique: true })
db.form_schemas.createIndex({ "isActive": 1 })
```

---

### 2.3 Version Metadata Index (Performance Optimization)

**Collection/Table Name:** `order_version_index`

```json
{
  "orderId": "ORD-12345",
  "orderVersionNumber": 5,
  "formVersionId": "v2.1.0",
  "orderStatus": "Committed",
  "userName": "deepa.ganesh@morganstanley.com",
  "timestamp": "2026-02-11T09:45:23.456Z",
  "isLatestVersion": true,
  "documentSize": 2048
}
```

**Purpose:**
- Fast queries without scanning large JSON documents
- Lightweight index for version history lists
- Enables efficient purge operations

**Indexes:**
```javascript
db.order_version_index.createIndex({ "orderId": 1, "orderVersionNumber": 1 }, { unique: true })
db.order_version_index.createIndex({ "orderId": 1, "isLatestVersion": 1 })
db.order_version_index.createIndex({ "orderStatus": 1, "timestamp": 1 })
```

---

### 2.4 Purge Audit Log

**Collection/Table Name:** `purge_audit_log`

```json
{
  "purgeId": "PURGE-20260211",
  "purgeDate": "2026-02-11T00:00:00Z",
  "purgeStartTime": "2026-02-11T00:05:00Z",
  "purgeEndTime": "2026-02-11T00:12:34Z",
  "ordersProcessed": 15234,
  "versionsDeleted": 45678,
  "versionsRetained": 30456,
  "status": "Completed",
  "errors": [],
  "deletedVersionsSample": [
    {
      "orderId": "ORD-12345",
      "deletedVersionNumbers":, [youtube](https://www.youtube.com/watch?v=4FjleDsZlmo)
      "retainedVersionNumber": 5
    }
  ]
}
```

---

### 2.5 Field Mapping Registry (Dimensional Table Integration)

**Collection/Table Name:** `field_mappings`

```json
{
  "mappingId": "MAP-001",
  "formVersionId": "v2.1.0",
  "sourceType": "dimensional_table",
  "mappings": [
    {
      "sourceTable": "customers",
      "sourceColumn": "customer_name",
      "targetJsonPath": "data.deliveryCompany.name",
      "transformation": "UPPERCASE"
    },
    {
      "sourceTable": "customers",
      "sourceColumn": "customer_phone",
      "targetJsonPath": "data.deliveryCompany.phone",
      "transformation": "PHONE_FORMAT"
    },
    {
      "sourceTable": "products",
      "sourceColumn": "item_number",
      "targetJsonPath": "data.items[].itemNumber",
      "transformation": null,
      "isLookupKey": true
    },
    {
      "sourceTable": "products",
      "sourceColumn": "item_name",
      "targetJsonPath": "data.items[].itemName",
      "transformation": null
    },
    {
      "sourceTable": "products",
      "sourceColumn": "unit_price",
      "targetJsonPath": "data.items[].price",
      "transformation": "DECIMAL_2"
    }
  ]
}
```

---

## 3. SCHEMA EVOLUTION & BACKWARD COMPATIBILITY

### 3.1 Version Migration Strategy

**Scenario:** Form schema changes from v1.0 to v2.0

**v1.0 Schema (Old):**
```json
{
  "formVersionId": "v1.0",
  "fields": [
    {"fieldId": "orderId", "fieldType": "text"},
    {"fieldId": "deliveryLocation", "fieldType": "text"},
    {"fieldId": "items", "fieldType": "table"}
  ]
}
```

**v2.0 Schema (New):**
```json
{
  "formVersionId": "v2.0",
  "fields": [
    {"fieldId": "orderId", "fieldType": "text"},
    {"fieldId": "deliveryLocations", "fieldType": "multivalue"},
    {"fieldId": "deliveryCompany", "fieldType": "subform"},
    {"fieldId": "items", "fieldType": "table"}
  ]
}
```

**Data Compatibility:**

**Old Order (v1.0):**
```json
{
  "orderId": "ORD-10000",
  "orderVersionNumber": 1,
  "formVersionId": "v1.0",
  "data": {
    "deliveryLocation": "Location A",
    "items": [...]
  }
}
```

**New Order (v2.0):**
```json
{
  "orderId": "ORD-20000",
  "orderVersionNumber": 1,
  "formVersionId": "v2.0",
  "data": {
    "deliveryLocations": ["Location A", "Location B"],
    "deliveryCompany": {...},
    "items": [...]
  }
}
```

**Rendering Logic:**
- When displaying ORD-10000, UI retrieves v1.0 schema and renders single location field
- When displaying ORD-20000, UI retrieves v2.0 schema and renders multi-value location field
- No data migration required!

---

### 3.2 Forward Compatibility Pattern (Optional Enhancement)

**Field Mapping Table for Migration:**

```json
{
  "fromFormVersion": "v1.0",
  "toFormVersion": "v2.0",
  "fieldTransformations": [
    {
      "sourceField": "deliveryLocation",
      "targetField": "deliveryLocations",
      "transformation": "STRING_TO_ARRAY"
    }
  ]
}
```

If user wants to "upgrade" old order to new schema, system applies transformations.

---

## 4. VERSION MANAGEMENT LOGIC (JAVA/SPRING BOOT)

### 4.1 Version Creation Algorithm

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionOrchestrationService {
    
    private final OrderVersionedRepository orderRepository;
    private final OrderVersionIndexRepository indexRepository;
    private final FormSchemaRepository schemaRepository;
    
    @Transactional
    public Integer createNewVersion(String orderId, 
                                   Map<String, Object> formData, 
                                   String userName, 
                                   boolean isFinalSave) {
        
        // Step 1: Get latest version number
        Optional<OrderVersionIndex> latestVersion = 
            indexRepository.findByOrderIdAndIsLatestVersionTrue(orderId);
        
        Integer newVersionNumber = latestVersion
            .map(v -> v.getOrderVersionNumber() + 1)
            .orElse(1);
        
        // Step 2: Determine status
        OrderStatus status = isFinalSave ? OrderStatus.COMMITTED : OrderStatus.WIP;
        
        // Step 3: Get current form version
        FormSchemaEntity currentSchema = schemaRepository.findByIsActiveTrue()
            .orElseThrow(() -> new SchemaNotFoundException("No active schema found"));
        
        // Step 4: Create new document
        OrderVersionedDocument newDocument = OrderVersionedDocument.builder()
            .orderId(orderId)
            .orderVersionNumber(newVersionNumber)
            .formVersionId(currentSchema.getFormVersionId())
            .orderStatus(status)
            .userName(userName)
            .timestamp(LocalDateTime.now())
            .isLatestVersion(true)
            .previousVersionNumber(latestVersion
                .map(OrderVersionIndex::getOrderVersionNumber)
                .orElse(null))
            .orderData(formData)
            .build();
        
        // Step 5: Insert new version
        OrderVersionedDocument savedDocument = orderRepository.save(newDocument);
        
        // Step 6: Update index (mark previous as not latest)
        latestVersion.ifPresent(prev -> {
            prev.setIsLatestVersion(false);
            indexRepository.save(prev);
        });
        
        // Step 7: Add to index
        OrderVersionIndex indexEntry = OrderVersionIndex.builder()
            .orderId(savedDocument.getOrderId())
            .orderVersionNumber(savedDocument.getOrderVersionNumber())
            .formVersionId(savedDocument.getFormVersionId())
            .orderStatus(savedDocument.getOrderStatus())
            .userName(savedDocument.getUserName())
            .timestamp(savedDocument.getTimestamp())
            .isLatestVersion(true)
            .documentSize(calculateDocumentSize(savedDocument))
            .build();
        indexRepository.save(indexEntry);
        
        log.info("Created version {} for order {}", newVersionNumber, orderId);
        
        return newVersionNumber;
    }
    
    private Integer calculateDocumentSize(OrderVersionedDocument document) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(document);
            return json.length();
        } catch (JsonProcessingException e) {
            log.warn("Unable to calculate document size", e);
            return 0;
        }
    }
}
```

**Required Repository Interfaces:**

```java
@Repository
public interface OrderVersionedRepository 
        extends MongoRepository<OrderVersionedDocument, String> {
    
    Optional<OrderVersionedDocument> findByOrderIdAndIsLatestVersionTrue(String orderId);
    
    Optional<OrderVersionedDocument> findByOrderIdAndOrderVersionNumber(
        String orderId, Integer versionNumber);
    
    List<OrderVersionedDocument> findByOrderIdOrderByOrderVersionNumberAsc(String orderId);
    
    @Modifying
    @Query(value = "{ 'orderId': ?0, 'orderVersionNumber': { $in: ?1 }, 'orderStatus': 'WIP' }", 
           delete = true)
    void deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
        String orderId, List<Integer> versionNumbers);
}

@Repository
public interface OrderVersionIndexRepository 
        extends MongoRepository<OrderVersionIndex, String> {
    
    Optional<OrderVersionIndex> findByOrderIdAndIsLatestVersionTrue(String orderId);
    
    List<OrderVersionIndex> findByOrderIdOrderByOrderVersionNumberAsc(String orderId);
    
    @Query("{ 'orderStatus': 'WIP' }")
    List<OrderVersionIndex> findAllWipVersions();
    
    @Aggregation(pipeline = {
        "{ $match: { 'orderStatus': 'WIP' } }",
        "{ $group: { '_id': '$orderId', 'versions': { $push: '$orderVersionNumber' } } }"
    })
    List<WipVersionsGroup> findOrdersWithWipVersions();
    
    @Modifying
    @Query(value = "{ 'orderId': ?0, 'orderVersionNumber': { $in: ?1 }, 'orderStatus': 'WIP' }", 
           delete = true)
    void deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
        String orderId, List<Integer> versionNumbers);
}

// DTO for aggregation result
@Data
public class WipVersionsGroup {
    @Id
    private String orderId;
    private List<Integer> versions;
}
```

**Domain Entities:**

```java
@Document(collection = "orders_versioned")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "orderId_version_idx", 
               def = "{'orderId': 1, 'orderVersionNumber': 1}", 
               unique = true)
@CompoundIndex(name = "orderId_latest_idx", 
               def = "{'orderId': 1, 'isLatestVersion': 1}")
public class OrderVersionedDocument {
    
    @Id
    private String id;
    
    @Indexed
    private String orderId;
    
    private Integer orderVersionNumber;
    private String formVersionId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    
    private String userName;
    
    @CreatedDate
    private LocalDateTime timestamp;
    
    private Boolean isLatestVersion;
    private Integer previousVersionNumber;
    private String changeDescription;
    
    @Field("data")
    private Map<String, Object> orderData;
}

@Document(collection = "order_version_index")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "orderId_version_idx", 
               def = "{'orderId': 1, 'orderVersionNumber': 1}", 
               unique = true)
public class OrderVersionIndex {
    
    @Id
    private String id;
    
    @Indexed
    private String orderId;
    
    private Integer orderVersionNumber;
    private String formVersionId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    
    private String userName;
    private LocalDateTime timestamp;
    private Boolean isLatestVersion;
    private Integer documentSize;
}

public enum OrderStatus {
    WIP("Work In Progress"),
    COMMITTED("Committed"),
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

---

### 4.2 End-of-Day Purge Algorithm

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeTasklet implements Tasklet {
    
    private final OrderVersionedRepository orderRepository;
    private final OrderVersionIndexRepository indexRepository;
    private final PurgeAuditLogRepository auditRepository;
    
    @Override
    public RepeatStatus execute(StepContribution contribution, 
                               ChunkContext chunkContext) throws Exception {
        
        log.info("Starting daily WIP purge job");
        
        LocalDateTime startTime = LocalDateTime.now();
        String purgeId = "PURGE-" + LocalDate.now().toString();
        
        // Step 1: Find all orders with WIP versions
        List<WipVersionsGroup> ordersWithWIP = 
            indexRepository.findOrdersWithWipVersions();
        
        int totalDeleted = 0;
        int totalRetained = 0;
        List<DeletedVersionsSample> deletedSamples = new ArrayList<>();
        
        // Step 2: For each order, keep only the highest WIP version
        for (WipVersionsGroup orderGroup : ordersWithWIP) {
            String orderId = orderGroup.getOrderId();
            List<Integer> versions = orderGroup.getVersions();
            
            // Sort in descending order
            versions.sort(Comparator.reverseOrder());
            
            Integer latestVersion = versions.get(0);
            List<Integer> versionsToDelete = versions.subList(1, versions.size());
            
            if (!versionsToDelete.isEmpty()) {
                // Delete from main collection
                orderRepository.deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
                    orderId, versionsToDelete);
                
                // Delete from index
                indexRepository.deleteByOrderIdAndOrderVersionNumberInAndOrderStatus(
                    orderId, versionsToDelete);
                
                totalDeleted += versionsToDelete.size();
                totalRetained++;
                
                // Add to samples (limit to first 100)
                if (deletedSamples.size() < 100) {
                    deletedSamples.add(DeletedVersionsSample.builder()
                        .orderId(orderId)
                        .deletedVersionNumbers(versionsToDelete)
                        .retainedVersionNumber(latestVersion)
                        .build());
                }
                
                log.debug("Order {}: deleted {} versions, retained version {}", 
                    orderId, versionsToDelete.size(), latestVersion);
            }
        }
        
        // Step 3: Log purge operation
        PurgeAuditLog auditLog = PurgeAuditLog.builder()
            .purgeId(purgeId)
            .purgeDate(LocalDate.now())
            .purgeStartTime(startTime)
            .purgeEndTime(LocalDateTime.now())
            .ordersProcessed(ordersWithWIP.size())
            .versionsDeleted(totalDeleted)
            .versionsRetained(totalRetained)
            .status("Completed")
            .errors(new ArrayList<>())
            .deletedVersionsSample(deletedSamples)
            .build();
        
        auditRepository.save(auditLog);
        
        log.info("Purge completed successfully. Processed {} orders, deleted {} versions, retained {} versions",
            ordersWithWIP.size(), totalDeleted, totalRetained);
        
        // Update step execution context
        contribution.incrementWriteCount(totalDeleted);
        
        return RepeatStatus.FINISHED;
    }
}
```

**Spring Batch Configuration:**

```java
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class PurgeJobConfig {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PurgeTasklet purgeTasklet;
    
    @Bean
    public Job purgeWipVersionsJob(Step purgeStep) {
        return new JobBuilder("purgeWipVersionsJob", jobRepository)
            .start(purgeStep)
            .build();
    }
    
    @Bean
    public Step purgeStep() {
        return new StepBuilder("purgeStep", jobRepository)
            .tasklet(purgeTasklet, transactionManager)
            .build();
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeJobScheduler {
    
    private final JobLauncher jobLauncher;
    private final Job purgeWipVersionsJob;
    
    @Scheduled(cron = "0 0 0 * * ?")  // Daily at midnight
    public void schedulePurgeJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("startTime", LocalDateTime.now())
                .toJobParameters();
            
            JobExecution execution = jobLauncher.run(purgeWipVersionsJob, params);
            
            log.info("Purge job completed with status: {}", execution.getStatus());
            
        } catch (JobExecutionException e) {
            log.error("Failed to execute purge job", e);
        }
    }
}
```

**Supporting Entities:**

```java
@Document(collection = "purge_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurgeAuditLog {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String purgeId;
    
    private LocalDate purgeDate;
    private LocalDateTime purgeStartTime;
    private LocalDateTime purgeEndTime;
    
    private Integer ordersProcessed;
    private Integer versionsDeleted;
    private Integer versionsRetained;
    
    private String status;
    private List<String> errors;
    
    private List<DeletedVersionsSample> deletedVersionsSample;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletedVersionsSample {
    private String orderId;
    private List<Integer> deletedVersionNumbers;
    private Integer retainedVersionNumber;
}

@Repository
public interface PurgeAuditLogRepository 
        extends MongoRepository<PurgeAuditLog, String> {
    
    Optional<PurgeAuditLog> findByPurgeId(String purgeId);
    
    List<PurgeAuditLog> findByPurgeDateBetweenOrderByPurgeDateDesc(
        LocalDate startDate, LocalDate endDate);
}
```

---

## 5. INTEGRATION WITH DIMENSIONAL TABLES

### 5.1 Data Population Flow

**Step 1: User selects lookup value** (e.g., Delivery Company ID from dropdown)

**Step 2: Service layer queries dimensional table:**

```sql
SELECT 
  company_id,
  company_name,
  contact_person,
  phone,
  email,
  street,
  city,
  state,
  zip_code,
  country
FROM delivery_companies
WHERE company_id = 'DC-789';
```

**Step 3: Transform to JSON structure (Java):**

```java
@Service
@RequiredArgsConstructor
public class DataTransformationService {
    
    private final FieldMappingRepository mappingRepository;
    private final JdbcTemplate jdbcTemplate;
    
    public Map<String, Object> transformDimensionalToJSON(
            String sourceTable, 
            String keyValue, 
            String formVersionId) {
        
        // Get mapping configuration
        FieldMapping mappingConfig = mappingRepository
            .findByFormVersionIdAndSourceTable(formVersionId, sourceTable)
            .orElseThrow(() -> new MappingNotFoundException(sourceTable));
        
        // Query dimensional table
        String sql = buildSelectQuery(mappingConfig, keyValue);
        Map<String, Object> dbRow = jdbcTemplate.queryForMap(sql);
        
        // Transform to JSON
        Map<String, Object> jsonData = new HashMap<>();
        
        for (Mapping mapping : mappingConfig.getMappings()) {
            Object sourceValue = dbRow.get(mapping.getSourceColumn());
            Object transformedValue = applyTransformation(
                sourceValue, 
                mapping.getTransformation()
            );
            setNestedValue(jsonData, mapping.getTargetJsonPath(), transformedValue);
        }
        
        return jsonData;
    }
    
    private Object applyTransformation(Object value, String transformation) {
        if (transformation == null || value == null) {
            return value;
        }
        
        switch (transformation) {
            case "UPPERCASE":
                return value.toString().toUpperCase();
            case "PHONE_FORMAT":
                return formatPhoneNumber(value.toString());
            case "DECIMAL_2":
                return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
            default:
                return value;
        }
    }
    
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = map;
        
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            if (!current.containsKey(key)) {
                current.put(key, new HashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(key);
        }
        
        current.put(keys[keys.length - 1], value);
    }
    
    private String formatPhoneNumber(String phone) {
        // Format phone number (e.g., +1-555-0100)
        return phone.replaceAll("[^0-9]", "")
            .replaceFirst("(\\d{1})(\\d{3})(\\d{4})", "+$1-$2-$3");
    }
}
```

**Step 4: Populate form fields with transformed data**

**Step 5: User modifies as needed (denormalized snapshot)**

**Step 6: Save creates new version with denormalized data**

### 5.2 Why Denormalization?

**Scenario:** Company "FastShip Logistics" changes phone number

- **If normalized:** Historical orders would show NEW phone number (incorrect)
- **If denormalized:** Historical orders preserve phone number at time of order (correct)

**Trade-off:**
- ✅ Historical accuracy
- ✅ No dependency on dimensional tables for reads
- ❌ Data duplication
- ❌ Larger storage requirements

**Decision:** Denormalization is correct for audit/compliance requirements

---

## 6. EXAMPLE QUERIES (Spring Data MongoDB)

### 6.1 Get Latest Version of Order

```java
// Fast query using index
Optional<OrderVersionIndex> latestIndex = indexRepository
    .findByOrderIdAndIsLatestVersionTrue("ORD-12345");

// Full document with data
Optional<OrderVersionedDocument> latestDoc = orderRepository
    .findByOrderIdAndIsLatestVersionTrue("ORD-12345");
```

### 6.2 Get All Versions of Order

```java
List<OrderVersionedDocument> allVersions = orderRepository
    .findByOrderIdOrderByOrderVersionNumberAsc("ORD-12345");
```

### 6.3 Get Specific Version

```java
Optional<OrderVersionedDocument> specificVersion = orderRepository
    .findByOrderIdAndOrderVersionNumber("ORD-12345", 3);
```

### 6.4 Get All WIP Versions (for purge)

```java
List<WipVersionsGroup> wipVersions = indexRepository
    .findOrdersWithWipVersions();

// Aggregation defined in repository:
// @Aggregation(pipeline = {
//     "{ $match: { 'orderStatus': 'WIP' } }",
//     "{ $group: { '_id': '$orderId', 'versions': { $push: '$orderVersionNumber' } } }"
// })
```

---

## 7. DATA MODEL VALIDATION RULES

**Consistency Rules:**
1. `orderVersionNumber` must be sequential (1, 2, 3...) for each `orderId`
2. Only one version can have `isLatestVersion = true` per `orderId`
3. `formVersionId` must exist in `form_schemas` collection
4. `timestamp` must be monotonically increasing for versions of same order
5. `orderStatus` must be valid enum value
6. Composite key (`orderId` + `orderVersionNumber`) must be unique

**Referential Integrity:**
- `formVersionId` → `form_schemas.formVersionId`
- `previousVersionNumber` → Valid version number for same `orderId`

**Validation in Java:**

```java
@Component
public class VersionValidator {
    
    public void validateNewVersion(OrderVersionedDocument document, 
                                   OrderVersionIndex previousVersion) {
        
        // Rule 1: Sequential version numbers
        if (previousVersion != null) {
            Integer expectedVersion = previousVersion.getOrderVersionNumber() + 1;
            if (!document.getOrderVersionNumber().equals(expectedVersion)) {
                throw new ValidationException(
                    "Version number must be sequential. Expected: " + expectedVersion);
            }
        }
        
        // Rule 2: isLatestVersion flag
        if (!document.getIsLatestVersion()) {
            throw new ValidationException("New version must have isLatestVersion = true");
        }
        
        // Rule 3: Valid formVersionId
        if (document.getFormVersionId() == null || document.getFormVersionId().isEmpty()) {
            throw new ValidationException("formVersionId is required");
        }
        
        // Rule 4: Valid orderStatus
        if (document.getOrderStatus() == null) {
            throw new ValidationException("orderStatus is required");
        }
        
        // Rule 5: Monotonic timestamp
        if (previousVersion != null && 
            document.getTimestamp().isBefore(previousVersion.getTimestamp())) {
            throw new ValidationException("Timestamp must be after previous version");
        }
    }
}
```

---

## 8. STORAGE ESTIMATES

**Assumptions:**
- Average order: 5 items, 2 locations, 1 delivery company
- Average JSON document size: ~2 KB
- 10,000 orders per day
- Average 5 versions per order per day (including auto-saves)
- Purge reduces to 2 versions per day (1 WIP + 1 Committed)

**Daily Storage:**
- Before purge: 10,000 orders × 5 versions × 2 KB = 100 MB/day
- After purge: 10,000 orders × 2 versions × 2 KB = 40 MB/day

**Annual Storage:**
- 40 MB/day × 365 days = 14.6 GB/year (very manageable)

**With indexing overhead:** ~20 GB/year total

---

## 9. MAVEN DEPENDENCIES

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    
    <dependencies>
        <!-- Spring Boot MongoDB -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        
        <!-- Spring Boot JPA (for PostgreSQL) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        
        <!-- Spring Batch -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-batch</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
    
</project>
```