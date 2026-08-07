package com.dndsaas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class DndSaasApplication

fun main(args: Array<String>) {
    runApplication<DndSaasApplication>(*args)
}
