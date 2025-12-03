package org.keon.book

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["org.keon.book"])
@ConfigurationPropertiesScan
class BookApplication

fun main(args: Array<String>) {
    runApplication<BookApplication>(*args)
}
