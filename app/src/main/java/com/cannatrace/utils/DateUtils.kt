package com.cannatrace.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitaires pour la gestion des dates en locale française.
 */
object DateUtils {

    private val FRENCH_LOCALE = Locale.FRANCE

    val DATE_FORMAT_DISPLAY = SimpleDateFormat("dd/MM/yyyy", FRENCH_LOCALE)
    val DATE_TIME_FORMAT_DISPLAY = SimpleDateFormat("dd/MM/yyyy HH:mm", FRENCH_LOCALE)
    val DATE_TIME_FORMAT_FULL = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", FRENCH_LOCALE)
    val DATE_FORMAT_API = SimpleDateFormat("yyyy-MM-dd", FRENCH_LOCALE)
    val DATE_TIME_FORMAT_ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", FRENCH_LOCALE)

    fun Long.toDisplayDate(): String = DATE_FORMAT_DISPLAY.format(Date(this))

    fun Long.toDisplayDateTime(): String = DATE_TIME_FORMAT_DISPLAY.format(Date(this))

    fun Long.toFullDateTime(): String = DATE_TIME_FORMAT_FULL.format(Date(this))

    fun Long.toApiDate(): String = DATE_FORMAT_API.format(Date(this))

    fun Long.toIsoDateTime(): String = DATE_TIME_FORMAT_ISO.format(Date(this))

    /**
     * Retourne la durée écoulée en texte lisible (ex: "il y a 2 jours").
     */
    fun Long.toRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - this

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = months / 12

        return when {
            seconds < 60 -> "il y a quelques secondes"
            minutes < 60 -> "il y a $minutes minute${if (minutes > 1) "s" else ""}"
            hours < 24 -> "il y a $hours heure${if (hours > 1) "s" else ""}"
            days < 30 -> "il y a $days jour${if (days > 1) "s" else ""}"
            months < 12 -> "il y a $months mois"
            else -> "il y a $years an${if (years > 1) "s" else ""}"
        }
    }

    /**
     * Vérifie si une date d'expiration est dans les X jours à venir.
     */
    fun isExpiringWithinDays(expiryTimestamp: Long, days: Int): Boolean {
        val now = System.currentTimeMillis()
        val threshold = now + days.toLong() * 24 * 60 * 60 * 1000
        return expiryTimestamp in now..threshold
    }

    /**
     * Vérifie si un timestamp est antérieur à maintenant moins X ans (pour RGPD).
     */
    fun isOlderThanYears(timestamp: Long, years: Int): Boolean {
        val cutoff = System.currentTimeMillis() - years.toLong() * 365 * 24 * 60 * 60 * 1000
        return timestamp < cutoff
    }

    /**
     * Retourne le timestamp Unix de début de journée (minuit) pour aujourd'hui.
     */
    fun startOfToday(): Long {
        val cal = java.util.Calendar.getInstance(FRENCH_LOCALE)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Retourne le timestamp Unix de fin de journée (23:59:59) pour aujourd'hui.
     */
    fun endOfToday(): Long {
        val cal = java.util.Calendar.getInstance(FRENCH_LOCALE)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
