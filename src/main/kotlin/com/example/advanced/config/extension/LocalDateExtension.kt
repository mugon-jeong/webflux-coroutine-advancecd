package com.example.advanced.config.extension

import java.awt.SystemColor.text
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun String.toLocalDate(format: String): LocalDate {
    return LocalDate.parse(this, DateTimeFormatter.ofPattern(format))
}

fun LocalDate.toString(format: String):String {
    return this.format(DateTimeFormatter.ofPattern(format))
}