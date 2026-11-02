package com.demo.taskmanager.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material 3 date pickers return UTC midnight. A local zone conversion would shift the day (DES-P2-008). */
fun utcMillisToIsoDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

fun isoDateToUtcMillis(date: String): Long =
    LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun isSelectableDay(utcMillis: Long, today: LocalDate): Boolean =
    !Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
