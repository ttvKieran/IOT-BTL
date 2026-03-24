# Architecture Diagrams - Tech Device E-Commerce Microservices

Tài liệu này chứa các biểu đồ kiến trúc được vẽ bằng Mermaid.js.

---

## 1. System Architecture Overview

```mermaid
graph TB
    Client[Client Applications<br/>Web Browser/Mobile]
    
    Gateway[API Gateway<br/>Django DRF<br/>Port 8000]
    
    subgraph "Authentication & Core Services"
        Auth[Auth Service<br/>MySQL<br/>Port 8001]
    end
    
    subgraph "Business Services"
        Catalog[Catalog Service<br/>PostgreSQL + JSONB<br/>Port 8003]
        Cart[Cart Service<br/>MySQL<br/>Port 8004]
        Customer[Customer Service<br/>MySQL<br/>Port 8002]
        Staff[Staff Service<br/>MySQL<br/>Port 8005]
    end
    
    subgraph "Databases - MySQL"
        DB_Auth[(auth_db)]
        DB_Customer[(customer_db)]
        DB_Cart[(cart_db)]
        DB_Staff[(staff_db)]
    end
    
    subgraph "Databases - PostgreSQL"
        DB_Catalog[(catalog_db<br/>with JSONB)]
    end
    
    Client -->|HTTP/HTTPS| Gateway
    
    Gateway -->|JWT Validation| Auth
    Gateway -->|Forward| Catalog
    Gateway -->|Forward| Cart
    Gateway -->|Forward| Customer
    Gateway -->|Forward| Staff
    
    Auth -.->|Connect| DB_Auth
    Catalog -.->|Connect| DB_Catalog
    Cart -.->|Connect| DB_Cart
    Customer -.->|Connect| DB_Customer
    Staff -.->|Connect| DB_Staff
    
    Cart -.->|Verify Item| Catalog
    
    style Gateway fill:#ff6b6b
    style Auth fill:#4ecdc4
    style Catalog fill:#4ecdc4
    style Cart fill:#4ecdc4
    style Customer fill:#4ecdc4
    style Staff fill:#4ecdc4
```

---

## 2. Authentication Flow

```mermaid
sequenceDiagram
    participant Client as Client<br/>Browser/Mobile
    participant Gateway as API Gateway<br/>8000
    participant Auth as Auth Service<br/>8001
    participant AuthDB as MySQL<br/>auth_db
    
    Note over Client,AuthDB: 1. Login Request
    Client->>Gateway: POST /auth/login<br/>{username, password}
    
    Note over Client,AuthDB: 2. Forward to Auth Service
    Gateway->>Auth: POST /auth/login<br/>{username, password}
    
    Note over Client,AuthDB: 3. Verify Credentials
    Auth->>AuthDB: SELECT * FROM users<br/>WHERE username=?
    AuthDB-->>Auth: User record + password_hash
    Auth->>Auth: bcrypt.validate(password,<br/>password_hash)
    
    Note over Client,AuthDB: 4. Generate JWT Tokens
    Auth->>Auth: Create Access Token<br/>(exp: 3600s)
    Auth->>Auth: Create Refresh Token<br/>(exp: 604800s)
    Auth->>AuthDB: INSERT INTO refresh_tokens<br/>token_hash, expires_at
    
    Note over Client,AuthDB: 5. Return Tokens
    Auth-->>Gateway: {access_token,<br/>refresh_token, user_info}
    Gateway-->>Client: {access_token,<br/>refresh_token, expires_in}
    
    Note over Client,AuthDB: 6. Subsequent Request with JWT
    Client->>Gateway: GET /catalog/items<br/>Authorization: Bearer <access_token>
    
    Note over Client,AuthDB: 7. API Gateway Validates
    Gateway->>Gateway: Validate JWT signature<br/>& expiration
    Gateway->>Gateway: Extract user_id, role, email
    
    Note over Client,AuthDB: 8. Forward to Service with Headers
    Gateway->>Auth: GET /catalog/items<br/>X-User-ID: <user_id><br/>X-User-Role: customer
    
    Note over Client,AuthDB: 9. Service Returns Response
    Auth-->>Gateway: {items: [...]}
    Gateway-->>Client: {items: [...]}
```

---

## 3. Role-Based Access Control (RBAC)

```mermaid
graph TD
    Request["Client Request<br/>+ JWT Token"]
    
    Gateway["API Gateway<br/>JWT Validation"]
    Extract["Extract from JWT:<br/>- user_id<br/>- role<br/>- email"]
    
    Decision{"Is role<br/>authorized?"}
    
    Customer["Customer Role<br/>✓ GET /catalog<br/>✓ GET/POST/PUT/DELETE /cart<br/>✓ GET /customers/{id}"]
    Staff["Staff Role<br/>✓ GET /catalog<br/>✓ POST/PUT/DELETE /catalog/items<br/>✓ PUT /catalog/items/{id}/stock<br/>✓ GET /staff/{id}"]
    Forbidden403["403 Forbidden<br/>Insufficient Permissions"]
    
    Allowed["Forward to Service<br/>+ X-User-ID header<br/>+ X-User-Role header"]
    
    Request --> Gateway
    Gateway --> Extract
    Extract --> Decision
    Decision -->|customer| Customer
    Decision -->|staff| Staff
    Decision -->|other| Forbidden403
    Customer --> Allowed
    Staff --> Allowed
    Forbidden403 --> End["Return Error"]
    Allowed --> End2["Process & Return"]
    
    style Gateway fill:#ff6b6b
    style Customer fill:#4ecdc4
    style Staff fill:#95e1d3
    style Forbidden403 fill:#ff6b6b
```

---

## 4. Cart to Catalog Verification Flow

```mermaid
sequenceDiagram
    participant Customer as Customer<br/>Browser
    participant Gateway as API Gateway<br/>8000
    participant Cart as Cart Service<br/>8004
    participant CartDB as MySQL<br/>cart_db
    participant Catalog as Catalog Service<br/>8003
    participant CatalogDB as PostgreSQL<br/>catalog_db
    
    Customer->>Gateway: POST /cart/items<br/>{item_id: 1, quantity: 2}
    Gateway->>Cart: POST /cart/items<br/>X-User-ID: user123<br/>{item_id: 1, quantity: 2}
    
    Cart->>Catalog: POST /catalog/items/1/verify
    Catalog->>CatalogDB: SELECT * FROM items<br/>WHERE id=1
    CatalogDB-->>Catalog: {name, price, stock}
    Catalog-->>Cart: {valid: true, price: 1299.99}
    
    Cart->>CartDB: INSERT INTO cart_items<br/>cart_id, item_id, quantity, unit_price
    CartDB-->>Cart: Success
    
    Cart-->>Gateway: {item_id: 1, quantity: 2, subtotal: 2599.98}
    Gateway-->>Customer: 201 Created
```

---

## 5. Data Model: Catalog with JSONB Specifications

```mermaid
classDiagram
    class Item {
        +int id
        +string name
        +string sku
        +enum category
        +decimal price
        +int stock_quantity
        +string description
        +jsonb specifications
        +string image_url
        +timestamptz created_at
        +timestamptz updated_at
        +uuid created_by_staff_id
        +uuid updated_by_staff_id
        +getSpecification(key)* object
        +updateStock(quantity)*
    }
    
    class LaptopSpecs {
        +string processor
        +int ram_gb
        +int storage_gb
        +string storage_type
        +string gpu
        +float screen_size_inch
        +float weight_kg
        +int battery_hour
    }
    
    class MobileSpecs {
        +string processor
        +int ram_gb
        +int storage_gb
        +float screen_size_inch
        +int camera_mp
        +int battery_mah
        +boolean 5g
        +string os
    }
    
    Item "1" --> "*" LaptopSpecs : category=laptop
    Item "1" --> "*" MobileSpecs : category=mobile
    
    note for Item "JSONB field allows flexible schema<br/>Laptop vs Mobile specs vary"
    note for LaptopSpecs "Laptop specific configurations"
    note for MobileSpecs "Mobile specific configurations"
```

---

## 6. Service Communication Diagram

```mermaid
graph LR
    subgraph "Client Layer"
        Browser["Web Browser<br/>Port 3000<br/>Frontend"]
    end
    
    subgraph "API Gateway Layer"
        GW["API Gateway<br/>Port 8000<br/>Django DRF"]
    end
    
    subgraph "Service Layer"
        Auth["Auth Service<br/>Port 8001"]
        Customer["Customer Service<br/>Port 8002"]
        Catalog["Catalog Service<br/>Port 8003"]
        Cart["Cart Service<br/>Port 8004"]
        Staff["Staff Service<br/>Port 8005"]
    end
    
    subgraph "Data Layer"
        MySQL["MySQL Cluster<br/>4 Databases"]
        PG["PostgreSQL<br/>1 Database"]
    end
    
    Browser -->|HTTP/HTTPS| GW
    
    GW -->|Validate JWT| Auth
    GW -->|Route| Customer
    GW -->|Route| Catalog
    GW -->|Route| Cart
    GW -->|Route| Staff
    
    Cart -->|Verify Item| Catalog
    
    Auth -.-> MySQL
    Customer -.-> MySQL
    Catalog -.-> PG
    Cart -.-> MySQL
    Staff -.-> MySQL
    
    style GW fill:#ff6b6b
    style Browser fill:#f0f0f0
```

---

## 7. Database Schema Overview

### MySQL Schema

```mermaid
erDiagram
    USERS {
        string id PK "UUID"
        string username UK "UNIQUE"
        string email UK "UNIQUE"
        string password_hash
        enum role "customer|staff"
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    REFRESH_TOKENS {
        string id PK "UUID"
        string user_id FK "REFERENCES USERS(id)"
        string token_hash UK
        boolean is_revoked
        timestamp expires_at
        timestamp created_at
    }
    
    CUSTOMERS {
        string id PK "UUID"
        string email UK
        string full_name
        string phone
        date birth_date
        enum gender
        json preferences
        timestamp created_at
        timestamp updated_at
    }
    
    CUSTOMER_ADDRESSES {
        bigint id PK "AUTO_INCREMENT"
        string customer_id FK "REFERENCES CUSTOMERS(id)"
        string label "home|office|other"
        string address_line1
        string address_line2
        string city
        string state_province
        string postal_code
        string country
        string phone
        boolean is_default
    }
    
    CARTS {
        bigint id PK "AUTO_INCREMENT"
        string customer_id UK "UUID"
        timestamp created_at
        timestamp updated_at
    }
    
    CART_ITEMS {
        bigint id PK "AUTO_INCREMENT"
        bigint cart_id FK "REFERENCES CARTS(id)"
        int item_id "Reference to Catalog"
        int quantity
        decimal unit_price
        timestamp added_at
    }
    
    STAFF {
        string id PK "UUID"
        string full_name
        string phone
        string email UK
        enum role "sales_staff|warehouse_staff|support_staff"
        string department
        boolean is_active
        date hire_date
        timestamp created_at
        timestamp updated_at
    }
    
    USERS ||--o{ REFRESH_TOKENS : "has"
    CUSTOMERS ||--o{ CUSTOMER_ADDRESSES : "has"
    CUSTOMERS ||--o{ CARTS : "has"
    CARTS ||--o{ CART_ITEMS : "has"
```

### PostgreSQL Schema (Catalog)

```mermaid
erDiagram
    ITEMS {
        serial id PK
        string name
        string sku UK
        string category "laptop|mobile"
        decimal price
        int stock_quantity
        text description
        jsonb specifications "JSONB - flexible"
        string image_url
        timestamptz created_at
        timestamptz updated_at
        uuid created_by_staff_id
        uuid updated_by_staff_id
    }
```

---

## 8. Environment Setup Flow

```mermaid
graph TD
    DockerCompose["docker-compose.yml"]
    
    MySQL5["MySQL Container<br/>auth_db<br/>customer_db<br/>cart_db<br/>staff_db"]
    
    PG["PostgreSQL Container<br/>catalog_db"]
    
    Services["Service Containers"]
    
    Auth["Auth Service<br/>env: DB_HOST,<br/>JWT_SECRET"]
    
    Catalog["Catalog Service<br/>env: DB_HOST,<br/>DB_TYPE=postgresql"]
    
    CartService["Cart Service<br/>env: DB_HOST,<br/>CATALOG_URL"]
    
    Customer["Customer Service<br/>env: DB_HOST"]
    
    StaffService["Staff Service<br/>env: DB_HOST"]
    
    Gateway["API Gateway<br/>env: JWT_SECRET,<br/>Service URLs"]
    
    DockerCompose --> MySQL5
    DockerCompose --> PG
    DockerCompose --> Services
    
    Services --> Auth
    Services --> Catalog
    Services --> CartService
    Services --> Customer
    Services --> StaffService
    Services --> Gateway
    
    Auth -.-> MySQL5
    Customer -.-> MySQL5
    CartService -.-> MySQL5
    StaffService -.-> MySQL5
    Catalog -.-> PG
    
    style DockerCompose fill:#ff6b6b
    style MySQL5 fill:#4ecdc4
    style PG fill:#f3a683
```

