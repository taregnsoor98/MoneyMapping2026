package com.moneymapping.backend

import com.sirolf2009.modulith.account.AccountModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BackendApplication

fun main(args: Array<String>) {
    AccountModule.issuer = "MoneyMapping"
    AccountModule.privatePem = "backend/src/main/resources/MoneyMapping-dev.private.pem"
    AccountModule.publicPem = "backend/src/main/resources/MoneyMapping-dev.public.pem"
    AccountModule.hibernateConfiguration = "backend/src/main/resources/hibernate.cfg.xml"
    runApplication<BackendApplication>(*args)
}