package com.dndsaas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = ["com.dndsaas.repository"])
@EnableScheduling
class DndSaasApplication

fun main(args: Array<String>) {
    runApplication<DndSaasApplication>(*args)
}
