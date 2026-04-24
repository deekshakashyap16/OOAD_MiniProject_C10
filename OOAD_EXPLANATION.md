# OOAD Analysis: BuyerController.java

## 1. OVERVIEW & PURPOSE
The `BuyerController` handles all HTTP requests for the **Buyer role** in a campus marketplace application. It implements three major use cases:
- **UC2**: Search & Browse Items
- **UC3**: Purchase Item
- **UC6**: View Purchase History

---

## 2. ARCHITECTURAL PATTERN: MVC (Model-View-Controller)

### What is MVC?
MVC separates an application into three interconnected components:

| Component | Role | In BuyerController |
|-----------|------|-------------------|
| **Model** | Data & business logic | `Item`, `Order`, `User` classes + database |
| **View** | User interface | HTML templates (`items.html`, `item-detail.html`, etc.) |
| **Controller** | Receives requests, coordinates between Model & View | **BuyerController** - routes requests to services |

### How BuyerController fits MVC:
```
User Browser Request
        ↓
    [CONTROLLER] ← BuyerController receives request
        ↓
  [SERVICE LAYER] ← Delegates to MarketplaceService (business logic)
        ↓
   [REPOSITORY] ← Service queries database
        ↓
    [VIEW] ← Controller renders HTML template with data
        ↓
   HTML Response
```

**Key principle**: The controller is **thin** — it receives requests, calls services, and passes data to views. It contains **zero business logic**.

---

## 3. DESIGN PATTERNS USED

### 3.1 **Dependency Injection (DI) Pattern**

**What is it?**  
Instead of a class creating its own dependencies, they are "injected" from outside.

---

#### **HOW IT WORKS: Step-by-Step**

##### **Without Dependency Injection (BAD WAY):**
```java
public class BuyerController {
    private MarketplaceService marketplaceService;
    
    public BuyerController() {
        // ❌ PROBLEM: Controller creates its own service
        this.marketplaceService = new MarketplaceServiceImpl();
    }
    
    public void browse() {
        marketplaceService.searchItems(...);  // Use the service
    }
}
```

**Problems with this approach:**
1. **Tightly coupled**: Controller is LOCKED to `MarketplaceServiceImpl`
   - Can't use a different implementation without changing this code
2. **Hard to test**: For unit tests, you're stuck with the real database
   - Can't inject a fake/mock service for testing
3. **Difficult to maintain**: If `MarketplaceServiceImpl` constructor changes, all controllers break
4. **Inflexible**: Can't swap implementations at runtime

---

##### **With Dependency Injection (GOOD WAY):**
```java
public class BuyerController {
    private final MarketplaceService marketplaceService;
    private final AuthService authService;
    
    // ✅ GOOD: Controller RECEIVES services from outside
    public BuyerController(MarketplaceService marketplaceService,
                           AuthService authService) {
        this.marketplaceService = marketplaceService;
        this.authService = authService;
    }
    
    public void browse() {
        marketplaceService.searchItems(...);  // Use the service
    }
}
```

**Benefits:**
1. **Loosely coupled**: Controller depends on interfaces, not concrete classes
2. **Easy to test**: Can inject mock services for unit tests
3. **Flexible**: Can swap different implementations
4. **Maintainable**: Changes to service implementations don't break controller

---

#### **WHO CREATES THE SERVICES? SPRING DOES!**

Here's what happens when your app starts:

```
APPLICATION START
    ↓
Spring Framework reads the code
    ↓
Spring discovers: "BuyerController needs two services"
    ↓
Spring creates MarketplaceServiceImpl instance
Spring creates AuthServiceImpl instance
    ↓
Spring calls: new BuyerController(marketplaceServiceImpl, authServiceImpl)
    ↓
BuyerController stores them in private variables
    ↓
App is ready! BuyerController has everything it needs
```

**You never write:**
```java
// ❌ DON'T DO THIS
BuyerController controller = new BuyerController();
```

**Instead, Spring automatically does it:**
```java
// ✅ SPRING DOES THIS FOR YOU
MarketplaceService service = new MarketplaceServiceImpl();
AuthService authService = new AuthServiceImpl();
BuyerController controller = new BuyerController(service, authService);
```

---

#### **CONCRETE EXAMPLE: THE PURCHASE FLOW**

Let's trace what happens when a user clicks "Buy":

```
1. User clicks "Buy" button
       ↓
2. Browser sends: POST /buyer/purchase/123
       ↓
3. Spring routing → finds BuyerController.purchase(123, principal, flash)
       ↓
4. Method runs:
       ↓
       User buyer = currentUser(principal);
       // Uses authService (which Spring injected)
       ↓
       Order order = marketplaceService.purchaseItem(123, buyer);
       // Uses marketplaceService (which Spring injected)
       ↓
5. Both services complete their work
       ↓
6. Response sent back to user
```

**The KEY POINT:**
- The controller **doesn't know** that Spring created these services
- The controller **doesn't care** which implementation is running
- The controller **just uses them** through the interface

---

#### **REAL EXAMPLE: TESTING WITH DI**

**Without Dependency Injection (can't test properly):**
```java
public class BuyerController {
    private MarketplaceService marketplaceService = new MarketplaceServiceImpl();
    
    @Test
    public void testBrowse() {
        // ❌ PROBLEM: Test uses REAL database!
        // Slow, unpredictable, might fail if database is down
        String result = controller.browse(null, null, null, null, model);
        assertEquals("items", result);
    }
}
```

**With Dependency Injection (can test properly):**
```java
public class BuyerControllerTest {
    @Test
    public void testBrowse() {
        // ✅ GOOD: Create mock service
        MarketplaceService mockService = mock(MarketplaceService.class);
        when(mockService.searchItems(null, null, null, null))
            .thenReturn(List.of(new Item(1L, "Laptop")));
        
        // ✅ Inject mock into controller
        BuyerController controller = new BuyerController(
            mockService,  // Mock service (no database!)
            authService   // Real auth service, or mock if needed
        );
        
        // ✅ Test is fast, reliable, isolated from database
        String result = controller.browse(null, null, null, null, model);
        assertEquals("items", result);
    }
}
```

**Why is the test better?**
- No database needed → test runs in milliseconds
- No side effects → test doesn't create real data
- Predictable → mock returns exact data we specify
- Isolated → test only tests the controller, not the service

---

#### **REAL-WORLD ANALOGY (DETAILED):**

**Restaurant Scenario:**

**WITHOUT Dependency Injection:**
```
Customer walks into restaurant
    ↓
Customer says: "I'm hungry"
    ↓
Customer must:
  1. Go to the market and buy ingredients
  2. Go home and cook the meal
  3. Come back to restaurant and eat
    ↓
Problems:
- Customer can't cook (doesn't know how)
- Takes forever
- Customer is locked to cooking at home
```

**WITH Dependency Injection:**
```
Customer walks into restaurant
    ↓
Customer says: "I want pasta"
    ↓
Waiter (Spring DI Container):
  1. Goes to kitchen
  2. Tells chef: "Make pasta"
  3. Brings cooked pasta to customer
    ↓
Customer eats without worrying about:
- How pasta is cooked
- Which ingredients are used
- Where the kitchen is located
    ↓
Benefits:
- Customer only cares about the result (food)
- Chef can change recipe anytime (swap implementation)
- If one chef is sick, another chef takes over (swap implementation)
- Customer never needs to know
```

---

#### **THREE TYPES OF DEPENDENCY INJECTION:**

##### **1. Constructor Injection (USED HERE ✅)**
```java
public BuyerController(MarketplaceService service, AuthService auth) {
    this.marketplaceService = service;
    this.authService = auth;
}
```
- ✅ **Best practice**
- ✅ Immutable (can use `final`)
- ✅ Clear dependencies at a glance
- ✅ Easy to test

##### **2. Setter Injection**
```java
public class BuyerController {
    private MarketplaceService marketplaceService;
    
    @Autowired
    public void setMarketplaceService(MarketplaceService service) {
        this.marketplaceService = service;
    }
}
```
- ⚠️ Dependencies can be set later (might be null initially)
- ⚠️ Less clear which dependencies are required

##### **3. Field Injection**
```java
public class BuyerController {
    @Autowired
    private MarketplaceService marketplaceService;
}
```
- ❌ **Not recommended**
- ❌ Can't use `final` (Spring must set the field)
- ❌ Hard to test (can't inject without reflection)
- ❌ Hidden dependencies

---

#### **SPRING DOES THE WIRING:**

**Spring configuration (usually automatic via @ComponentScan):**
```java
// Spring reads these annotations and creates beans:

@Service  // ← "Hey Spring, create an instance of this"
public class MarketplaceServiceImpl implements MarketplaceService { }

@Service  // ← "Hey Spring, create an instance of this"
public class AuthServiceImpl implements AuthService { }

@Controller  // ← "Hey Spring, create an instance of this"
public class BuyerController {
    // When Spring creates this, it sees:
    // "BuyerController needs MarketplaceService and AuthService"
    // Spring says: "I already created those! Let me pass them."
}
```

**The Magic:**
```
1. Spring scans all classes
2. Finds @Service and @Controller annotations
3. Creates instances (beans)
4. When creating BuyerController, sees constructor parameters
5. Matches parameter types to available beans
6. Automatically calls constructor with correct beans
7. Controller is ready to use!
```

---

#### **SUMMARY TABLE:**

| Aspect | Without DI | With DI |
|--------|-----------|---------|
| **Who creates services?** | The class itself | Spring framework |
| **Is it testable?** | No (locked to real DB) | Yes (can inject mocks) |
| **Can you swap implementations?** | No (hardcoded) | Yes (different bean) |
| **Code coupling** | Tight (depends on concrete class) | Loose (depends on interface) |
| **Maintenance** | Hard (change affects many places) | Easy (change in one place) |
| **Readability** | Hidden dependencies | Clear dependencies |

**In BuyerController:**
```java
private final MarketplaceService marketplaceService;
private final AuthService        authService;

public BuyerController(MarketplaceService marketplaceService,
                       AuthService authService) {
    this.marketplaceService = marketplaceService;
    this.authService        = authService;
}
```

**Why this is important:**
- ✅ **Testability**: You can inject mock services for unit testing
- ✅ **Flexibility**: Swap implementations without changing controller code
- ✅ **Decoupling**: Controller doesn't know HOW services work, only their contracts
- ✅ **Spring manages it**: @Controller annotation + constructor automatically wires dependencies
- ✅ **Maintainability**: Change service implementation once, all dependents benefit

**Real-world analogy:**
> A restaurant customer doesn't make food themselves; they order from a waiter (DI container), who gets the chef (service) to prepare it. The customer doesn't care who the chef is, just that the food arrives. If the restaurant hires a new chef, the customer never knows—the waiter handles it.

---

### 3.2 **Facade Pattern**

**What is it?**  
A single unified interface that provides simplified access to complex subsystems.

**In BuyerController:**
```
Buyer makes HTTP request
    ↓
BuyerController (FACADE)
    ├─ Calls → MarketplaceService.searchItems()
    ├─ Calls → MarketplaceService.purchaseItem()
    └─ Calls → AuthService.findByEmail()
    ↓
Buyer gets response
```

The controller **masks the complexity** of:
- Database queries
- Security checks
- Transaction management
- Order creation logic

**Why?** The client (browser) doesn't care about database details—it just wants to buy an item.

---

### 3.3 **Template Method Pattern** (Implicit)

**In the `purchase()` method:**
```java
@PostMapping("/purchase/{itemId}")
public String purchase(@PathVariable Long itemId, 
                       @AuthenticationPrincipal UserDetails principal,
                       RedirectAttributes flash) {
    User buyer = currentUser(principal);
    try {
        Order order = marketplaceService.purchaseItem(itemId, buyer);  // ← Template
        flash.addFlashAttribute("successMessage", "...");
        return "redirect:/buyer/orders";
    } catch (ItemNotAvailableException | UnauthorizedActionException ex) {
        flash.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/buyer/item/" + itemId;
    }
}
```

**The template steps are:**
1. Get current user
2. Call service method (which internally: verify item, prevent self-buy, create order, update status)
3. Handle success → redirect with message
4. Handle failures → show error

The controller doesn't implement these steps—it **delegates to the service**. This is called **Template Method** because the algorithm's skeleton is defined here, but details are elsewhere.

---

### 3.4 **Strategy Pattern** (Implicit)

Different item search strategies:
```java
List<Item> items = marketplaceService.searchItems(
    keyword, categoryId, minPrice, maxPrice
);
```

The service can implement different **strategies**:
- Search by keyword only
- Filter by category
- Range filter by price
- Combine all filters

The controller **doesn't care how** the search works—it just calls the method with parameters.

---

## 4. SOLID PRINCIPLES

### 4.1 **Single Responsibility Principle (SRP)**

**Definition:** A class should have only ONE reason to change.

**In BuyerController:**
```
✅ Responsibility: Handle HTTP requests for Buyer role ONLY
❌ Does NOT handle: Seller logic, Admin logic, database queries, business calculations
```

If you need to add "seller item listing," you create a `SellerController`, NOT modify this class.

**Why it matters:**
- Easy to understand
- Easy to test
- Easy to maintain
- Changes in seller flow don't break buyer flow

---

### 4.2 **Open/Closed Principle (OCP)**

**Definition:** Classes should be OPEN for extension but CLOSED for modification.

**In BuyerController:**
```java
// Currently handles: browse, purchase, view history
// To add: wishlist, reviews, ratings
// Solution: Add new methods WITHOUT modifying existing ones ✅

@PostMapping("/wishlist/{itemId}")
public String addToWishlist(...) { /* new feature */ }

@PostMapping("/review/{orderId}")
public String submitReview(...) { /* new feature */ }
```

**Not violating OCP:**
```java
// ❌ BAD: Modifying existing purchase() method every time
// ✅ GOOD: Creating new methods for new features
```

---

### 4.3 **Liskov Substitution Principle (LSP)**

**Definition:** Subtypes must be substitutable for their base types.

**In BuyerController:**
```java
private final MarketplaceService marketplaceService;  // Interface
```

You could have:
- `MarketplaceServiceImpl` - normal implementation
- `MockMarketplaceService` - for testing
- `CachedMarketplaceService` - with caching layer

The controller works with **ALL of them** because they satisfy the `MarketplaceService` contract.

---

### 4.4 **Interface Segregation Principle (ISP)**

**Definition:** Clients should not depend on interfaces they don't use.

**In BuyerController:**
```java
private final MarketplaceService marketplaceService;  // Buyer-specific methods
private final AuthService authService;                // Auth-specific methods
```

Instead of one massive `Service` interface with 100 methods, we have:
- **MarketplaceService** - only methods buyers need (search, purchase, history)
- **AuthService** - only authentication methods (findByEmail)

Each interface is **focused and minimal**.

---

### 4.5 **Dependency Inversion Principle (DIP)**

**Definition:** Depend on abstractions (interfaces), not concrete implementations.

**In BuyerController:**
```java
// ✅ Depends on INTERFACE (abstraction)
private final MarketplaceService marketplaceService;

// ❌ Would be bad (concrete dependency):
// private final MarketplaceServiceImpl serviceImpl = new MarketplaceServiceImpl();
```

**Why it matters:**
- If `MarketplaceServiceImpl` changes internally, controller is unaffected
- Easy to swap implementations
- Testable with mock objects

---

## 5. GRASP PRINCIPLES

GRASP = **General Responsibility Assignment Software Patterns**

These are more fine-grained than SOLID.

### 5.1 **Controller (GRASP)**

**Definition:** Assign responsibility to an object that represents a system, subsystem, or use case.

```java
@Controller
@RequestMapping("/buyer")
public class BuyerController {
    // Represents the "Buyer" subsystem/use case
}
```

The controller is the **first object** to receive HTTP requests and coordinate the response. It's the entry point.

---

### 5.2 **Information Expert (GRASP)**

**Definition:** Assign responsibility to the class that has the most information needed to fulfill it.

**In BuyerController:**
```java
private User currentUser(UserDetails principal) {
    return authService.findByEmail(principal.getUsername())
            .orElseThrow(...);
}
```

**Why delegate to `AuthService`?**
- `AuthService` is the **expert** on user lookups
- It knows database queries, caching, etc.
- Controller doesn't have this information

---

### 5.3 **Low Coupling (GRASP)**

**Definition:** Minimize dependencies between objects.

**In BuyerController:**
```java
// ✅ NO direct database calls
// ✅ NO direct JPA repository access
// ✅ Only depends on two service interfaces
```

**Coupling map:**
```
BuyerController
    ├─ depends on → MarketplaceService (INTERFACE)
    ├─ depends on → AuthService (INTERFACE)
    └─ does NOT depend on → ItemRepository, UserRepository, OrderRepository, JPA
```

**Benefits:**
- Controller is isolated from database changes
- If ORM changes (SQL → NoSQL), controller is unaffected
- Easier testing

---

### 5.4 **High Cohesion (GRASP)**

**Definition:** Keep related responsibilities together; unrelated ones apart.

**In BuyerController:**
- ✅ ALL methods relate to **Buyer role**
- ✅ `browse()`, `itemDetail()`, `purchase()`, `purchaseHistory()` are cohesive
- ❌ DOES NOT include seller methods (add items, view sales)
- ❌ DOES NOT include admin methods (approve items, manage users)

**Compare with bad design:**
```java
// ❌ BAD: Low cohesion
class AllUsersController {
    public buyerBrowse() { }
    public buyerPurchase() { }
    public sellerAddItem() { }      // ← Doesn't belong here!
    public adminApproveItem() { }   // ← Doesn't belong here!
}
```

---

### 5.5 **Protected Variation (GRASP)**

**Definition:** Protect classes from variation in things they depend on.

**In BuyerController:**
```java
@PostMapping("/purchase/{itemId}")
public String purchase(...) {
    try {
        Order order = marketplaceService.purchaseItem(itemId, buyer);
        flash.addFlashAttribute("successMessage", 
            "Purchase successful! Order #" + order.getId() + " confirmed.");
        return "redirect:/buyer/orders";
    } catch (ItemNotAvailableException | UnauthorizedActionException ex) {
        flash.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/buyer/item/" + itemId;
    }
}
```

**What's protected?**
- ✅ Internal exceptions are **caught** and hidden
- ✅ User sees **friendly messages** instead of stack traces
- ✅ UI is **shielded** from technical error details

**Example:**
- Service throws `ItemNotAvailableException`
- User sees: "Item not available"
- NOT: "org.springframework.data.EntityNotFoundException: Item with ID 5 not found"

---

## 6. SPRING FRAMEWORK CONCEPTS

### 6.1 **@Controller Annotation**
```java
@Controller
@RequestMapping("/buyer")
public class BuyerController { }
```
- Declares this as a **Spring MVC controller**
- Spring auto-instantiates it and manages its lifecycle
- Enables dependency injection

### 6.2 **@RequestMapping / @GetMapping / @PostMapping**
```java
@GetMapping("/browse")           // Maps GET /buyer/browse
@PostMapping("/purchase/{itemId}")  // Maps POST /buyer/purchase/123
```
These map HTTP methods + URLs to Java methods.

### 6.3 **@AuthenticationPrincipal**
```java
@AuthenticationPrincipal UserDetails principal
```
Spring Security automatically injects the logged-in user. No need to fetch from session manually.

### 6.4 **@PathVariable & @RequestParam**
```java
@PathVariable Long id                        // From URL: /item/{id}
@RequestParam(required = false) String keyword  // From query: ?keyword=laptop
```
Extracts data from HTTP requests and converts to Java types.

### 6.5 **Model Object**
```java
model.addAttribute("items", items);
```
Passes data to the HTML template for rendering.

---

## 7. TRANSACTIONAL INTEGRITY (Implicit)

The service layer is likely annotated with `@Transactional`:

```java
@Transactional  // ← In MarketplaceServiceImpl
public Order purchaseItem(Long itemId, User buyer) {
    // 1. Verify item APPROVED
    // 2. Prevent self-purchase
    // 3. Create Order
    // 4. Mark item SOLD
    // ALL 4 steps: Either ALL succeed or ALL rollback (no partial success)
}
```

**Why?** If step 3 fails but step 4 succeeds, database is inconsistent (order exists but item not marked sold).

---

## 8. EXCEPTION HANDLING HIERARCHY

The code uses custom exceptions with a common pattern:

```
Exception (Java built-in)
    └─ RuntimeException
        ├─ ItemNotAvailableException
        └─ UnauthorizedActionException
```

**Benefits:**
- Controller can catch specific exceptions
- Each exception has a meaningful message
- Easier to debug than generic `Exception`

---

## 9. URL DESIGN (RESTful-ish Conventions)

```
GET    /buyer/browse              → List items
GET    /buyer/item/{id}           → Show item details
POST   /buyer/purchase/{itemId}   → Create order
GET    /buyer/orders              → List user's orders
```

| Method | Purpose | CRUD Operation |
|--------|---------|---|
| GET | Retrieve data | Read |
| POST | Create new resource | Create |
| PUT/PATCH | Update resource | Update |
| DELETE | Remove resource | Delete |

---

## 10. DATA FLOW EXAMPLE: Purchase Flow

```
1. User clicks "Buy" button on item-detail.html
        ↓
2. Browser sends: POST /buyer/purchase/123
        ↓
3. Spring routing → BuyerController.purchase(123, principal, flash)
        ↓
4. Controller extracts user: User buyer = currentUser(principal)
        ↓
5. Controller delegates: marketplaceService.purchaseItem(123, buyer)
        ↓
6. Service validates item exists, is approved, not self-buy
        ↓
7. Service creates Order object
        ↓
8. Service saves order to database
        ↓
9. Service marks item as SOLD
        ↓
10. Service returns Order object to controller
        ↓
11. Controller checks for exceptions (none thrown = success)
        ↓
12. Controller adds success message: "Purchase successful! Order #456"
        ↓
13. Controller returns: redirect:/buyer/orders
        ↓
14. Browser redirects to /buyer/orders
        ↓
15. BuyerController.purchaseHistory() is called
        ↓
16. Controller fetches orders: marketplaceService.getPurchaseHistory(buyer)
        ↓
17. Service queries database for this buyer's orders
        ↓
18. Controller adds orders to model
        ↓
19. Spring renders purchase-history.html with orders data
        ↓
20. Browser shows purchase history with success message
```

---

## 11. WHY EACH PRINCIPLE MATTERS

| Principle | Problem if Ignored | Benefit if Applied |
|-----------|-------------------|-------------------|
| **SRP** | God classes that do everything; hard to change one thing without breaking others | Easy maintenance; changes isolated |
| **OCP** | Modify existing code constantly; high risk of bugs | New features without touching old code |
| **LSP** | Swapping implementations causes crashes | Polymorphism works safely |
| **ISP** | Fat interfaces with unused methods | Clients depend only on what they use |
| **DIP** | Direct dependencies on concrete classes; hard to test | Flexible, testable, swappable code |
| **Controller (GRASP)** | No clear entry point; scattered request handling | Centralized request handling |
| **Info Expert** | One class knows everything; low cohesion | Responsibilities distributed properly |
| **Low Coupling** | Changes propagate everywhere | Isolated changes; easier debugging |
| **High Cohesion** | Related logic scattered across files | Easy to locate and understand code |
| **Protected Variation** | Implementation details leak to UI | Clean separation of concerns |

---

## 12. SUMMARY TABLE

| Concept | How Used | Benefit |
|---------|----------|---------|
| **MVC** | Controller delegates to services; renders views | Separation of concerns |
| **Dependency Injection** | Constructor injection of services | Testability, flexibility |
| **Facade** | Controller simplifies access to services | Hides complexity |
| **SRP** | Only buyer HTTP logic | Focused, maintainable |
| **OCP** | New buyer features as new methods | No modification of existing code |
| **DIP** | Depends on interfaces, not implementations | Decoupled from implementations |
| **ISP** | Two focused interfaces (MarketplaceService, AuthService) | Minimal dependencies |
| **Controller (GRASP)** | Entry point for HTTP requests | Organized request handling |
| **Protected Variation** | Catches exceptions, shows friendly messages | Safe error handling |
| **Spring Annotations** | @Controller, @GetMapping, @AuthenticationPrincipal | Declarative, concise code |

---

## 13. TALKING POINTS FOR YOUR PROFESSOR

### ✅ "What pattern is this?"
- **Answer**: MVC pattern with Dependency Injection. The controller is the HTTP entry point, services handle business logic.

### ✅ "Why are there two services instead of one?"
- **Answer**: Interface Segregation Principle (ISP). Each service has a specific role:
  - `MarketplaceService` → marketplace operations
  - `AuthService` → authentication operations
  - The controller depends only on what it uses.

### ✅ "Why doesn't the controller directly query the database?"
- **Answer**: Dependency Inversion + Low Coupling. The controller depends on service **interfaces**, not repositories. This:
  - Makes it testable (inject mock services)
  - Makes it flexible (swap implementations)
  - Isolates it from database changes

### ✅ "How is the purchase flow validated?"
- **Answer**: Protected Variation + Exception Handling. The service validates and throws domain exceptions. The controller catches them and converts to user-friendly messages, shielding the UI from technical details.

### ✅ "How does Spring Security work here?"
- **Answer**: `@AuthenticationPrincipal UserDetails principal` is auto-injected by Spring Security. We extract the username and look up the user via `AuthService`.

### ✅ "Why are the methods organized this way?"
- **Answer**: High Cohesion + SRP. All methods serve the buyer role. Seller/Admin operations are in separate controllers.

### ✅ "How would you add a wishlist feature?"
- **Answer**: Without modifying existing methods (OCP), add:
  ```java
  @PostMapping("/wishlist/{itemId}")
  public String addToWishlist(@PathVariable Long itemId, 
                              @AuthenticationPrincipal UserDetails principal) {
      User buyer = currentUser(principal);
      marketplaceService.addToWishlist(itemId, buyer);
      return "redirect:/buyer/item/" + itemId;
  }
  ```

---

## KEY TAKEAWAY

This is a **textbook example of SOLID + GRASP principles** applied to a Spring MVC controller:

- ✅ **Thin controller** (no business logic)
- ✅ **Dependency injection** (no direct instantiation)
- ✅ **Service delegation** (controller calls services)
- ✅ **Exception handling** (catches and converts exceptions)
- ✅ **Single responsibility** (only buyer HTTP flows)
- ✅ **Low coupling** (depends on interfaces)
- ✅ **High cohesion** (related methods grouped)

The code is **production-ready** and follows industry best practices.
