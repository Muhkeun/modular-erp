package com.modularerp.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.modularerp"])
@EntityScan(basePackages = ["com.modularerp"])
@EnableJpaRepositories(basePackages = ["com.modularerp"])
@EnableScheduling
class ModularErpApplication

fun main(args: Array<String>) {
    runApplication<ModularErpApplication>(*args)
}
