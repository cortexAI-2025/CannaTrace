package com.cannatrace.domain.model

import java.math.BigDecimal
import java.util.UUID

/** Pure rules shared by transaction service and tests. Quantities have at most 3 decimals. */
object TraceRules {
    val units = setOf("g", "kg", "unité")
    fun quantity(value: String): BigDecimal {
        val n = value.trim().replace(',', '.').toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Quantité invalide")
        require(n >= BigDecimal.ZERO && n <= BigDecimal("1000000000") && n.stripTrailingZeros().scale() <= 3) {
            "Quantité entre 0 et 1 milliard, 3 décimales maximum"
        }
        return n
    }
    fun balance(old: String, type: String, amount: String): BigDecimal {
        val current = quantity(old)
        val q = quantity(amount)
        val result = when (type) {
            "ENTREE" -> { require(q.signum() > 0); current + q }
            "SORTIE" -> { require(q.signum() > 0); current - q }
            "CORRECTION" -> q
            else -> throw IllegalArgumentException("Mouvement inconnu")
        }
        return quantity(result.toPlainString())
    }
    fun uuid(id: String): String {
        require(id.length == 36 && UUID.fromString(id).toString() == id) { "Identifiant invalide" }
        return id
    }
    fun qr(id: String) = "cannatrace:v1:lot:${uuid(id)}"
    fun parseQr(raw: String): String? = runCatching {
        require(raw.startsWith("cannatrace:v1:lot:"))
        uuid(raw.removePrefix("cannatrace:v1:lot:"))
    }.getOrNull()
    fun authorize(role: DemoRole, action: String) {
        require(role != DemoRole.LECTEUR) { "Profil lecteur : écriture interdite" }
        require(action != "ARCHIVE" || role == DemoRole.ADMIN) { "Archivage réservé à l'administrateur" }
    }
}

enum class DemoRole { ADMIN, OPERATEUR, LECTEUR }
