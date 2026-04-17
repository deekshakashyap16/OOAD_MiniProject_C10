package com.pes.marketplace.config;

import com.pes.marketplace.model.*;
import com.pes.marketplace.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Seeds the database with demo data on first application start.
 * Runs only when the users table is empty (idempotent).
 *
 * ── SOLID ──────────────────────────────────────────────────────────────────
 * SRP : Only handles data initialisation — zero business logic here.
 *
 * ── GRASP ──────────────────────────────────────────────────────────────────
 * Pure Fabrication : A helper that exists for operational convenience;
 *                    not part of the real-world marketplace domain.
 *
 * ── Design Pattern ─────────────────────────────────────────────────────────
 * Singleton : The @Bean method is called once; Spring caches the CommandLineRunner.
 *
 * Default credentials printed to console on startup:
 *   admin@campus.edu  / admin123
 *   alice@campus.edu  / pass123   (Seller)
 *   bob@campus.edu    / pass123   (Seller)
 *   charlie@campus.edu/ pass123   (Buyer)
 *   diana@campus.edu  / pass123   (Buyer)
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(
            UserRepository     userRepo,
            CategoryRepository categoryRepo,
            ItemRepository     itemRepo,
            PasswordEncoder    encoder) {

        return args -> {

            // Guard: run only on first start
            if (userRepo.count() > 0) return;

            // ── Categories ──────────────────────────────────────────────────
            Category books       = categoryRepo.save(new Category("Books & Notes",    "Textbooks, notes, study material"));
            Category electronics = categoryRepo.save(new Category("Electronics",       "Laptops, phones, accessories"));
            Category clothing    = categoryRepo.save(new Category("Clothing",          "Clothes, shoes, accessories"));
            Category stationery  = categoryRepo.save(new Category("Stationery",        "Pens, notebooks, art supplies"));
            Category sports      = categoryRepo.save(new Category("Sports & Fitness",  "Gym equipment, sports gear"));

            // ── Admin ────────────────────────────────────────────────────────
            userRepo.save(new User("Admin User",    "admin@campus.edu",
                                   encoder.encode("admin123"), Role.ADMIN));

            // ── Sellers ──────────────────────────────────────────────────────
            User alice = userRepo.save(new User("Alice Seller", "alice@campus.edu",
                                                encoder.encode("pass123"), Role.SELLER));
            User bob   = userRepo.save(new User("Bob Seller",   "bob@campus.edu",
                                                encoder.encode("pass123"), Role.SELLER));

            // ── Buyers ───────────────────────────────────────────────────────
            userRepo.save(new User("Charlie Buyer", "charlie@campus.edu",
                                   encoder.encode("pass123"), Role.BUYER));
            userRepo.save(new User("Diana Buyer",   "diana@campus.edu",
                                   encoder.encode("pass123"), Role.BUYER));

            // ── Sample Items ─────────────────────────────────────────────────
            Item i1 = new Item("CLRS – Data Structures Textbook",
                    "Cormen 4th edition, excellent condition, minimal highlighting",
                    new BigDecimal("450.00"), alice, books);
            i1.setStatus(ItemStatus.APPROVED);
            itemRepo.save(i1);

            Item i2 = new Item("Casio fx-991 Scientific Calculator",
                    "Used for 1 semester, fully functional, comes with protective case",
                    new BigDecimal("800.00"), alice, electronics);
            i2.setStatus(ItemStatus.APPROVED);
            itemRepo.save(i2);

            Item i3 = new Item("Head First Java (3rd Edition)",
                    "No markings, like new condition",
                    new BigDecimal("350.00"), bob, books);
            i3.setStatus(ItemStatus.APPROVED);
            itemRepo.save(i3);

            Item i4 = new Item("Sony WH-1000XM4 Headphones",
                    "6 months old, noise cancellation works perfectly, original box",
                    new BigDecimal("3500.00"), bob, electronics);
            i4.setStatus(ItemStatus.PENDING_REVIEW);   // awaiting admin approval
            itemRepo.save(i4);

            Item i5 = new Item("Campus Hoodie – Size L",
                    "Official campus hoodie, size Large, worn twice, no damage",
                    new BigDecimal("600.00"), alice, clothing);
            i5.setStatus(ItemStatus.APPROVED);
            itemRepo.save(i5);

            Item i6 = new Item("Basketball",
                    "Spalding NBA official size, good grip, used 3 months",
                    new BigDecimal("900.00"), bob, sports);
            i6.setStatus(ItemStatus.PENDING_REVIEW);
            itemRepo.save(i6);

            // Console summary
            System.out.println("""
                    ╔══════════════════════════════════════════════════════╗
                    ║         Campus Marketplace — Sample Data Ready       ║
                    ╠══════════════════════════════════════════════════════╣
                    ║  Admin  : admin@campus.edu   / admin123              ║
                    ║  Seller : alice@campus.edu   / pass123               ║
                    ║  Seller : bob@campus.edu     / pass123               ║
                    ║  Buyer  : charlie@campus.edu / pass123               ║
                    ║  Buyer  : diana@campus.edu   / pass123               ║
                    ╠══════════════════════════════════════════════════════╣
                    ║  http://localhost:8080                               ║
                    ║  H2 Console: http://localhost:8080/h2-console        ║
                    ╚══════════════════════════════════════════════════════╝
                    """);
        };
    }
}
