# ARCHITECTURE DIAGRAMS & VISUAL SPECIFICATIONS

**Project:** Dynamic Versioned Form Management System  
**Document Version:** 1.0  
**Date:** February 11, 2026  
**Prepared by:** Demo Owner

---

## DIAGRAM 1: HIGH-LEVEL 3-TIER ARCHITECTURE

```
┌───────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                         │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Dynamic    │  │   Version    │  │   Schema     │         │
│  │     Form     │  │   History    │  │   Admin      │         │
│  │   Renderer   │  │    Viewer    │  │   Console    │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                 │                 │                 │
│         │    React/Angular/Vue.js           │                 │
└─────────┼──────────────────┼────────────────┼─────────────────┘
          │                  │                │
          └──────────┬───────┴────────────────┘
                     │ REST API (HTTPS/JSON)
                     │
┌────────────────────▼──────────────────────────────────────────┐
│                    SERVICE LAYER                              │
│                                                               │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│  │   Version      │  │    Schema      │  │   Validation   │   │
│  │ Orchestration  │  │  Management    │  │    Service     │   │
│  │    Service     │  │    Service     │  │                │   │
│  └────────┬───────┘  └────────┬───────┘  └────────┬───────┘   │
│           │                   │                   │           │
│  ┌────────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐  │
│  │      Data       │  │     Purge      │  │  Authentication│  │
│  │ Transformation  │  │    Service     │  │  Authorization │  │
│  │    Service      │  │   (Batch)      │  │    Service     │  │
│  └────────┬────────┘  └───────┬────────┘  └────────────────┘  │
│           │                   │                               │
│           │  Java Spring Boot |                               │
└───────────┼───────────────────┼───────────────────────────────┘
            │                   │
    ┌───────┴────────┬──────────┴──────────┐
    │                │                     │
┌───▼────────────┐  ┌▼────────────────┐  ┌─▼──────────────────┐
│   JSON Store   │  │  Dimensional    │  │   Schema Registry  │
│   (MongoDB/    │  │    Tables       │  │   (Postgres)       │
│   Postgres)    │  │  (SQL Legacy)   │  │                    │
│                │  │                 │  │                    │
│ orders_        │  │ - customers     │  │ - form_schemas     │
│  versioned     │  │ - products      │  │ - field_mappings   │
│ order_version_ │  │ - delivery_     │  │                    │
│  index         │  │   companies     │  │                    │
│ purge_audit_   │  │                 │  │                    │
│  log           │  │                 │  │                    │
└────────────────┘  └─────────────────┘  └────────────────────┘
        DATA LAYER
```

**Description for Visual Tool:**
- 3 horizontal layers with clear separation
- Presentation layer: 3 boxes (Form Renderer, Version History, Schema Admin)
- Service layer: 6 service boxes arranged in 2 rows
- Data layer: 3 database cylinders
- Arrows showing bidirectional communication between layers
- Color scheme: Blue for presentation, Green for service, Gray for data

---

## DIAGRAM 2: DETAILED SERVICE LAYER ARCHITECTURE

```
┌──────────────────────────────────────────────────────────────────┐
│                      API GATEWAY / LOAD BALANCER                 │
│                   (Rate Limiting, Authentication)                │
└──────────────────────────────┬───────────────────────────────────┘
                               │
        ┌──────────────────────┼───────────────────────┐
        │                      │                       │
┌───────▼────────┐    ┌────────▼─────────┐    ┌────────▼─────────┐
│   Version      │    │    Schema        │    │   Validation     │
│ Orchestration  │◄───┤  Management      │───►│    Service       │
│   Service      │    │    Service       │    │                  │
├────────────────┤    ├──────────────────┤    ├──────────────────┤
│ -  Get Latest  │    │ -  Get Schema    │    │ -  Field Rules   │
│ -  Create New  │    │ -  Create Schema │    │ -  Cross-field   │
│ -  Version     │    │ -  Version Schema│    │ -  Business Rules│
│   Comparison   │    │ -  Activate      │    │ -  Type Checking │
│ -  Rollback    │    │ -  Deprecate     │    └──────────────────┘
└────────┬───────┘    └──────────────────┘             │
         │                                             │
         │            ┌────────────────────────────────┘
         │            │
┌────────▼────────────▼────────┐      ┌──────────────────────┐
│   Data Transformation        │      │   Purge Service      │
│        Service               │      │     (Batch Job)      │
├──────────────────────────────┤      ├──────────────────────┤
│ -  Dimensional → JSON        │      │ -  Identify WIP      │
│ -  Apply Mappings            │      │ -  Delete Old WIP    │
│ -  Format Conversions        │      │ -  Keep Latest WIP   │
│ -  Denormalization           │      │ -  Audit Logging     │
│ -  Populate Lookups          │      │ -  Schedule: Daily   │
└────────┬─────────────────────┘      │   00:00 AM           │
         │                            └──────────────────────┘
         │
┌────────▼──────────────────────────────────────────────────┐
│              SHARED DATA ACCESS LAYER                     │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   MongoDB    │  │  PostgreSQL  │  │    Redis     │     │
│  │   Client     │  │   Client     │  │   Cache      │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────────────────────────────────────────┘
```

**Key Components:**

**1. Version Orchestration Service**
- Core service managing version lifecycle
- Determines next version number
- Handles concurrent save conflicts
- Manages isLatestVersion flag
- Provides version comparison API

**2. Schema Management Service**
- CRUD operations for form schemas
- Schema versioning and activation
- Backward compatibility validation
- Schema deprecation workflow

**3. Validation Service**
- Schema-driven field validation
- Business rule enforcement
- Data type and format checking
- Cross-field validation (e.g., total = sum of items)

**4. Data Transformation Service**
- Bidirectional conversion (Dimensional ↔ JSON)
- Field mapping registry lookup
- Data denormalization
- Lookup population from master tables

**5. Purge Service**
- Batch job (cron/scheduled)
- Runs daily at midnight
- Identifies and deletes WIP versions
- Maintains audit trail

**6. Shared Data Access Layer**
- Connection pooling
- Query optimization
- Caching strategy (Redis)
- Transaction management

---

## DIAGRAM 3: DATA FLOW - CREATE NEW ORDER

```
┌─────────┐                                              ┌──────────┐
│  User   │                                              │  System  │
└────┬────┘                                              └────┬─────┘
     │                                                        │
     │ 1. Open New Order Form                                 │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                                    2. Get Active Schema│
     │                                        (form_schemas)  │
     │  ◄─────────────────────────────────────────────────────┤
     │                                                        │
     │ 3. Render Empty Form (Schema v2.1.0)                   │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │ 4. Select "Delivery Company" (DC-789)                  │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                    5. Query Dimensional Table          │
     │                       (delivery_companies)             │
     │                         Get Company Details            │
     │  ◄─────────────────────────────────────────────────────┤
     │                                                        │
     │ 6. Populate Company Fields (Name, Contact, Address)    │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │ 7. Add Items (ITEM-001, ITEM-002)                      │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                         8. Validate Each Item          │
     │                            (products table)            │
     │  ◄─────────────────────────────────────────────────────┤
     │                                                        │
     │ 9. Calculate Totals (Client-side)                      │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │ 10. Click "Save Draft" (Auto-save)                     │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                    11. Validate Form Data              │
     │                        (Validation Service)            │
     │                                                        │
     │                    12. Get Next Version Number         │
     │                        orderId: ORD-12345              │
     │                        versionNumber: 1 (new)          │
     │                                                        │
     │                    13. Insert New Document             │
     │                        orders_versioned                │
     │                        {                               │
     │                          orderId: "ORD-12345",         │
     │                          orderVersionNumber: 1,        │
     │                          formVersionId: "v2.1.0",      │
     │                          orderStatus: "WIP",           │
     │                          isLatestVersion: true,        │
     │                          data: {...}                   │
     │                        }                               │
     │                                                        │
     │                    14. Insert Index Entry              │
     │                        order_version_index             │
     │                                                        │
     │  ◄─────────────────15. Success Response────────────────┤
     │                        (Version 1 saved)               │
     │                                                        │
     │ 16. Continue Editing...                                │
     │ 17. Auto-save every 30 seconds                         │
     ├────────────────────────────────────────────────────►   │
     │                    18. Create Version 2 (WIP)          │
     │                    19. Create Version 3 (WIP)          │
     │                    20. Create Version 4 (WIP)          │
     │                                                        │
     │ 21. Click "Submit Order" (Final Save)                  │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                    22. Create Version 5                │
     │                        orderStatus: "Committed"        │
     │                        Mark v5 as latest               │
     │                                                        │
     │  ◄─────────────────23. Order Submitted─────────────────┤
     │                        (Version 5 - Final)             │
     │                                                        │
```

**Result:** 5 versions created (v1-v4 are WIP, v5 is Committed)

---

## DIAGRAM 4: DATA FLOW - VIEW HISTORICAL ORDER

```
┌─────────┐                                              ┌──────────┐
│  User   │                                              │  System  │
└────┬────┘                                              └────┬─────┘
     │                                                        │
     │ 1. Search Order: "ORD-12345"                           │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                    2. Query order_version_index        │
     │                       WHERE orderId = "ORD-12345"      │
     │                       AND isLatestVersion = true       │
     │                                                        │
     │  ◄─────────────────3. Return Latest Version────────────┤
     │                       Version 5 (Committed)            │
     │                       formVersionId: "v2.1.0"          │
     │                                                        │
     │ 4. Display Current Order                               │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │ 5. Click "View History"                                │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                    6. Query All Versions               │
     │                       WHERE orderId = "ORD-12345"      │
     │                       AND orderStatus = "Committed"    │
     │                       ORDER BY orderVersionNumber      │
     │                                                        │
     │  ◄─────────────────7. Return Version List──────────────┤
     │                       v5 (Current)                     │
     │                       v1 (First submission)            │
     │                                                        │
     │ 8. Display Version Timeline                            │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │ 9. Click "View Version 1"                              │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
     │                    10. Get Full Document               │
     │                        orders_versioned                │
     │                        WHERE orderId = "ORD-12345"     │
     │                        AND orderVersionNumber = 1      │
     │                                                        │
     │                    11. Extract formVersionId           │
     │                        Result: "v1.0" (old schema!)    │
     │                                                        │
     │                    12. Get Schema Definition           │
     │                        form_schemas                    │
     │                        WHERE formVersionId = "v1.0"    │
     │                                                        │
     │  ◄─────────────────13. Return v1.0 Schema──────────────┤
     │                        (Old field layout)              │
     │                                                        │
     │ 14. Render Order Using OLD Schema                      │
     │     - deliveryLocation (single value)                  │
     │     - No deliveryCompany field (didn't exist)          │
     ├────────────────────────────────────────────────────►   │
     │                                                        │
```

**Key Point:** Historical data rendered with historical schema = Perfect backward compatibility!

---

## DIAGRAM 5: SCHEMA EVOLUTION FLOW

```
TIME: January 2026
┌────────────────────────────────────────┐
│     Form Schema v1.0 (Active)          │
├────────────────────────────────────────┤
│ Fields:                                │
│ - orderId (text)                       │
│ - deliveryLocation (text, single)      │
│ - items (table)                        │
└────────────────────────────────────────┘
                  │
                  │ Orders Created:
                  │ ORD-10001 (v1.0)
                  │ ORD-10002 (v1.0)
                  │ ORD-10003 (v1.0)
                  ▼
┌────────────────────────────────────────┐
│       JSON Documents                   │
├────────────────────────────────────────┤
│ formVersionId: "v1.0"                  │
│ data: {                                │
│   deliveryLocation: "Location A"       │
│ }                                      │
└────────────────────────────────────────┘

═══════════════════════════════════════════
TIME: February 2026 - SCHEMA CHANGE NEEDED
═══════════════════════════════════════════

Business Requirement: Support multiple delivery locations

┌────────────────────────────────────────┐
│  Schema Admin Creates v2.0             │
├────────────────────────────────────────┤
│ Changes:                               │
│ + deliveryLocations (array) [NEW]      │
│ - deliveryLocation (removed)           │
│ + deliveryCompany (subform) [NEW]      │
│ -  items (table) [UNCHANGED]           │
└────────────────────────────────────────┘
                  │
                  │ Admin clicks "Activate v2.0"
                  ▼
┌────────────────────────────────────────┐
│ form_schemas Table                     │
├────────────────────────────────────────┤
│ v1.0: isActive = false                 │
│ v2.0: isActive = true                  │
└────────────────────────────────────────┘

NEW ORDERS USE v2.0:
═══════════════════════
                  │
                  │ Orders Created:
                  │ ORD-20001 (v2.0)
                  │ ORD-20002 (v2.0)
                  ▼
┌────────────────────────────────────────┐
│       JSON Documents                   │
├────────────────────────────────────────┤
│ formVersionId: "v2.0"                  │
│ data: {                                │
│   deliveryLocations: ["A", "B"]        │
│   deliveryCompany: {...}               │
│ }                                      │
└────────────────────────────────────────┘

VIEWING OLD ORDERS:
═══════════════════════

User views ORD-10001 (created with v1.0)
                  │
                  ▼
System reads: formVersionId = "v1.0"
                  │
                  ▼
System fetches v1.0 schema definition
                  │
                  ▼
UI renders with OLD LAYOUT (single location field)

✓ BACKWARD COMPATIBLE! ✓
```

---

## DIAGRAM 6: END-OF-DAY PURGE PROCESS

```
TIME: 11:59 PM - Before Purge
═════════════════════════════

Order: ORD-12345
┌──────────────────────────────────────────┐
│ Version 1 │ Status: WIP    │ 9:00 AM    │
├──────────────────────────────────────────┤
│ Version 2 │ Status: WIP    │ 10:00 AM   │
├──────────────────────────────────────────┤
│ Version 3 │ Status: WIP    │ 11:00 AM   │
├──────────────────────────────────────────┤
│ Version 4 │ Status: WIP    │ 2:00 PM    │
├──────────────────────────────────────────┤
│ Version 5 │ Status: Committed│ 5:00 PM  │  ← User submitted
├──────────────────────────────────────────┤
│ Version 6 │ Status: WIP    │ 6:00 PM    │  ← User reopened
├──────────────────────────────────────────┤
│ Version 7 │ Status: WIP    │ 7:00 PM    │  ← Latest WIP
└──────────────────────────────────────────┘

Order: ORD-67890
┌──────────────────────────────────────────┐
│ Version 1 │ Status: WIP    │ 10:00 AM   │
├──────────────────────────────────────────┤
│ Version 2 │ Status: WIP    │ 11:00 AM   │
├──────────────────────────────────────────┤
│ Version 3 │ Status: WIP    │ 3:00 PM    │  ← Latest WIP
└──────────────────────────────────────────┘

════════════════════════════════════════════
TIME: 12:00 AM - Purge Job Executes
════════════════════════════════════════════

PURGE LOGIC:
┌──────────────────────────────────────────┐
│ FOR EACH orderId:                        │
│   1. Find all WIP versions               │
│   2. Sort by version number DESC         │
│   3. Keep ONLY the highest version       │
│   4. DELETE all others                   │
│   5. KEEP all Committed versions         │
└──────────────────────────────────────────┘

PROCESSING ORD-12345:
                  │
                  ▼
WIP Versions Found: 1, 2, 3, 4, 6, 7
Highest WIP: Version 7
DELETE: Versions 1, 2, 3, 4, 6
KEEP: Version 7 (latest WIP)
KEEP: Version 5 (Committed - never deleted)

PROCESSING ORD-67890:
                  │
                  ▼
WIP Versions Found: 1, 2, 3
Highest WIP: Version 3
DELETE: Versions 1, 2
KEEP: Version 3 (latest WIP)

════════════════════════════════════════════
TIME: 12:05 AM - After Purge
════════════════════════════════════════════

Order: ORD-12345
┌──────────────────────────────────────────┐
│ Version 5 │ Status: Committed│ 5:00 PM  │  ✓ Kept
├──────────────────────────────────────────┤
│ Version 7 │ Status: WIP    │ 7:00 PM    │  ✓ Kept (latest)
└──────────────────────────────────────────┘

Order: ORD-67890
┌──────────────────────────────────────────┐
│ Version 3 │ Status: WIP    │ 3:00 PM    │  ✓ Kept (latest)
└──────────────────────────────────────────┘

AUDIT LOG:
┌──────────────────────────────────────────┐
│ Purge ID: PURGE-20260211                 │
│ Orders Processed: 2                      │
│ Versions Deleted: 7                      │
│ Versions Retained: 3                     │
│ Status: Completed                        │
│ Duration: 5 minutes                      │
└──────────────────────────────────────────┘
```

---

## DIAGRAM 7: INTEGRATION WITH DIMENSIONAL TABLES

```
SCENARIO: User creates order and selects "Delivery Company"

┌─────────────────────────────────────────────────────────────┐
│                    USER INTERFACE                           │
│                                                             │
│  Order Form (v2.0)                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Order ID: ORD-12345                                │     │
│  │                                                    │     │
│  │ Delivery Company: [Dropdown ▼]                     │     │
│  │                                                    │     │
│  └────────────────────────────────────────────────────┘     │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 1. User clicks dropdown
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              DATA TRANSFORMATION SERVICE                    │
│                                                             │
│  2. Query dimensional table for dropdown options            │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│          DIMENSIONAL TABLES (Legacy System)                 │
│                                                             │
│  delivery_companies Table                                   │
│  ┌─────────┬──────────────┬───────────┬──────────────┐      │
│  │company  │ company_name │  contact  │    phone     │      │
│  │  _id    │              │  _person  │              │      │
│  ├─────────┼──────────────┼───────────┼──────────────┤      │
│  │ DC-789  │ FastShip     │ John Doe  │ 555-0100     │      │
│  │ DC-456  │ QuickDelivery│ Jane Smith│ 555-0200     │      │
│  │ DC-123  │ ExpressShip  │ Bob Johnson│555-0300     │      │
│  └─────────┴──────────────┴───────────┴──────────────┘      │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 3. Return list to UI
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    USER INTERFACE                           │
│                                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Delivery Company: [FastShip ▼]                     │     │
│  │                   ├ FastShip                       │     │
│  │                   ├ QuickDelivery                  │     │
│  │                   └ ExpressShip                    │     │
│  └────────────────────────────────────────────────────┘     │
│                                                             │
│  User selects: "FastShip" (DC-789)                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 4. User selects DC-789
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              DATA TRANSFORMATION SERVICE                    │
│                                                             │
│  5. Lookup field_mappings for formVersionId "v2.0"          │
│  6. Query dimensional table with DC-789                     │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│          DIMENSIONAL TABLES                                 │
│                                                             │
│  SELECT                                                     │
│    company_id,                                              │
│    company_name,                                            │
│    contact_person,                                          │
│    phone,                                                   │
│    email,                                                   │
│    street,                                                  │
│    city,                                                    │
│    state                                                    │
│  FROM delivery_companies                                    │
│  WHERE company_id = 'DC-789'                                │
│                                                             │
│  RESULT:                                                    │
│  ┌─────────────────────────────────────────────────┐        │
│  │ DC-789, FastShip, John Doe, 555-0100, ...       │        │
│  └─────────────────────────────────────────────────┘        │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 7. Transform to JSON
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              DATA TRANSFORMATION SERVICE                    │
│                                                             │
│  Apply field_mappings:                                      │
│                                                             │
│  customers.company_name → data.deliveryCompany.name         │
│  customers.contact_person → data.deliveryCompany.contact    │
│  customers.phone → data.deliveryCompany.phone               │
│  ...                                                        │
│                                                             │
│  Output JSON:                                               │
│  {                                                          │
│    "deliveryCompany": {                                     │
│      "companyId": "DC-789",                                 │
│      "name": "FastShip",                                    │
│      "contactPerson": "John Doe",                           │
│      "phone": "555-0100",                                   │
│      ...                                                    │
│    }                                                        │
│  }                                                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ 8. Populate form fields
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    USER INTERFACE                           │
│                                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Delivery Company                                   │     │
│  │ ┌────────────────────────────────────────────────┐ │     │
│  │ │ Company ID: DC-789                             │ │     │
│  │ │ Name: FastShip                                 │ │     │
│  │ │ Contact: John Doe                              │ │     │
│  │ │ Phone: 555-0100                                │ │     │
│  │ │ Email: john@fastship.com                       │ │     │
│  │ │ Address: 123 Logistics Way, NY 10001           │ │     │
│  │ └────────────────────────────────────────────────┘ │     │
│  └────────────────────────────────────────────────────┘     │
│                                                             │
│  9. User can modify fields (editable)                       │
│  10. When saved, JSON stores DENORMALIZED snapshot          │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ 11. Save order
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              JSON STORE (orders_versioned)                  │
│                                                             │
│  {                                                          │
│    "orderId": "ORD-12345",                                  │
│    "orderVersionNumber": 1,                                 │
│    "data": {                                                │
│      "deliveryCompany": {                                   │
│        "companyId": "DC-789",                               │
│        "name": "FastShip",              ← Snapshot at       │
│        "contactPerson": "John Doe",        time of order    │
│        "phone": "555-0100",                                 │
│        ...                                                  │
│      }                                                      │
│    }                                                        │
│  }                                                          │
│                                                             │
│  EVEN IF company changes phone number later,                │
│  this order preserves ORIGINAL phone number!                │
└─────────────────────────────────────────────────────────────┘
```

**Key Points:**
1. Lookup values populated from dimensional tables
2. Data transformed using field_mappings
3. User can edit populated values
4. Final save creates denormalized snapshot
5. Historical accuracy preserved

---

## DIAGRAM 8: DEPLOYMENT ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────┐
│                    INTERNET / VPN                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    LOAD BALANCER                            │
│                  (AWS ALB / Azure LB)                       │
│               SSL Termination, Health Checks                │
└────────┬────────────────────────────────────┬───────────────┘
         │                                    │
         │                                    │
    ┌────▼────────┐                      ┌───▼─────────┐
    │   Web App   │                      │   Web App   │
    │  Instance 1 │                      │  Instance 2 │
    │  (React)    │                      │  (React)    │
    └────┬────────┘                      └───┬─────────┘
         │                                    │
         └────────────────┬───────────────────┘
                          │
                          │ REST API
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   API GATEWAY                               │
│            Rate Limiting, Authentication                    │
└────────┬────────────────────────────────────┬───────────────┘
         │                                    │
    ┌────▼────────┐                      ┌───▼─────────┐
    │  Service    │                      │  Service    │
    │  Instance 1 │◄────────────────────►│  Instance 2 │
    │(Spring Boot)│   Load Balanced      │(Spring Boot)│
    └────┬────────┘                      └───┬─────────┘
         │                                    │
         └────────────────┬───────────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    ┌────▼──────┐   ┌────▼──────┐   ┌────▼──────┐
    │  MongoDB  │   │ PostgreSQL│   │   Redis   │
    │  Replica  │   │  Primary  │   │   Cache   │
    │    Set    │   │           │   │           │
    │           │   │           │   │           │
    │ Primary + │   │ + Replica │   │ Sentinel  │
    │ 2 Replicas│   │           │   │  Cluster  │
    └───────────┘   └───────────┘   └───────────┘

┌─────────────────────────────────────────────────────────────┐
│                  BATCH JOB SCHEDULER                        │
│                   (Kubernetes CronJob)                      │
│                                                             │
│              Purge Service: Daily 00:00 AM                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              MONITORING & LOGGING                           │
│                                                             │
│  Prometheus + Grafana (Metrics)                             │
│  ELK Stack (Logs)                                           │
│  CloudWatch / Azure Monitor (Alerts)                        │
└─────────────────────────────────────────────────────────────┘
```

---

## SUMMARY OF ARCHITECTURE DECISIONS

| **Aspect** | **Decision** | **Rationale** |
|------------|-------------|---------------|
| **Data Store** | MongoDB or PostgreSQL with JSONB | Native JSON support, flexible schema |
| **Versioning** | Insert-only immutable model | Complete audit trail, regulatory compliance |
| **Schema Evolution** | Schema versioning with backward compatibility | No data migration, historical accuracy |
| **Service Layer** | Microservices with dedicated responsibilities | Scalability, maintainability, separation of concerns |
| **Integration** | Denormalized snapshots from dimensional tables | Historical accuracy, no dependency on legacy systems |
| **Purge Strategy** | Daily batch job with WIP cleanup | Balance storage vs. audit requirements |
| **API Design** | RESTful with JSON payloads | Industry standard, easy integration |
| **Caching** | Redis for schema and lookup data | Performance optimization |
| **Deployment** | Containerized (Docker/Kubernetes) | Scalability, portability, cloud-native |

---