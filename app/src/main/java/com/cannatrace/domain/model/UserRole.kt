package com.cannatrace.domain.model

enum class UserRole(val displayName: String, val level: Int) {
    PRODUCTEUR("Producteur", 1),
    TRANSFORMATEUR("Transformateur", 2),
    DISPENSAIRE("Dispensaire", 3),
    PRESCRIPTEUR("Prescripteur", 4),
    PATIENT("Patient", 5),
    INSPECTEUR_ANSM("Inspecteur ANSM", 6)
}
