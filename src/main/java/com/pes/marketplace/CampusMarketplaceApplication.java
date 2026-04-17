package com.pes.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║               Campus Marketplace — Spring Boot Application             ║
 * ║               UE23CS352B  Object Oriented Analysis & Design            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * Architecture: MVC (Model-View-Controller) via Spring MVC + Thymeleaf
 * ──────────────────────────────────────────────────────────────────────────
 *  Layer        Package                          Responsibility
 *  ──────────   ──────────────────────────────── ─────────────────────────────
 *  Model        com.pes.marketplace.model        Domain entities + enums
 *  View         src/main/resources/templates     Thymeleaf HTML templates
 *  Controller   com.pes.marketplace.controller   HTTP request handling
 *  Service      com.pes.marketplace.service      Business logic interfaces + impls
 *  Repository   com.pes.marketplace.repository   JPA data-access interfaces
 *  Security     com.pes.marketplace.security     Spring Security bridge
 *  Config       com.pes.marketplace.config       Security + data seeding
 *  DTO          com.pes.marketplace.dto          Form / transfer objects
 *  Exception    com.pes.marketplace.exception    Custom runtime exceptions
 *
 * ══════════════════════════════════════════════════════════════════════════
 * SOLID PRINCIPLES
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  S  Single Responsibility Principle
 *     Every class has exactly one reason to change.
 *     • User / Item / Order / Category  → pure data, zero business logic
 *     • AuthServiceImpl       → only user-identity operations
 *     • MarketplaceServiceImpl→ only marketplace business rules
 *     • AuthController        → only register / login HTTP flow
 *     • BuyerController       → only UC2 / UC3 / UC6
 *     • SellerController      → only UC1 / UC5
 *     • AdminController       → only UC4 / UC7 / UC8 + category & user mgmt
 *     • CustomUserDetailsService → only bridges User → Spring Security
 *     • SecurityConfig        → only security / URL-rule wiring
 *     • DataInitializer       → only demo-data seeding
 *
 *  O  Open / Closed Principle
 *     Open for extension, closed for modification.
 *     • Adding a MODERATOR role: add to Role enum + one rule in SecurityConfig;
 *       zero existing classes change.
 *     • New use cases (auction, wishlist): add to MarketplaceService interface
 *       + implement; existing controller methods are untouched.
 *
 *  L  Liskov Substitution Principle
 *     Implementations fully substitute their interfaces.
 *     • AuthServiceImpl       ≡ AuthService
 *     • MarketplaceServiceImpl≡ MarketplaceService
 *     • Spring Data JPA impls ≡ JpaRepository
 *
 *  I  Interface Segregation Principle
 *     Clients see only the methods they actually use.
 *     • AuthService           → user / auth methods only
 *     • MarketplaceService    → item / order / category / report methods only
 *     • Four separate repositories, each scoped to one entity
 *
 *  D  Dependency Inversion Principle
 *     High-level modules depend on abstractions, never concretions.
 *     • All controllers inject AuthService / MarketplaceService (interfaces)
 *     • Service impls inject repository interfaces (not Hibernate classes)
 *     • Spring injects concrete beans at runtime via constructor injection
 *
 * ══════════════════════════════════════════════════════════════════════════
 * GRASP PRINCIPLES
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  Information Expert
 *      Item.isAvailable()            → Item knows its own status
 *      Order.purchasePrice           → Order keeps a price snapshot at buy-time
 *      ItemRepository.searchItems()  → repo owns item-search JPQL
 *      OrderRepository.sumRevenue()  → repo owns revenue aggregation
 *
 *  Creator
 *      AuthServiceImpl   creates User  (has name, email, password, role)
 *      MarketplaceServiceImpl creates Item  (has ItemRequest + seller)
 *      MarketplaceServiceImpl creates Order (has buyer + item at call-time)
 *
 *  Controller (GRASP)
 *      AuthController    → first receiver of auth HTTP events
 *      BuyerController   → first receiver of buyer HTTP events
 *      SellerController  → first receiver of seller HTTP events
 *      AdminController   → first receiver of admin HTTP events
 *      All delegate immediately to services (thin controllers).
 *
 *  Low Coupling
 *      Controllers depend only on two service interfaces, never on JPA.
 *      Services depend only on repository interfaces, never on controllers.
 *      DTOs decouple the HTTP layer from domain entities.
 *
 *  High Cohesion
 *      Each controller serves exactly one actor's use cases.
 *      AuthService handles only user-identity concerns.
 *      MarketplaceService handles only marketplace-domain concerns.
 *
 *  Polymorphism
 *      Role enum     → drives URL routing in SecurityConfig
 *      ItemStatus    → drives state-transition validation in the service
 *      OrderStatus   → drives order-lifecycle operations
 *
 *  Pure Fabrication
 *      UserRepository, ItemRepository, OrderRepository, CategoryRepository
 *          → invented purely to encapsulate DB access cleanly
 *      DataInitializer   → invented for operational demo-data seeding
 *      SecurityConfig    → invented for security wiring
 *      CustomUserDetailsService → invented as an adapter bridge
 *
 *  Indirection (Adapter)
 *      CustomUserDetailsService sits between our User entity and the
 *      Spring Security authentication pipeline — neither side knows about
 *      the other's internals.
 *
 *  Protected Variation
 *      Item.isAvailable()             hides "status == APPROVED" rule
 *      moderateItem()                 enforces PENDING→APPROVED/REJECTED
 *      purchaseItem()                 enforces the full purchase-integrity rules
 *      SecurityConfig                 centralises all role-to-URL mappings
 *
 * ══════════════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  Creational
 *      Singleton      : All Spring @Service / @Repository beans are singletons
 *      Factory Method : register() and listItem() encapsulate object creation
 *
 *  Structural
 *      Facade         : AdminController unifies two services behind one interface
 *      Adapter        : CustomUserDetailsService adapts User → UserDetails
 *
 *  Behavioural
 *      Strategy       : DaoAuthenticationProvider is a swappable auth strategy
 *      Template Method: purchaseItem() follows a fixed sequence of steps
 *      State          : ItemStatus and OrderStatus model explicit state machines
 */
@SpringBootApplication
public class CampusMarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusMarketplaceApplication.class, args);
	}
}
