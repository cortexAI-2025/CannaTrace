package com.cannatrace.presentation.navigation

/**
 * Définition de toutes les routes de navigation de l'application CannaTrace.
 */
sealed class Screen(val route: String) {

    // Authentification
    data object Login : Screen("login")

    // Navigation principale
    data object Dashboard : Screen("dashboard")
    data object BatchList : Screen("batches")
    data object CreateBatch : Screen("batches/create")
    data object BatchDetail : Screen("batches/{batchId}") {
        fun createRoute(batchId: String) = "batches/$batchId"
    }
    data object CultureJournal : Screen("batches/{batchId}/culture") {
        fun createRoute(batchId: String) = "batches/$batchId/culture"
    }
    data object Stock : Screen("stock")
    data object QrScanner : Screen("scanner")
    data object Reports : Screen("reports")
    data object Prescriptions : Screen("prescriptions")
    data object AuditLogs : Screen("audit")
}
