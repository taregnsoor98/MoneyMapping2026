package com.moneymapping.backend

import com.sirolf2009.modulith.account.AccountModule // your friend's auth module
import org.springframework.boot.autoconfigure.SpringBootApplication // marks this as a Spring Boot app
import org.springframework.boot.runApplication // function to start the Spring Boot app
import org.springframework.scheduling.annotation.EnableAsync // enables background/async processing across the whole app

@SpringBootApplication
@EnableAsync // enables async so EmailService can send emails in the background
class BackendApplication

fun main(args: Array<String>) {
    AccountModule.issuer = "MoneyMapping" // sets the JWT token issuer name
    AccountModule.privatePem = "backend/src/main/resources/MoneyMapping-dev.private.pem" // path to private key for signing tokens
    AccountModule.publicPem = "backend/src/main/resources/MoneyMapping-dev.public.pem" // path to public key for verifying tokens
    AccountModule.hibernateConfiguration = "backend/src/main/resources/hibernate.cfg.xml" // path to hibernate config for friend's auth library
    runApplication<BackendApplication>(*args) // starts the Spring Boot application
}