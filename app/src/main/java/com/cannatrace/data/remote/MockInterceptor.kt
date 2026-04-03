package com.cannatrace.data.remote

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Intercepteur OkHttp de mock pour la démonstration locale sans backend réel.
 * Simule les réponses du serveur CannaTrace.
 *
 * Activer en passant useMock = true dans NetworkModule.
 */
class MockInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method

        val json = when {
            // Auth
            url.contains("/auth/login") && method == "POST" -> AUTH_LOGIN_RESPONSE
            url.contains("/auth/refresh") -> AUTH_REFRESH_RESPONSE

            // Batches
            url.contains("/batches") && method == "GET" && !url.contains("/transition") ->
                BATCHES_LIST_RESPONSE
            url.contains("/batches") && method == "POST" -> BATCH_CREATE_RESPONSE
            url.contains("/batches") && url.contains("/transition") -> BATCH_TRANSITION_RESPONSE

            // Audit logs
            url.contains("/audit-logs/latest") -> AUDIT_LOG_LATEST_RESPONSE
            url.contains("/audit-logs") && method == "GET" -> AUDIT_LOGS_LIST_RESPONSE
            url.contains("/audit-logs") && method == "POST" -> AUDIT_LOG_CREATE_RESPONSE

            // Stock
            url.contains("/stock-entries") && method == "GET" -> STOCK_ENTRIES_RESPONSE
            url.contains("/stock-entries") && method == "POST" -> STOCK_ENTRY_CREATE_RESPONSE

            // Prescriptions
            url.contains("/prescriptions") && method == "GET" -> PRESCRIPTIONS_RESPONSE
            url.contains("/prescriptions") && method == "POST" -> PRESCRIPTION_CREATE_RESPONSE

            // Rapports
            url.contains("/reports/inventory") -> INVENTORY_REPORT_RESPONSE

            else -> """{"message": "OK"}"""
        }

        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(json.toResponseBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
    }

    companion object {
        private val AUTH_LOGIN_RESPONSE = """
            {
                "accessToken": "mock_access_token_cannatrace_2024",
                "refreshToken": "mock_refresh_token_cannatrace_2024",
                "expiresIn": 3600,
                "userId": "user-001",
                "role": "PRODUCTEUR"
            }
        """.trimIndent()

        private val AUTH_REFRESH_RESPONSE = """
            {
                "accessToken": "mock_access_token_refreshed_2024",
                "refreshToken": "mock_refresh_token_refreshed_2024",
                "expiresIn": 3600,
                "userId": "user-001",
                "role": "PRODUCTEUR"
            }
        """.trimIndent()

        private val BATCHES_LIST_RESPONSE = """
            {
                "content": [
                    {
                        "id": "batch-001",
                        "batchNumber": "LOT-2024-001",
                        "strainName": "Bedrocan",
                        "status": "STOCKAGE",
                        "weight": 1500.0,
                        "thcContent": 22.0,
                        "cbdContent": 0.1,
                        "createdAt": 1704067200000,
                        "updatedAt": 1704067200000,
                        "ownerId": "user-001",
                        "locationId": "loc-001",
                        "parentBatchId": null,
                        "isClosed": false,
                        "hashChain": "abc123def456",
                        "notes": ""
                    },
                    {
                        "id": "batch-002",
                        "batchNumber": "LOT-2024-002",
                        "strainName": "Tilray T10",
                        "status": "TRANSFORMATION",
                        "weight": 800.0,
                        "thcContent": 10.0,
                        "cbdContent": 0.5,
                        "createdAt": 1704153600000,
                        "updatedAt": 1704153600000,
                        "ownerId": "user-001",
                        "locationId": "loc-002",
                        "parentBatchId": null,
                        "isClosed": false,
                        "hashChain": "def789ghi012",
                        "notes": ""
                    }
                ],
                "totalElements": 2,
                "totalPages": 1,
                "page": 0,
                "size": 50
            }
        """.trimIndent()

        private val BATCH_CREATE_RESPONSE = """
            {
                "id": "batch-new-001",
                "batchNumber": "LOT-2024-NEW",
                "strainName": "Nouveau Lot",
                "status": "GRAINE",
                "weight": 0.0,
                "thcContent": 0.0,
                "cbdContent": 0.0,
                "createdAt": 1704240000000,
                "updatedAt": 1704240000000,
                "ownerId": "user-001",
                "locationId": "loc-001",
                "isClosed": false,
                "hashChain": "",
                "notes": ""
            }
        """.trimIndent()

        private val BATCH_TRANSITION_RESPONSE = BATCH_CREATE_RESPONSE

        private val AUDIT_LOGS_LIST_RESPONSE = """
            {
                "content": [
                    {
                        "id": "audit-001",
                        "action": "CREATE_BATCH",
                        "entityType": "BATCH",
                        "entityId": "batch-001",
                        "userId": "user-001",
                        "timestamp": 1704067200000,
                        "hash": "a1b2c3d4e5f6",
                        "previousHash": "GENESIS",
                        "data": "batchNumber=LOT-2024-001;strain=Bedrocan"
                    }
                ],
                "totalElements": 1,
                "totalPages": 1,
                "page": 0,
                "size": 100
            }
        """.trimIndent()

        private val AUDIT_LOG_LATEST_RESPONSE = """
            {
                "id": "audit-001",
                "action": "CREATE_BATCH",
                "entityType": "BATCH",
                "entityId": "batch-001",
                "userId": "user-001",
                "timestamp": 1704067200000,
                "hash": "a1b2c3d4e5f6",
                "previousHash": "GENESIS",
                "data": "batchNumber=LOT-2024-001;strain=Bedrocan"
            }
        """.trimIndent()

        private val AUDIT_LOG_CREATE_RESPONSE = """
            {
                "id": "audit-new-001",
                "action": "SYNC",
                "entityType": "BATCH",
                "entityId": "batch-001",
                "userId": "user-001",
                "timestamp": 1704240000000,
                "hash": "newhashabc123",
                "previousHash": "a1b2c3d4e5f6",
                "data": "synchronized"
            }
        """.trimIndent()

        private val STOCK_ENTRIES_RESPONSE = """
            {
                "content": [],
                "totalElements": 0,
                "totalPages": 0,
                "page": 0,
                "size": 100
            }
        """.trimIndent()

        private val STOCK_ENTRY_CREATE_RESPONSE = """
            {
                "id": "stock-new-001",
                "batchId": "batch-001",
                "movementType": "ENTREE",
                "quantity": 1000.0,
                "unit": "g",
                "reason": "Réception lot initial",
                "operatorId": "user-001",
                "timestamp": 1704240000000
            }
        """.trimIndent()

        private val PRESCRIPTIONS_RESPONSE = """[]"""

        private val PRESCRIPTION_CREATE_RESPONSE = """
            {
                "id": "presc-new-001",
                "anonymizedPatientId": "PAT-ANON-001",
                "prescriberId": "user-presc-001",
                "batchId": "batch-001",
                "dosage": "10mg",
                "frequency": "2x par jour",
                "startDate": 1704240000000,
                "endDate": 1706832000000,
                "adverseEffects": ""
            }
        """.trimIndent()

        private val INVENTORY_REPORT_RESPONSE = """
            {
                "reportDate": "2024-01-15",
                "totalActiveBatches": 2,
                "totalStock": 2300.0,
                "unit": "g",
                "batches": []
            }
        """.trimIndent()
    }
}
