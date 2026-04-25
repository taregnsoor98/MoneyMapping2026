package com.moneymapping.backend

// ─── Modulith import ──────────────────────────────────────────────────────────
import com.sirolf2009.modulith.account.AccountModule  // the JWT library that needs RSA keys configured

// ─── Spring imports ───────────────────────────────────────────────────────────
import jakarta.annotation.PostConstruct               // marks a function to run automatically after Spring starts
import org.springframework.boot.test.context.TestConfiguration  // marks this class as test-only Spring configuration

// ─────────────────────────────────────────────────────────────────────────────
// WHY THIS CLASS EXISTS:
// The real app sets AccountModule keys inside main() — but tests never call main().
// This class runs the same setup when the TEST Spring context starts.
// Without this, the JWT library cannot sign or verify tokens — login always fails.
// @TestConfiguration means Spring only loads this class during tests, never in production.
// ─────────────────────────────────────────────────────────────────────────────
@TestConfiguration
class TestConfig {

    @PostConstruct
    // @PostConstruct means: run this function automatically after Spring has finished
    // setting up all the beans — this is when we know AccountModule exists and is ready
    fun initModulith() {
        println("Working directory: ${System.getProperty("user.dir")}")
        // prints the folder Java is running from — helps us verify the PEM file paths are correct

        AccountModule.issuer = "MoneyMapping"
        // sets the JWT token issuer name — must match what the real app sets in main()

        AccountModule.privatePem = "src/main/resources/MoneyMapping-dev.private.pem"
        // path to the private RSA key — used to SIGN tokens when a user logs in

        AccountModule.publicPem = "src/main/resources/MoneyMapping-dev.public.pem"
        // path to the public RSA key — used to VERIFY tokens on protected endpoints

        AccountModule.hibernateConfiguration = "src/main/resources/hibernate.cfg.xml"
        // hibernate config the modulith library needs internally to manage its own data
    }
}