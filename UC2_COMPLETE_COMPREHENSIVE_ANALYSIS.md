# UC2: COMPLETE COMPREHENSIVE ANALYSIS
# Search & Browse Items - All Files, All Layers, All Patterns

> **Complete code-to-code flow showing EVERY file involved in UC2 with OOAD analysis**

---

## 📂 FILES INVOLVED IN UC2

| # | File | Layer | Purpose |
|---|------|-------|---------|
| 1 | `items.html` | **View** | HTML template with search form and results display |
| 2 | `BuyerController.java` | **Controller** | HTTP handler - receives requests, delegates to service |
| 3 | `MarketplaceService.java` | **Service (Interface)** | Contract defining what marketplace operations are available |
| 4 | `MarketplaceServiceImpl.java` | **Service (Implementation)** | Actual business logic - filtering, validation |
| 5 | `ItemRepository.java` | **Repository** | Database access - JPQL queries |
| 6 | `Item.java` | **Model** | Database entity representing a product listing |
| 7 | `Category.java` | **Model** | Database entity representing a category |
| 8 | `ItemStatus.java` | **Model** | Enum for item states (APPROVED, PENDING_REVIEW, SOLD, etc.) |
| 9 | `User.java` | **Model** | Database entity representing a user |
| 10 | **Database (H2)** | **Data** | In-memory database storing actual data |

---

## 🎬 COMPLETE UC2 FLOW DIAGRAM

```
BROWSER (User Action)
    ↓ User opens /buyer/browse and enters search criteria
HTTP REQUEST (Network)
    ↓ GET /buyer/browse?keyword=laptop&categoryId=5&minPrice=100&maxPrice=500
SPRING DISPATCHER (Routing)
    ↓ Routes to BuyerController.browse()
BUYERCONTROLLER.BROWSE() (Controller - Thin)
    ↓ Extracts parameters, calls service, puts data in model
MARKETPLACESERVICE.SEARCHITEMS() (Service - Business Logic)
    ↓ Sanitizes input, delegates to repository
ITEMREPOSITORY.SEARCHITEMS() (Repository - DB Access)
    ↓ Executes JPQL query with filters
DATABASE (H2 - Data)
    ↓ Queries table, returns 15 matching Item rows
BACK UP THE CHAIN (Data flowing back)
    ↓ ItemRepository → MarketplaceService → BuyerController
ITEMS.HTML (Template - View)
    ↓ Renders HTML with search form and item cards
HTTP RESPONSE (HTML Page)
    ↓ 200 OK with complete HTML
BROWSER (Display Results)
    ↓ User sees search results page with 15 items
```

---

## 📋 FILE 1: ITEMS.HTML (View / User Interface)

**Location:** `src/main/resources/templates/items.html`

**What it does:** Displays the search form and item results to the user

### HTML Form (Lines 35-77):

```html
<!-- Search form that user interacts with -->
<form th:action="@{/buyer/browse}" method="get" class="search-bar">
    
    <!-- Text input for keyword search -->
    <div class="form-group">
        <label>Search</label>
        <input type="text" name="keyword" class="form-control"
               placeholder="Item name or description..."
               th:value="${keyword}"/>  <!-- ← Keeps previous search term -->
    </div>
    
    <!-- Dropdown for category filter -->
    <div class="form-group">
        <label>Category</label>
        <select name="categoryId" class="form-control">
            <option value="">All Categories</option>
            <!-- Loop through categories from model -->
            <option th:each="cat : ${categories}"
                    th:value="${cat.id}"
                    th:text="${cat.name}"
                    th:selected="${cat.id == selectedCategoryId}">
            </option>
        </select>
    </div>
    
    <!-- Min price input -->
    <div class="form-group">
        <label>Min Price (₹)</label>
        <input type="number" name="minPrice" class="form-control"
               th:value="${minPrice}"/>  <!-- ← Keeps previous filter -->
    </div>
    
    <!-- Max price input -->
    <div class="form-group">
        <label>Max Price (₹)</label>
        <input type="number" name="maxPrice" class="form-control"
               th:value="${maxPrice}"/>  <!-- ← Keeps previous filter -->
    </div>
    
    <!-- Submit button -->
    <button type="submit" class="btn btn-primary">Search</button>
</form>
```

**Key Point:** When user clicks "Search", this form sends:
```
GET /buyer/browse?keyword=laptop&categoryId=5&minPrice=100&maxPrice=500
```

### Results Display (Lines 78-105):

```html
<!-- Show message if no results -->
<div th:if="${items.empty}" class="empty-state">
    <p>No items match your search. Try different filters.</p>
</div>

<!-- Loop through items returned from server -->
<div class="items-grid" th:unless="${items.empty}">
    <div class="card" th:each="item : ${items}">  <!-- ← For each Item object -->
        <div class="card-body">
            <!-- Display item name (from Item.java - name property) -->
            <div class="card-title" th:text="${item.name}">Item Name</div>
            
            <!-- Display price (from Item.java - price property) -->
            <div class="card-price" th:text="'₹' + ${item.price}">₹0</div>
            
            <!-- Display category (from Item.java - category property) -->
            <div class="card-meta">
                <span th:text="${item.category.name}">Category</span>
            </div>
            
            <!-- Display description (from Item.java - description property) -->
            <div class="card-meta" th:text="${item.description}">Description</div>
            
            <!-- Link to view full details -->
            <a th:href="@{/buyer/item/{id}(id=${item.id})}"
               class="btn btn-outline">View Details</a>
        </div>
    </div>
</div>
```

**What the template does:**
1. ✅ Shows search form with 4 filters
2. ✅ Fills in previous values so user sees what they searched for
3. ✅ Shows category dropdown with all categories from `${categories}`
4. ✅ Loops through `${items}` list and displays each item as a card
5. ✅ For each item, shows: name, price, category, description
6. ✅ Provides link to view item details

**Data it receives from controller (Model):**
- `${items}` → List of Item objects
- `${categories}` → List of Category objects
- `${keyword}` → String (search term)
- `${selectedCategoryId}` → Long (category ID)
- `${minPrice}` → BigDecimal (minimum price)
- `${maxPrice}` → BigDecimal (maximum price)

---

## 📋 FILE 2: BUYERCONTROLLER.JAVA (HTTP Handler)

**Location:** `src/main/java/com/pes/marketplace/controller/BuyerController.java`

**Lines 79-102: The browse() method**

```java
@GetMapping("/browse")  // ← Maps GET /buyer/browse
public String browse(
        @RequestParam(required = false) String     keyword,
        @RequestParam(required = false) Long       categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        Model model) {
```

### Step-by-Step Explanation:

#### Step 1: Spring extracts parameters from URL
```
From URL: GET /buyer/browse?keyword=laptop&categoryId=5&minPrice=100&maxPrice=500

Spring automatically extracts:
  keyword = "laptop"
  categoryId = 5L
  minPrice = 100.00
  maxPrice = 500.00
  model = new Model()
```

#### Step 2: Call service (Line 87-88)
```java
List<Item> items = marketplaceService.searchItems(
    keyword, categoryId, minPrice, maxPrice);
```

**The controller says:**
> "Hey MarketplaceService, find me items with these search criteria"

**The service responds:**
> "Queried database, filtered results, here are 15 Item objects"

#### Step 3: Put data in Model (Lines 90-95)
```java
model.addAttribute("items",              items);
// ↑ Add the 15 Item objects to model so template can loop through them

model.addAttribute("categories",         marketplaceService.getAllCategories());
// ↑ Add all categories so template dropdown can show them

model.addAttribute("keyword",            keyword);
// ↑ Add "laptop" so form field shows what user searched for

model.addAttribute("selectedCategoryId", categoryId);
// ↑ Add 5 so dropdown marks "Electronics" as selected

model.addAttribute("minPrice",           minPrice);
// ↑ Add 100.00 so input field shows "100"

model.addAttribute("maxPrice",           maxPrice);
// ↑ Add 500.00 so input field shows "500"
```

#### Step 4: Return template name (Line 97)
```java
return "items";   // → Spring renders src/main/resources/templates/items.html
```

### What BuyerController does:

| Step | Action | Annotation/Code |
|------|--------|-----------------|
| 1 | Receives HTTP request | `@GetMapping("/browse")` |
| 2 | Extracts parameters | `@RequestParam(required = false)` |
| 3 | Calls service method | `marketplaceService.searchItems(...)` |
| 4 | Prepares Model with data | `model.addAttribute(...)` |
| 5 | Returns template name | `return "items"` |

**Key Principle:** 
- ✅ Does NOT contain business logic
- ✅ Does NOT contain database queries
- ✅ Does NOT create objects
- ✅ **Thin controller** - only coordinates request/response

---

## 📋 FILE 3: MARKETPLACESERVICE.JAVA (Service Interface)

**Location:** `src/main/java/com/pes/marketplace/service/MarketplaceService.java`

**What it does:** Defines the CONTRACT that all marketplace services must follow

```java
public interface MarketplaceService {
    
    /**
     * UC2 Search & Browse Items.
     * 
     * Returns filtered items based on buyer criteria.
     * All parameters are optional.
     */
    List<Item> searchItems(String keyword, Long categoryId,
                          BigDecimal minPrice, BigDecimal maxPrice);
}
```

### Why an interface?

| Benefit | Explanation |
|---------|-------------|
| **Testability** | Can inject MockMarketplaceService for unit tests |
| **Flexibility** | Can swap implementations (real, cached, slow, etc.) |
| **SOLID** | Follows **Dependency Inversion Principle** |
| **Decoupling** | Controller depends on abstraction, not concrete class |

---

## 📋 FILE 4: MARKETPLACESERVICEIMPL.JAVA (Service Implementation)

**Location:** `src/main/java/com/pes/marketplace/service/impl/MarketplaceServiceImpl.java`

**Lines 155-164: The searchItems() method**

```java
@Override
@Transactional(readOnly = true)  // ← Database read-only transaction
public List<Item> searchItems(String keyword, Long categoryId,
                              BigDecimal minPrice, BigDecimal maxPrice) {
    
    // Treat blank string the same as null
    String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
    
    // Delegate to repository
    return itemRepository.searchItems(kw, categoryId, minPrice, maxPrice);
}
```

### What happens:

1. **Receives** 4 parameters from controller
2. **Sanitizes** keyword (converts blank string to null)
3. **Delegates** to ItemRepository to query database
4. **Returns** List of Item objects

### Annotations explained:

| Annotation | Purpose |
|------------|---------|
| `@Override` | This implements the interface method |
| `@Transactional(readOnly = true)` | Opens DB transaction, marks read-only, auto-commits/rolls back |

### Why service layer exists:

| Benefit | Example |
|---------|---------|
| **Separation of Concerns** | Database logic separate from HTTP logic |
| **Reusability** | Multiple controllers can call same service |
| **Testability** | Can mock service in controller tests |
| **Business Rules** | Validation, filtering logic in one place |

---

## 📋 FILE 5: ITEMREPOSITORY.JAVA (Database Access)

**Location:** `src/main/java/com/pes/marketplace/repository/ItemRepository.java`

**Lines 62-80: The searchItems() JPQL query**

```java
@Query("""
        SELECT i FROM Item i
        WHERE i.status = 'APPROVED'              -- ← Only show approved items
          AND (:keyword   IS NULL                -- ← Keyword is optional
               OR LOWER(i.name)        LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR i.category.id = :categoryId)  -- ← Category optional
          AND (:minPrice   IS NULL OR i.price >= :minPrice)         -- ← Min price optional
          AND (:maxPrice   IS NULL OR i.price <= :maxPrice)         -- ← Max price optional
        ORDER BY i.createdAt DESC  -- ← Newest items first
        """)
List<Item> searchItems(
        @Param("keyword")    String     keyword,
        @Param("categoryId") Long       categoryId,
        @Param("minPrice")   BigDecimal minPrice,
        @Param("maxPrice")   BigDecimal maxPrice
);
```

### Breaking down the JPQL query:

#### SELECT clause:
```sql
SELECT i FROM Item i
```
- Select Item objects (not raw database columns)
- 'i' is an alias for Item

#### WHERE clause - Status filter (MANDATORY):
```sql
WHERE i.status = 'APPROVED'
```
- **ONLY** show items that have been approved by admin
- Items with status PENDING_REVIEW, REJECTED, SOLD, REMOVED are hidden
- **This is the key rule:** buyers only see approved items

#### Keyword filter (OPTIONAL):
```sql
AND (:keyword IS NULL 
     OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
     OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
```
- If keyword is null → skip filter (IS NULL)
- If keyword is provided → show items where name OR description contains it
- Case-insensitive search (LOWER function)
- `%keyword%` → keyword can be anywhere in text

**Example:** User searches "laptop"
- Finds: "DELL LAPTOP", "laptop for studies", "Used Laptop Pro"
- Doesn't find: "lap top" (space in between), "Laptop123" (if searching exact)

#### Category filter (OPTIONAL):
```sql
AND (:categoryId IS NULL OR i.category.id = :categoryId)
```
- If categoryId null → skip filter
- If provided → only show items in that category
- `i.category.id = :categoryId` → Foreign key match

#### Price range filters (OPTIONAL):
```sql
AND (:minPrice IS NULL OR i.price >= :minPrice)
AND (:maxPrice IS NULL OR i.price <= :maxPrice)
```
- If minPrice null → skip
- If provided → only show items with price >= minPrice
- Same logic for maxPrice

#### ORDER BY clause:
```sql
ORDER BY i.createdAt DESC
```
- Sort by creation date, newest first
- User sees latest items at top

### What the query returns:

If user searches: `keyword="laptop" categoryId=5 minPrice=100 maxPrice=500`

The SQL becomes:
```sql
SELECT i FROM Item i
WHERE i.status = 'APPROVED'
  AND (LOWER(i.name) LIKE LOWER(CONCAT('%', 'laptop', '%'))
       OR LOWER(i.description) LIKE LOWER(CONCAT('%', 'laptop', '%')))
  AND i.category.id = 5
  AND i.price >= 100
  AND i.price <= 500
ORDER BY i.createdAt DESC
```

**Database returns 15 Item rows matching criteria** ✅

---

## 📋 FILE 6: ITEM.JAVA (Model / Entity)

**Location:** `src/main/java/com/pes/marketplace/model/Item.java`

**What it does:** Represents a product listing in the database

```java
@Entity          // ← This is a database table
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // ← Database ID (PRIMARY KEY)

    @Column(nullable = false)
    private String name;  // ← What the item is called

    @Column(nullable = false, length = 2000)
    private String description;  // ← Details about the item

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;  // ← Cost in rupees

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.PENDING_REVIEW;  // ← State

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;  // ← Which category

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;  // ← Who is selling it

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();  // ← When listed
    
    // Helper method
    public boolean isAvailable() {
        return this.status == ItemStatus.APPROVED;
    }
}
```

### Database table structure:

```
items table:
┌────┬──────────────────────┬──────────────────┬───────┬──────────────┬────────────┬──────────────┐
│ id │ name                 │ description      │ price │ status       │ category_id│ seller_id    │
├────┼──────────────────────┼──────────────────┼───────┼──────────────┼────────────┼──────────────┤
│  1 │ DELL Laptop XPS 15   │ High-perf Intel  │  450  │ APPROVED     │ 5          │ 10           │
│  2 │ HP Pavilion 15       │ Budget friendly  │  350  │ APPROVED     │ 5          │ 11           │
│  3 │ MacBook Pro 14       │ Premium Apple    │ 1299  │ APPROVED     │ 5          │ 12           │
│  4 │ Physics Textbook     │ Engineering...   │   45  │ APPROVED     │ 6          │ 13           │
│  5 │ Calculator           │ Scientific calc  │   25  │ PENDING_REVIEW│ 6         │ 14           │
│  6 │ Notebook Set         │ 100 pages each   │   15  │ REJECTED     │ 6          │ 15           │
└────┴──────────────────────┴──────────────────┴───────┴──────────────┴────────────┴──────────────┘
```

### Key fields for UC2:

| Field | Purpose | Searchable |
|-------|---------|-----------|
| `id` | Unique identifier | No |
| `name` | Product name | **Yes (keyword search)** |
| `description` | Product details | **Yes (keyword search)** |
| `price` | Cost | **Yes (price range filter)** |
| `status` | MUST be APPROVED | **Yes (mandatory filter)** |
| `category_id` | Category classification | **Yes (dropdown filter)** |
| `seller_id` | Who is selling | No (not shown to buyer in UC2) |
| `createdAt` | When listed | **Yes (sort by, newest first)** |

---

## 📋 FILE 7: CATEGORY.JAVA (Model / Entity)

**Location:** `src/main/java/com/pes/marketplace/model/Category.java`

**What it does:** Represents a category that items belong to

```java
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // ← "Electronics", "Books", "Sports"

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Item> items;  // ← All items in this category
}
```

### Database table:

```
categories table:
┌────┬──────────────┬─────────────────────────┐
│ id │ name         │ description             │
├────┼──────────────┼─────────────────────────┤
│  5 │ Electronics  │ Laptops, phones, etc.   │
│  6 │ Books        │ Textbooks, novels, etc. │
│  7 │ Sports       │ Equipment, gear, etc.   │
│  8 │ Furniture    │ Tables, chairs, etc.    │
└────┴──────────────┴─────────────────────────┘
```

**In UC2:** The dropdown shows all categories, users can filter by one

---

## 📋 FILE 8: ITEMSTATUS.JAVA (Enum)

**Location:** `src/main/java/com/pes/marketplace/model/ItemStatus.java`

**What it does:** Defines all possible states an item can be in

```java
public enum ItemStatus {
    PENDING_REVIEW,  // ← Admin hasn't reviewed yet
    APPROVED,        // ← Admin approved, visible to buyers (UC2 shows ONLY these)
    REJECTED,        // ← Admin rejected, seller can edit and resubmit
    SOLD,            // ← Buyer purchased it
    REMOVED          // ← Seller removed listing
}
```

### State transitions:

```
PENDING_REVIEW → APPROVED → SOLD
       ↓           ↓
    REJECTED      REMOVED
       ↓
PENDING_REVIEW (resubmit after editing)
```

**In UC2:** Only APPROVED items are shown to buyers (enforced in ItemRepository query)

---

## 📋 FILE 9: USER.JAVA (Model / Entity)

**Location:** `src/main/java/com/pes/marketplace/model/User.java`

**What it does:** Represents a user (Buyer, Seller, or Admin)

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;  // ← "john@college.edu"

    @Column(nullable = false)
    private String password;  // ← Encrypted password

    @Column(nullable = false)
    private String fullName;  // ← "John Doe"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // ← BUYER, SELLER, or ADMIN

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "seller")
    private List<Item> itemsForSale;  // ← Items this seller is selling

    @OneToMany(mappedBy = "buyer")
    private List<Order> purchaseHistory;  // ← Items this buyer purchased
}
```

**In UC2:** Each Item has a seller_id (foreign key to User), but buyers don't see seller details

---

## 🗄️ DATABASE (H2 In-Memory)

**Location:** Runs in memory during development

### Tables involved:

| Table | Purpose |
|-------|---------|
| **items** | All product listings |
| **categories** | Category classifications |
| **users** | All users |
| **orders** | Purchase records (related to UC3) |

### When UC2 executes, H2 executes:

```sql
-- This is the actual SQL that H2 executes
SELECT i.id, i.name, i.description, i.price, i.status, 
       i.category_id, i.seller_id, i.created_at
FROM items i
WHERE i.status = 'APPROVED'
  AND (i.name ILIKE '%laptop%' OR i.description ILIKE '%laptop%')
  AND i.category_id = 5
  AND i.price >= 100
  AND i.price <= 500
ORDER BY i.created_at DESC
```

**H2 returns 15 rows** matching these criteria

---

## 🔄 COMPLETE DATA FLOW RECAP

### 1. USER INTERACTION (Browser)
```
User opens: http://yoursite.com/buyer/browse
User types: "laptop" in search box
User selects: "Electronics" in category dropdown
User enters: Min Price = 100, Max Price = 500
User clicks: "Search" button
```

### 2. HTTP REQUEST
```
GET /buyer/browse?keyword=laptop&categoryId=5&minPrice=100&maxPrice=500
```

### 3. SPRING ROUTING
```
Spring reads: @GetMapping("/browse")
Spring finds: BuyerController.browse() method
Spring extracts: keyword="laptop", categoryId=5L, minPrice=100.00, maxPrice=500.00
```

### 4. BUYERCONTROLLER (Lines 87-97)
```java
List<Item> items = marketplaceService.searchItems(...);
// ↓ Service returns 15 Item objects

model.addAttribute("items", items);
model.addAttribute("categories", ...);
model.addAttribute("keyword", keyword);
model.addAttribute("selectedCategoryId", categoryId);
model.addAttribute("minPrice", minPrice);
model.addAttribute("maxPrice", maxPrice);
// ↓ All data put in model

return "items";  // Return template name
```

### 5. MARKETPLACESERVICE
```java
public List<Item> searchItems(String keyword, Long categoryId, 
                              BigDecimal minPrice, BigDecimal maxPrice) {
    String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
    // ↓ Sanitize keyword
    
    return itemRepository.searchItems(kw, categoryId, minPrice, maxPrice);
    // ↓ Delegate to repository
}
```

### 6. ITEMREPOSITORY (JPQL Query)
```sql
SELECT i FROM Item i
WHERE i.status = 'APPROVED'
  AND (i.name LIKE '%laptop%' OR i.description LIKE '%laptop%')
  AND i.category.id = 5
  AND i.price >= 100
  AND i.price <= 500
ORDER BY i.createdAt DESC
```
- Executes against H2 database
- Returns: 15 Item objects

### 7. BACK UP THE CHAIN
```
ItemRepository returns: List<Item> [Item1, Item2, Item3, ...Item15]
        ↓
MarketplaceService returns: List<Item> [same list]
        ↓
BuyerController adds to Model
        ↓
Model now contains: items (list of 15), categories (list of 8), keyword, etc.
```

### 8. TEMPLATE RENDERING (items.html)
```html
<!-- Loop through items and display each -->
<div th:each="item : ${items}">
    <div th:text="${item.name}">Dell Laptop XPS 15</div>
    <div th:text="'₹' + ${item.price}">₹450</div>
    <div th:text="${item.category.name}">Electronics</div>
</div>
<!-- Repeat 15 times, once for each item -->
```

Generates HTML with 15 item cards

### 9. HTTP RESPONSE
```
200 OK
Content-Type: text/html; charset=UTF-8

<html>
  <form>...search form with filled values...</form>
  <div class="items-grid">
    <div class="card">...Item 1 card...</div>
    <div class="card">...Item 2 card...</div>
    ...
    <div class="card">...Item 15 card...</div>
  </div>
</html>
```

### 10. BROWSER DISPLAYS
```
┌─────────────────────────────────────┐
│  Campus Marketplace - Browse Items  │
│                                     │
│  Search: [laptop]        ▼[Elect.]  │
│  Min: [100]  Max: [500]  [Search]   │
│                                     │
│  RESULTS (15 items found)           │
│                                     │
│  ┌──────────────────────────────┐   │
│  │ Dell Laptop XPS 15           │   │
│  │ ₹450.00                      │   │
│  │ Electronics                  │   │
│  │ [View Details]               │   │
│  └──────────────────────────────┘   │
│                                     │
│  ┌──────────────────────────────┐   │
│  │ HP Pavilion 15               │   │
│  │ ₹350.00                      │   │
│  │ Electronics                  │   │
│  │ [View Details]               │   │
│  └──────────────────────────────┘   │
│                                     │
│  ... (13 more items) ...            │
│                                     │
└─────────────────────────────────────┘
```

---

## 🎯 OOAD PRINCIPLES DEMONSTRATED IN UC2

### ARCHITECTURAL PATTERNS

#### 1. **MVC Pattern (Model-View-Controller)**

| Component | What It Is | In UC2 |
|-----------|-----------|--------|
| **Model** | Data & business logic | Item, Category, User classes + database |
| **View** | User interface | items.html template |
| **Controller** | Orchestrates flow | BuyerController |

**How it works in UC2:**
```
User Browser Request
        ↓
    [CONTROLLER] ← BuyerController.browse()
        ↓
  [SERVICE] ← MarketplaceService
        ↓
  [REPOSITORY] ← ItemRepository
        ↓
  [DATABASE] ← H2 database
        ↓
    [MODEL] ← Item objects created from DB
        ↓
    [VIEW] ← items.html rendered with model data
        ↓
   HTML Response to browser
```

#### 2. **Dependency Injection Pattern**

**In BuyerController:**
```java
public class BuyerController {
    private final MarketplaceService marketplaceService;  // ← Injected
    private final AuthService authService;                // ← Injected
    
    public BuyerController(MarketplaceService marketplaceService,
                           AuthService authService) {
        this.marketplaceService = marketplaceService;  // ← Spring provides
        this.authService = authService;                // ← Spring provides
    }
}
```

**Benefits:**
- ✅ Testable (inject mocks)
- ✅ Flexible (swap implementations)
- ✅ Decoupled (depends on interfaces)
- ✅ Managed by Spring (no manual creation)

#### 3. **Facade Pattern**

BuyerController acts as a facade:
```
User makes HTTP request
        ↓
BuyerController (FACADE) masks complexity
    ├─ Calls → MarketplaceService.searchItems()
    ├─ Calls → MarketplaceService.getAllCategories()
    └─ Calls → AuthService.findByEmail()
        ↓
User sees simple result (HTML page)
```

Complexity hidden:
- Database queries
- JPA/Hibernate internals
- Transaction management
- Query optimization

---

### SOLID PRINCIPLES

#### 1. **Single Responsibility Principle (SRP)**

**Definition:** A class should have only ONE reason to change.

| Class | Single Responsibility |
|-------|----------------------|
| **BuyerController** | Handle HTTP requests for buyers ONLY |
| **MarketplaceService** | Implement business logic ONLY |
| **ItemRepository** | Access database ONLY |
| **Item** | Represent item data ONLY |

**Evidence in UC2:**
- BuyerController does NOT handle seller logic, admin logic, database queries
- MarketplaceService does NOT handle HTTP, authentication, or repository details
- ItemRepository does NOT handle business rules, just queries

#### 2. **Open/Closed Principle (OCP)**

**Definition:** Classes should be OPEN for extension but CLOSED for modification.

**In UC2:**
```java
// To add: wishlist, ratings, reviews
// Solution: Add new methods WITHOUT modifying existing ones

@PostMapping("/wishlist/{itemId}")
public String addToWishlist(...) { /* new feature */ }

@PostMapping("/review/{orderId}")
public String submitReview(...) { /* new feature */ }
```

**Not violating OCP:**
- ✅ Adding features = adding new methods
- ❌ NOT modifying existing browse(), purchase(), purchaseHistory() methods

#### 3. **Liskov Substitution Principle (LSP)**

**Definition:** Subtypes must be substitutable for their base types.

**In UC2:**
```java
private final MarketplaceService marketplaceService;  // Interface
```

You could use:
- `MarketplaceServiceImpl` - normal implementation
- `MockMarketplaceService` - for testing
- `CachedMarketplaceService` - with caching
- `LoggingMarketplaceService` - with logging

**Controller works with ALL of them** because they satisfy the contract.

#### 4. **Interface Segregation Principle (ISP)**

**Definition:** Clients should not depend on interfaces they don't use.

**In UC2:**
```java
private final MarketplaceService marketplaceService;  // Focused
private final AuthService authService;                // Focused
```

**NOT:**
```java
// ❌ BAD: One massive interface
private final MegaService megaService;
// Has methods: listItem, updateItem, removeItem, purchaseItem, 
//             approveListing, rejectListing, generateReport, deleteUser, etc.
```

**Benefits:**
- ✅ Each interface has only methods we use
- ✅ MarketplaceService has marketplace methods only
- ✅ AuthService has auth methods only

#### 5. **Dependency Inversion Principle (DIP)**

**Definition:** Depend on abstractions (interfaces), not concrete implementations.

**In UC2:**
```java
// ✅ Depends on INTERFACE (abstraction)
private final MarketplaceService marketplaceService;

// ❌ Would be bad (concrete dependency):
// private final MarketplaceServiceImpl serviceImpl = new MarketplaceServiceImpl();
```

**Why DIP matters:**
- If `MarketplaceServiceImpl` changes internally → controller unaffected
- Easy to swap implementations
- Testable with mocks

---

### GRASP PRINCIPLES

#### 1. **Controller (GRASP)**

**Definition:** Assign responsibility to an object representing a system, subsystem, or use case.

**In UC2:**
```java
@Controller
@RequestMapping("/buyer")
public class BuyerController {
    // Represents the "Buyer" use case/subsystem
}
```

**Responsibility:**
- Receive HTTP requests for buyer operations
- Route to appropriate service
- Prepare response

#### 2. **Information Expert (GRASP)**

**Definition:** Assign responsibility to the class that has the most information needed.

**In UC2:**
```java
// ItemRepository is EXPERT on item queries
List<Item> searchItems(...) {
    return itemRepository.searchItems(...);  // ← Delegate to expert
}
```

**Why?**
- ItemRepository knows database structure
- ItemRepository knows all queries
- Controller should NOT know database details

#### 3. **Low Coupling (GRASP)**

**Definition:** Minimize dependencies between objects.

**In UC2:**
```
BuyerController
    ├─ depends on → MarketplaceService (INTERFACE)
    ├─ depends on → AuthService (INTERFACE)
    └─ does NOT depend on → ItemRepository, UserRepository, 
                          JPA, Hibernate, Database details
```

**Benefits:**
- Controller isolated from database changes
- If ORM changes SQL → NoSQL, controller unaffected
- Easier testing

#### 4. **High Cohesion (GRASP)**

**Definition:** Keep related responsibilities together; unrelated ones apart.

**In UC2:**
- ✅ `browse()`, `itemDetail()`, `purchase()`, `purchaseHistory()` → all buyer-related
- ❌ Does NOT include `addItem()` (seller operation)
- ❌ Does NOT include `approveItem()` (admin operation)

**Good cohesion:**
- All methods are buyer domain concerns
- Easy to understand what BuyerController does
- Easy to locate buyer-related code

#### 5. **Protected Variation (GRASP)**

**Definition:** Protect classes from variation in things they depend on.

**In UC2:**
```java
// Repository query handles status check (APPROVED only)
// Controller doesn't need to know about status checking logic
// If status logic changes, only repository changes

@Query("""
        WHERE i.status = 'APPROVED'  // ← Status check encapsulated here
        ...
        """)
```

**Benefits:**
- If status rules change, update query only
- Controller stays unchanged
- Single source of truth for status rule

---

### SPRING FRAMEWORK CONCEPTS

#### 1. **@Controller Annotation**
```java
@Controller
@RequestMapping("/buyer")
public class BuyerController { }
```
- Declares as Spring MVC controller
- Spring auto-instantiates and manages lifecycle
- Enables dependency injection

#### 2. **@GetMapping / @PostMapping**
```java
@GetMapping("/browse")  // Maps GET requests to /buyer/browse
```
- Routes HTTP method + URL to Java method
- Declarative routing (not procedural)

#### 3. **@RequestParam**
```java
@RequestParam(required = false) String keyword
```
- Extracts URL query parameters
- Spring auto-converts types (String → Long, String → BigDecimal)
- `required = false` → optional parameter

#### 4. **Model Object**
```java
model.addAttribute("items", items);
```
- Container passing data from controller to view
- Template accesses via `${items}`

#### 5. **Template Resolution**
```java
return "items";  // Spring finds src/main/resources/templates/items.html
```
- Spring uses "items" as template name
- Automatically renders with model data
- Fills in `${...}` variables

---

## 📊 SUMMARY TABLE: Which File Does What

| Task | File | How? |
|------|------|------|
| **Receive HTTP request** | BuyerController | `@GetMapping("/browse")` |
| **Extract URL parameters** | BuyerController | Spring auto-extracts `@RequestParam` |
| **Call business logic** | BuyerController | `marketplaceService.searchItems(...)` |
| **Validate/sanitize input** | MarketplaceServiceImpl | `keyword.isBlank()` check |
| **Query database** | ItemRepository | JPQL query with filters |
| **Filter by status** | ItemRepository | `WHERE i.status = 'APPROVED'` |
| **Filter by keyword** | ItemRepository | `LIKE CONCAT('%', :keyword, '%')` |
| **Filter by category** | ItemRepository | `i.category.id = :categoryId` |
| **Filter by price** | ItemRepository | `i.price >= :minPrice AND i.price <= :maxPrice` |
| **Sort results** | ItemRepository | `ORDER BY i.createdAt DESC` |
| **Prepare view data** | BuyerController | `model.addAttribute(...)` |
| **Render HTML** | items.html | `<form>`, `<div th:each>` |
| **Show search form** | items.html | `<input th:value="${keyword}">` |
| **Show results** | items.html | `<div th:each="item : ${items}">` |
| **Display item data** | items.html | `<span th:text="${item.name}">` |

---

## ✅ WHY THIS ARCHITECTURE IS GOOD

### Clean Separation

```
Changing database?        → Only change ItemRepository
Changing UI design?       → Only change items.html
Adding filters?           → Only change ItemRepository + controller
Improving performance?    → Only change service/repository
```

### Testable

```
Unit test BuyerController       → Mock MarketplaceService
Unit test MarketplaceService    → Mock ItemRepository
Integration test               → Use real database
```

### Flexible

```
Add caching layer               → Wrap service with CachedMarketplaceService
Change database from H2 to MySQL → Change repository implementation only
Add search analytics            → Add decorator to repository
```

### Maintainable

```
Each file has ONE clear purpose
Easy to find where to make changes
Easy to understand how data flows
Follows industry best practices
```

---

## 🎓 KEY TAKEAWAY

**UC2 is a textbook example of professional enterprise architecture:**

1. **Browser** sends HTTP request with search criteria
2. **Controller** (thin) extracts parameters, calls service, prepares model
3. **Service** (business logic) sanitizes input, delegates to repository
4. **Repository** (database access) executes JPQL query with filters
5. **Models** represent data entities (Item, Category, User)
6. **Database** stores and retrieves data
7. **Template** displays results to user
8. **All layers** are decoupled (depend on abstractions)
9. **All principles** (MVC, SOLID, GRASP, DI) are followed
10. **Easy to test, change, extend**

---

## 💬 TALKING POINTS FOR YOUR PROFESSOR

### "What pattern is UC2 demonstrating?"

> UC2 demonstrates the **MVC (Model-View-Controller) Pattern** with **Dependency Injection**:
> - **Model**: Item, Category, User entities and database
> - **View**: items.html template
> - **Controller**: BuyerController coordinates request/response
> - **Services**: Handle business logic between controller and repositories
> - **Dependencies**: Injected by Spring, making code testable and flexible

### "Why are there different layers (Controller, Service, Repository)?"

> Each layer has a single responsibility:
> - **Controller**: Handle HTTP only (receive request, prepare response)
> - **Service**: Business logic only (filtering, validation, rules)
> - **Repository**: Database access only (queries, CRUD)
> 
> This **Separation of Concerns** makes code:
> - Easy to understand (each file has one job)
> - Easy to test (can mock each layer)
> - Easy to change (modify one layer without touching others)
> - Follows **Single Responsibility Principle (SRP)**

### "How are the optional search filters implemented?"

> The **JPQL query in ItemRepository** handles optional filters:
> ```sql
> AND (:keyword IS NULL OR ... LIKE ...)
> AND (:categoryId IS NULL OR ...)
> AND (:minPrice IS NULL OR ...)
> AND (:maxPrice IS NULL OR ...)
> ```
> 
> If parameter is NULL, that filter is skipped. This **Protected Variation** 
> hides filter logic in the repository, not the controller.

### "Why doesn't the controller directly query the database?"

> The controller depends on the **Service interface, not the Repository**:
> ```java
> private final MarketplaceService marketplaceService;  // ✅ Interface
> // NOT: private final ItemRepository itemRepository;  // ❌ Wrong
> ```
> 
> This **Dependency Inversion Principle (DIP)** makes code:
> - Testable (inject mock service)
> - Flexible (swap implementations)
> - Isolated from database changes

### "How would you extend UC2 to add new features?"

> Following **Open/Closed Principle (OCP)**, add WITHOUT modifying:
>
> **Add sorting:**
> ```java
> @GetMapping("/browse")
> public String browse(..., @RequestParam String sortBy, ...) {
>     // Add sortBy parameter
> }
> ```
>
> **Add pagination:**
> ```java
> @GetMapping("/browse")
> public String browse(..., @RequestParam int page, int size, ...) {
>     // Add pagination parameters
> }
> ```
> 
> **Add ratings filter:**
> ```java
> @GetMapping("/browse")
> public String browse(..., @RequestParam Float minRating, ...) {
>     // Add rating filter
> }
> ```
> 
> **No modification to existing methods** → **Closed for modification**
> **New features as new parameters** → **Open for extension**

---

## 📝 COMPLETE CHECKLIST

You now understand UC2 completely across **10 components**:

- ✅ items.html (View - Search form + Results display)
- ✅ BuyerController (Controller - HTTP handler)
- ✅ MarketplaceService (Interface - Service contract)
- ✅ MarketplaceServiceImpl (Service - Business logic)
- ✅ ItemRepository (Repository - JPQL query)
- ✅ Item (Model - Entity)
- ✅ Category (Model - Entity)
- ✅ ItemStatus (Model - Enum)
- ✅ User (Model - Entity)
- ✅ Database (H2 - Data store)

Plus:
- ✅ **MVC Pattern** - Separation of Model, View, Controller
- ✅ **Dependency Injection** - Spring manages object creation
- ✅ **Facade Pattern** - Controller simplifies service access
- ✅ **SRP** - Each class has one responsibility
- ✅ **OCP** - Open for extension, closed for modification
- ✅ **LSP** - Subtypes are substitutable
- ✅ **ISP** - Focused interfaces
- ✅ **DIP** - Depends on abstractions
- ✅ **GRASP** - Controller, Information Expert, Low Coupling, High Cohesion, Protected Variation
- ✅ **Complete Data Flow** - From browser to database and back

**You are ready to explain UC2 to your professor!** 🎓

