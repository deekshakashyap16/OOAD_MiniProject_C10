# BuyerController.java - LINE BY LINE EXPLANATION

> For someone new to OOAD and Spring, here's exactly what each line does

---

## SECTION 1: PACKAGE & IMPORTS (Lines 1-17)

### Line 1:
```java
package com.pes.marketplace.controller;
```
- **What**: Declares which folder/package this class belongs to
- **Why**: Java organizes code into folders. This file is in the `controller` package
- **Think of it**: Like saying "this file is in folder: com → pes → marketplace → controller"

### Lines 3-14: Import Statements
```java
import com.pes.marketplace.exception.ItemNotAvailableException;
import com.pes.marketplace.exception.UnauthorizedActionException;
import com.pes.marketplace.model.Item;
import com.pes.marketplace.model.Order;
import com.pes.marketplace.model.User;
import com.pes.marketplace.service.AuthService;
import com.pes.marketplace.service.MarketplaceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
```

**What does each import mean?**

| Import | Purpose |
|--------|---------|
| `ItemNotAvailableException` | Custom error class for when an item is out of stock |
| `UnauthorizedActionException` | Custom error class for when buyer tries to buy own item |
| `Item`, `Order`, `User` | Data classes representing database tables |
| `AuthService` | Service that handles user lookups |
| `MarketplaceService` | Service that handles all business logic (buy, search, etc) |
| `@AuthenticationPrincipal` | Spring annotation to get logged-in user |
| `UserDetails` | Spring class representing current user |
| `@Controller` | Spring annotation marking this as a controller |
| `Model` | Spring class to pass data to HTML templates |
| `@GetMapping`, `@PostMapping`, `@RequestMapping` | Spring annotations for HTTP routes |
| `RedirectAttributes` | Spring class to pass messages when redirecting |

### Lines 16-17: Java utility imports
```java
import java.math.BigDecimal;    // For prices (precise decimal numbers)
import java.util.List;           // For lists of items
import java.util.Optional;       // For "maybe this exists, maybe not"
```

---

## SECTION 2: CLASS DECLARATION & COMMENTS (Lines 19-49)

### Line 49:
```java
@Controller
```
- **What**: A Spring annotation (marker)
- **Means**: "Hey Spring, this class handles HTTP requests"
- **Result**: Spring automatically creates an instance of this class when the app starts

### Line 50:
```java
@RequestMapping("/buyer")
```
- **What**: Another Spring annotation
- **Means**: "All URLs in this class start with `/buyer`"
- **Example**: If a method has `@GetMapping("/browse")`, the full URL is `/buyer/browse`

### Line 51:
```java
public class BuyerController {
```
- **What**: Creates a class called `BuyerController`
- **public**: Anyone can use this class
- **class**: It's a blueprint for creating objects

---

## SECTION 3: PRIVATE VARIABLES (Lines 52-53)

### Lines 52-53:
```java
private final MarketplaceService marketplaceService;
private final AuthService        authService;
```

**What are these?**
- These are **instance variables** — stored data that belongs to this controller
- **private**: Only this class can access them
- **final**: Once set, they can never change (immutable)

**What do they store?**
- `marketplaceService`: The object that has all business logic methods (buy, search, etc)
- `authService`: The object that handles user authentication

**Why store them?**
- So any method in this class can use them
- Example: The `browse()` method can call `marketplaceService.searchItems()`

**Real-world analogy:**
> Imagine you're a waiter at a restaurant.
> - `marketplaceService` = Your access to the kitchen (chef)
> - `authService` = Your access to the customer database
> You keep these "tools" with you throughout your shift so you can use them anytime.

---

## SECTION 4: CONSTRUCTOR (Lines 55-59)

### Lines 55-59:
```java
public BuyerController(MarketplaceService marketplaceService,
                       AuthService authService) {
    this.marketplaceService = marketplaceService;
    this.authService        = authService;
}
```

**What is a constructor?**
- A special method that runs ONCE when the class is created
- Used to set up initial values

**What does this constructor do?**
1. Line 56: Receives two services as parameters
2. Line 57: Stores the marketplace service in the private variable
3. Line 58: Stores the auth service in the private variable

**Who calls this constructor?**
- NOT you! Spring calls it automatically
- Spring is smart enough to say: "Hey, this class needs two services. Let me create those services first, then pass them to the constructor."

**This is Dependency Injection:**
```
Spring:  "Here BuyerController, you need these services. I'll give them to you."
         ↓ (passes services)
         ↓
Controller: "Thanks! I'll store them and use them whenever I need."
```

---

## SECTION 5: HELPER METHOD (Lines 65-73)

### Lines 65-73:
```java
private User currentUser(UserDetails principal) {
    return authService.findByEmail(principal.getUsername())
            .orElseThrow(() -> new IllegalStateException(
                    "Authenticated user not found in DB: " + principal.getUsername()));
}
```

**What does this method do?**

| Line | What |
|------|------|
| 65 | Method name is `currentUser`, it takes a `UserDetails` object |
| 66 | Ask authService: "Find me the User with this email" |
| 67-69 | If no user found, throw an error saying "User not in database" |

**Step by step:**
1. User logs in with email "john@gmail.com"
2. Spring creates a `UserDetails` object for that user
3. We pass that `UserDetails` to this method
4. This method extracts the email: `principal.getUsername()` → "john@gmail.com"
5. This method asks the database: "Does a User with email john@gmail.com exist?"
6. If YES → Return that User object
7. If NO → Throw an error

**Why this method exists:**
- We use it in EVERY method to get the current logged-in user
- Instead of writing the same code 3 times, we write it once in this helper method
- This is called **DRY principle**: "Don't Repeat Yourself"

---

## SECTION 6: BROWSE METHOD (Lines 79-102)

### Line 79:
```java
@GetMapping("/browse")
```
- **GET**: When user REQUESTS data (not submitting forms)
- **/browse**: Full URL is `/buyer/browse`
- **What happens**: When user goes to `http://yoursite.com/buyer/browse`, Spring calls this method

### Lines 80-85:
```java
public String browse(
        @RequestParam(required = false) String     keyword,
        @RequestParam(required = false) Long       categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        Model model) {
```

**What are `@RequestParam`?**
- These are URL parameters (after the `?`)
- **Example URLs:**
  - `/buyer/browse` → Show all items
  - `/buyer/browse?keyword=laptop` → Show items with "laptop" in name
  - `/buyer/browse?categoryId=5&minPrice=100` → Show items in category 5, over $100
  - `/buyer/browse?keyword=laptop&categoryId=5&minPrice=100&maxPrice=500` → All filters

**`required = false` means:**
- The parameter is optional — user doesn't have to provide it
- If user doesn't provide it, the variable is `null`

**What is `Model model`?**
- It's a container to pass data to the HTML template
- Like a box where we put items, and HTML template opens the box to see items

### Lines 87-88:
```java
List<Item> items = marketplaceService.searchItems(keyword, categoryId, minPrice, maxPrice);
```

**What happens:**
1. Call the service method `searchItems()`
2. Pass the parameters (keyword, categoryId, minPrice, maxPrice)
3. Service returns a list of Item objects
4. Store that list in variable `items`

**What does the service do behind the scenes?**
- Queries the database: "Give me all approved items"
- Filters by keyword if provided
- Filters by category if provided
- Filters by price range if provided
- Returns matching items

**But the controller doesn't care HOW.** It just calls the method and gets results.

### Lines 90-95:
```java
model.addAttribute("items",              items);
model.addAttribute("categories",         marketplaceService.getAllCategories());
model.addAttribute("keyword",            keyword);
model.addAttribute("selectedCategoryId", categoryId);
model.addAttribute("minPrice",           minPrice);
model.addAttribute("maxPrice",           maxPrice);
```

**What is this doing?**
- Putting data into a box (Model) so the HTML template can use it

**Example in HTML:**
```html
<!-- In items.html -->
<div th:each="item : ${items}">
    <p th:text="${item.name}">Item Name</p>
    <p th:text="${item.price}">Item Price</p>
</div>
```

The `${items}` is the variable we put in the model above.

### Line 97:
```java
return "items";   // → templates/items.html
```

**What:**
- Returns the HTML template name "items"
- Spring automatically looks for a file called `items.html` in the templates folder
- Fills it with the data we put in the model
- Sends it to the browser

---

## SECTION 7: ITEM DETAIL METHOD (Lines 104-117)

### Line 104:
```java
@GetMapping("/item/{id}")
```

**What is `{id}`?**
- It's a placeholder for a number
- **Example URLs:**
  - `/buyer/item/5` → id = 5
  - `/buyer/item/123` → id = 123

### Line 105:
```java
public String itemDetail(@PathVariable Long id, Model model) {
```

**`@PathVariable Long id`:**
- Spring automatically extracts the `id` from the URL
- If URL is `/buyer/item/5`, then `id = 5`

### Lines 106-107:
```java
Optional<Item> optional = marketplaceService.findItemById(id);
```

**What is `Optional`?**
- A box that might contain an Item, or might be empty
- It means: "Maybe this item exists, maybe it doesn't"
- Better than `null` because it forces you to check

**Why?**
- If user visits `/buyer/item/999` and item 999 doesn't exist
- The service returns empty Optional instead of `null`
- We can check: "Is there an item here or not?"

### Lines 108-110:
```java
if (optional.isEmpty() || !optional.get().isAvailable()) {
    return "redirect:/buyer/browse";
}
```

**What happens:**
- Check 1: Is the Optional empty? (Does item exist?)
- Check 2: Is the item available? (Not sold out?)
- If EITHER check fails → Redirect to browse page
- It's like saying: "If item doesn't exist OR is sold out, send user back to browse"

**`redirect:`**
- Tells browser to go to a different URL
- User sees URL change to `/buyer/browse`

### Lines 112-113:
```java
model.addAttribute("item", optional.get());
return "item-detail";   // → templates/item-detail.html
```

- Put the item in the model
- Return the `item-detail.html` template
- Browser sees the item details page with a "Buy" button

---

## SECTION 8: PURCHASE METHOD (Lines 121-145)

### Line 121:
```java
@PostMapping("/purchase/{itemId}")
```

- **POST**: When user SENDS data (like submitting a form)
- This is called when user clicks the "Buy" button
- **Example:** User clicks "Buy" on item 5 → POST `/buyer/purchase/5`

### Lines 122-125:
```java
public String purchase(
        @PathVariable Long itemId,
        @AuthenticationPrincipal UserDetails principal,
        RedirectAttributes flash) {
```

**Parameters:**
- `itemId`: Which item to buy (from URL)
- `principal`: The logged-in user (Spring gives this automatically)
- `flash`: A box to pass messages to the next page

**`@AuthenticationPrincipal`:**
- Spring automatically provides the logged-in user
- You don't need to ask for it or fetch it from database
- It's injected automatically

### Line 127:
```java
User buyer = currentUser(principal);
```

- Convert Spring's `UserDetails` to our `User` class
- Use the helper method we created earlier
- Now we have the full User object with email, password, role, etc.

### Lines 129-136:
```java
try {
    Order order = marketplaceService.purchaseItem(itemId, buyer);
    flash.addFlashAttribute("successMessage",
            "Purchase successful! Order #" + order.getId() + " confirmed.");
    return "redirect:/buyer/orders";
```

**What is `try`?**
- It means: "Try to run the code inside. If something goes wrong, don't crash — instead, jump to the `catch` block."

**Line 130:**
- Call service to purchase item
- Service validates: Is item approved? Is it the seller trying to buy? Etc.
- If all OK → Creates an Order object and returns it
- If something wrong → Throws an exception (error)

**Lines 131-133:**
- If purchase successful:
  - Create a success message
  - Use `flash` to pass that message to the next page
  - Flash messages are like sticky notes — they show up once then disappear

**Line 134:**
- Redirect to `/buyer/orders` (purchase history page)
- User sees "Order #123 confirmed!"

### Lines 136-140:
```java
} catch (ItemNotAvailableException | UnauthorizedActionException ex) {
    flash.addFlashAttribute("errorMessage", ex.getMessage());
    return "redirect:/buyer/item/" + itemId;
}
```

**What is `catch`?**
- If the `try` block fails and throws an exception, run this code

**Two possible exceptions:**
1. `ItemNotAvailableException` → Item is sold out
2. `UnauthorizedActionException` → User is trying to buy their own item

**What it does:**
- Catch the error message from the exception
- Put it in flash message: "Item not available" or "You cannot buy your own item"
- Redirect back to item page so user sees the error

---

## SECTION 9: PURCHASE HISTORY METHOD (Lines 148-159)

### Line 148:
```java
@GetMapping("/orders")
```

- **GET request** to `/buyer/orders`
- User goes to "My Purchases" page
- URL: `/buyer/orders`

### Lines 149-152:
```java
public String purchaseHistory(
        @AuthenticationPrincipal UserDetails principal,
        Model model) {
```

- `principal`: Current logged-in user (auto-injected by Spring)
- `model`: Box to put data for the HTML template

### Line 154:
```java
User buyer = currentUser(principal);
```

- Convert to our User class (same helper method as before)

### Line 155:
```java
List<Order> orders = marketplaceService.getPurchaseHistory(buyer);
```

- Call service: "Give me all orders from this buyer"
- Service queries database and returns a list of orders

### Line 156:
```java
model.addAttribute("orders", orders);
```

- Put the orders list into the model
- HTML template will loop through and display each order

### Line 157:
```java
return "purchase-history";   // → templates/purchase-history.html
```

- Render the purchase history template with the orders data

---

## COMPLETE FLOW SUMMARY

### **Flow 1: User Browses Items**
```
1. User goes to: http://yoursite.com/buyer/browse?keyword=laptop&minPrice=100
2. Spring routing → Calls browse() method
3. browse() extracts parameters:
   - keyword = "laptop"
   - minPrice = 100.00
   - categoryId = null (not provided)
   - maxPrice = null (not provided)
4. browse() calls: marketplaceService.searchItems("laptop", null, 100.00, null)
5. Service queries database and returns matching items
6. browse() puts items in model
7. browse() returns "items" template name
8. Spring renders items.html with the items data
9. Browser shows list of laptops over $100
```

### **Flow 2: User Buys an Item**
```
1. User clicks "Buy" button on item with ID 5
2. Browser submits: POST /buyer/purchase/5
3. Spring routing → Calls purchase(5, principal, flash)
4. purchase() extracts current user from principal
5. purchase() calls: marketplaceService.purchaseItem(5, buyer)
6. Service checks:
   - Is item 5 available? ✓
   - Is buyer the seller? ✗ (good, not seller)
   - Create Order object
   - Mark item as SOLD
   - Return Order
7. purchase() receives Order object (no exception thrown)
8. purchase() adds success message to flash
9. purchase() returns redirect to /buyer/orders
10. Browser redirects
11. Spring calls purchaseHistory()
12. purchaseHistory() fetches all orders from buyer
13. purchaseHistory() returns purchase-history template
14. Browser shows "Order #456 confirmed!" + list of orders
```

### **Flow 3: User Buys Own Item (Error Flow)**
```
1. User clicks "Buy" on their own item (ID 10)
2. Browser submits: POST /buyer/purchase/10
3. Spring routing → Calls purchase(10, principal, flash)
4. purchase() extracts current user
5. purchase() calls: marketplaceService.purchaseItem(10, buyer)
6. Service checks:
   - Is item 10 available? ✓
   - Is buyer the seller? ✗ YES they are (bad!)
   - Throws UnauthorizedActionException("You cannot buy your own item")
7. purchase() CATCHES the exception in catch block
8. purchase() extracts message: "You cannot buy your own item"
9. purchase() adds error to flash
10. purchase() returns redirect to /buyer/item/10
11. Browser redirects
12. Spring calls itemDetail(10, model)
13. itemDetail() fetches item 10 from service
14. itemDetail() returns item-detail template
15. Browser shows item detail page + error message "You cannot buy your own item"
```

---

## KEY CONCEPTS TO REMEMBER

### **@Controller**
- Marks this class as handling HTTP requests
- Spring automatically creates an instance

### **@RequestMapping / @GetMapping / @PostMapping**
- Map URLs to methods
- GET = user requesting data
- POST = user sending data

### **Dependency Injection**
```java
public BuyerController(MarketplaceService marketplaceService, AuthService authService) {
    this.marketplaceService = marketplaceService;
    this.authService = authService;
}
```
- Spring gives us the services we need
- We don't create them ourselves
- Makes code testable

### **@AuthenticationPrincipal**
- Spring automatically gives us the logged-in user
- No need to fetch from database manually

### **Model.addAttribute()**
- Puts data into a box
- HTML template opens the box and uses the data

### **Flash Messages**
- Messages that show once then disappear
- Used for success/error messages

### **Optional<T>**
- Maybe the value exists, maybe it doesn't
- Forces you to check before using

### **Try-Catch**
- Try: Run normal code
- Catch: If something goes wrong, handle the error

---

## DESIGN PATTERNS FOUND HERE

| Pattern | Where | Why |
|---------|-------|-----|
| **MVC** | Controller → Service → Template | Separation of concerns |
| **Dependency Injection** | Constructor receives services | Testability, flexibility |
| **Facade** | Service methods hide complexity | Clean interface |
| **SRP** | Only buyer logic here | Focused responsibility |
| **Low Coupling** | Calls services, not repositories | Isolated from database |
| **High Cohesion** | All methods are buyer-related | Related code together |
| **Protected Variation** | Catches exceptions, shows friendly messages | Shield UI from errors |

