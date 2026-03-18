package com.modularerp.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.modularerp"])
@EntityScan(basePackages = ["com.modularerp"])
@EnableJpaRepositories(basePackages = ["com.modularerp"])
class ModularErpApplication

fun main(args: Array<String>) {
    runApplication<ModularErpApplication>(*args)
}
