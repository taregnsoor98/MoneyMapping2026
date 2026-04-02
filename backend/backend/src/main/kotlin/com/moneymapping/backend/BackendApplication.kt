package com.moneymapping.backend

import com.sirolf2009.modulith.account.AccountModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BackendApplication

fun main(args: Array<String>) {
    AccountModule.issuer = "MoneyMapping"
    AccountModule.privatePem = "src/main/resources/MoneyMapping-dev.private.pem"
    AccountModule.publicPem = "src/main/resources/MoneyMapping-dev.public.pem"
    AccountModule.hibernateConfiguration = "src/main/resources/hibernate.cfg.xml"
    runApplication<BackendApplication>(*args)
}
