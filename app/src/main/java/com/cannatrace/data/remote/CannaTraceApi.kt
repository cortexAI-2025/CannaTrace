package com.cannatrace.data.remote

import com.cannatrace.data.local.database.entities.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit pour la communication avec le serveur central CannaTrace.
 * Les endpoints respectent les exigences de traçabilité ANSM.
 */
interface CannaTraceApi {

    // ─── Authentification ───────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthTokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthTokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    // ─── Lots (Batches) ───────────────────────────────────────────────────

    @GET("batches")
    suspend fun getBatches(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("status") status: String? = null
    ): Response<PagedResponse<BatchEntity>>

    @GET("batches/{id}")
    suspend fun getBatchById(@Path("id") id: String): Response<BatchEntity>

    @POST("batches")
    suspend fun createBatch(@Body batch: BatchEntity): Response<BatchEntity>

    @PUT("batches/{id}")
    suspend fun updateBatch(
        @Path("id") id: String,
        @Body batch: BatchEntity
    ): Response<BatchEntity>

    @POST("batches/{id}/transition")
    suspend fun transitionBatchStatus(
        @Path("id") id: String,
        @Query("newStatus") newStatus: String,
        @Query("reason") reason: String = ""
    ): Response<BatchEntity>

    @POST("batches/{id}/correction-notes")
    suspend fun addCorrectionNote(
        @Path("id") batchId: String,
        @Body note: CorrectionNoteEntity
    ): Response<CorrectionNoteEntity>

    // ─── Plantes / Graines ───────────────────────────────────────────────

    @GET("plants")
    suspend fun getPlants(@Query("batchId") batchId: String? = null): Response<List<PlantEntity>>

    @GET("plants/qr/{qrCode}")
    suspend fun getPlantByQrCode(@Path("qrCode") qrCode: String): Response<PlantEntity>

    @POST("plants")
    suspend fun createPlant(@Body plant: PlantEntity): Response<PlantEntity>

    @PUT("plants/{id}")
    suspend fun updatePlant(
        @Path("id") id: String,
        @Body plant: PlantEntity
    ): Response<PlantEntity>

    // ─── Journal de culture ──────────────────────────────────────────────

    @GET("culture-entries")
    suspend fun getCultureEntries(
        @Query("batchId") batchId: String
    ): Response<List<CultureEntryEntity>>

    @POST("culture-entries")
    suspend fun createCultureEntry(@Body entry: CultureEntryEntity): Response<CultureEntryEntity>

    // ─── Récolte ────────────────────────────────────────────────────────

    @GET("harvests/{batchId}")
    suspend fun getHarvestByBatch(@Path("batchId") batchId: String): Response<HarvestEntity>

    @POST("harvests")
    suspend fun createHarvest(@Body harvest: HarvestEntity): Response<HarvestEntity>

    // ─── Transformation ─────────────────────────────────────────────────

    @GET("transformations")
    suspend fun getTransformations(@Query("batchId") batchId: String): Response<List<TransformationEntity>>

    @POST("transformations")
    suspend fun createTransformation(@Body transformation: TransformationEntity): Response<TransformationEntity>

    // ─── Conditionnement ────────────────────────────────────────────────

    @GET("packaging")
    suspend fun getPackaging(@Query("batchId") batchId: String): Response<List<PackagingEntity>>

    @POST("packaging")
    suspend fun createPackaging(@Body packaging: PackagingEntity): Response<PackagingEntity>

    // ─── Stock ──────────────────────────────────────────────────────────

    @GET("stock-entries")
    suspend fun getStockEntries(
        @Query("batchId") batchId: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PagedResponse<StockEntryEntity>>

    @POST("stock-entries")
    suspend fun createStockEntry(@Body entry: StockEntryEntity): Response<StockEntryEntity>

    // ─── Prescriptions ──────────────────────────────────────────────────

    @GET("prescriptions")
    suspend fun getPrescriptions(
        @Query("patientId") patientId: String? = null
    ): Response<List<PrescriptionEntity>>

    @POST("prescriptions")
    suspend fun createPrescription(@Body prescription: PrescriptionEntity): Response<PrescriptionEntity>

    @PUT("prescriptions/{id}")
    suspend fun updatePrescription(
        @Path("id") id: String,
        @Body prescription: PrescriptionEntity
    ): Response<PrescriptionEntity>

    // ─── Logs d'audit ───────────────────────────────────────────────────

    @GET("audit-logs")
    suspend fun getAuditLogs(
        @Query("entityId") entityId: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PagedResponse<AuditLogEntity>>

    @POST("audit-logs")
    suspend fun createAuditLog(@Body auditLog: AuditLogEntity): Response<AuditLogEntity>

    @GET("audit-logs/latest")
    suspend fun getLatestAuditLog(): Response<AuditLogEntity>

    // ─── Rapports ANSM ──────────────────────────────────────────────────

    @GET("reports/inventory")
    suspend fun getInventoryReport(
        @Query("startDate") startDate: Long,
        @Query("endDate") endDate: Long
    ): Response<String> // JSON string for CSV/PDF generation

    @GET("reports/audit-trail/{batchId}")
    suspend fun getAuditTrailReport(@Path("batchId") batchId: String): Response<String>
}
