package com.cannatrace.domain.model

enum class LocationType(val displayName: String) {
    SERRE("Serre"),
    CHAMBRE_CULTURE("Chambre de Culture"),
    LABORATOIRE("Laboratoire"),
    ENTREPOT("Entrepôt"),
    DISPENSAIRE("Dispensaire")
}

data class Location(
    val id: String,
    val name: String,
    val type: LocationType,
    val address: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
