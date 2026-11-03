package com.cannatrace.data.repository

import androidx.room.withTransaction
import com.cannatrace.BuildConfig
import com.cannatrace.data.local.database.CannaTraceDatabase
import com.cannatrace.data.local.database.entities.*
import com.cannatrace.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Single-device fictional beta. No remote authentication or synchronization is claimed. */
@Singleton
class LocalTraceStore @Inject constructor(private val db: CannaTraceDatabase) {
    private val gson = Gson()
    private val session = MutableStateFlow<DemoRole?>(null)
    val role = session.asStateFlow()
    val batches = db.batchDao().getAllBatches()
    val metadata = db.traceDao().metadata()
    val events = db.traceDao().events()

    fun enterDemo(role: DemoRole) { check(BuildConfig.DEBUG); session.value = role }
    fun logout() { session.value = null }

    private fun actor(action: String): String {
        check(BuildConfig.DEBUG) { "Service réservé à la bêta locale" }
        val role = requireNotNull(session.value) { "Ouvrez une session de démonstration" }
        TraceRules.authorize(role, action)
        return "demo:${role.name}"
    }

    suspend fun create(operationId: String, id: String, name: String, category: String,
                       description: String, origin: String, supplier: String, amount: String,
                       unit: String, location: String): String = db.withTransaction {
        val actor = actor("CREATE")
        TraceRules.uuid(id); TraceRules.uuid(operationId)
        val q = TraceRules.quantity(amount)
        require(unit in TraceRules.units && name.isNotBlank() && category.isNotBlank() && location.isNotBlank()) {
            "Désignation, catégorie, unité et emplacement obligatoires"
        }
        require(listOf(name, category, description, origin, supplier, location).all { it.length <= 2000 })
        val request = gson.toJson(listOf(id, name.trim(), category.trim(), description.trim(), origin.trim(), supplier.trim(), q.toPlainString(), unit, location.trim()))
        val fingerprint = digest("CREATE", actor, request)
        if (replay(operationId, fingerprint)) return@withTransaction id
        val timestamp = nextTime()
        val batch = BatchEntity(id, "DEMO-${id}", name.trim(), "STOCKAGE", q.toDouble(),
            0.0, 0.0, timestamp, timestamp, "demo-local", location.trim())
        val meta = TraceMetadata(id, category.trim(), description.trim(), origin.trim(), supplier.trim(), unit)
        db.batchDao().insertBatch(batch)
        db.traceDao().insertMetadata(meta)
        if (q.signum() > 0) stock(operationId, batch, meta, q.toDouble(), "ENTREE", "Stock initial", actor, timestamp)
        append(operationId, id, "CREATE", actor, "", snapshot(batch, meta), "Création fictive", fingerprint, timestamp)
        id
    }

    suspend fun change(operationId: String, id: String, revision: Long, action: String,
                       value: String, reason: String) = db.withTransaction {
        val actor = actor(action)
        TraceRules.uuid(operationId)
        require(action in setOf("ENTREE", "SORTIE", "CORRECTION", "TRANSFERT", "STATUT", "QUALITE", "ARCHIVE", "DESIGNATION", "DESCRIPTION", "CATEGORIE", "ORIGINE", "FOURNISSEUR"))
        require(reason.trim().length in 5..2000) { "Motif obligatoire (5 à 2000 caractères)" }
        require(value.length <= 2000)
        val fingerprint = digest(action, actor, gson.toJson(listOf(id, revision.toString(), value, reason)))
        if (replay(operationId, fingerprint)) return@withTransaction
        val before = requireNotNull(db.batchDao().getBatchById(id)) { "Lot inconnu" }
        val meta = requireNotNull(db.traceDao().metadata(id)) { "Lot ancien : import contrôlé requis" }
        check(meta.revision == revision) { "Le lot a changé. Rechargez sa fiche." }
        check(!before.isClosed) { "Lot archivé : modification interdite" }
        val timestamp = nextTime()
        var after = before.copy(updatedAt = timestamp)
        var nextMeta = meta.copy(revision = meta.revision + 1)
        when (action) {
            "ENTREE", "SORTIE", "CORRECTION" -> {
                val balance = TraceRules.balance(before.weight.toString(), action, value)
                val delta = balance.toDouble() - before.weight
                require(delta != 0.0) { "Aucun changement de quantité" }
                stock(operationId, before, meta, kotlin.math.abs(delta), if (delta > 0) "ENTREE" else "SORTIE_PERTE", reason, actor, timestamp)
                after = after.copy(weight = balance.toDouble())
            }
            "TRANSFERT" -> {
                require(value.isNotBlank() && value.trim() != before.locationId) { "Nouvel emplacement requis" }
                after = after.copy(locationId = value.trim())
            }
            "STATUT" -> {
                require(value in setOf("STOCKAGE", "CONTROLE_QUALITE", "CONDITIONNEMENT", "TRANSFORMATION", "SECHAGE", "RECOLTE", "CROISSANCE", "GERMINATION", "GRAINE")) { "Statut invalide" }
                require(value != before.status) { "Statut inchangé" }
                after = after.copy(status = value)
            }
            "ARCHIVE" -> after = after.copy(isClosed = true, status = "CLOTURE")
            "QUALITE" -> require(value.isNotBlank()) { "Résultat du contrôle requis" }
            "DESIGNATION" -> { require(value.isNotBlank()); after = after.copy(strainName = value.trim()) }
            "DESCRIPTION" -> nextMeta = nextMeta.copy(description = value.trim())
            "CATEGORIE" -> { require(value.isNotBlank()); nextMeta = nextMeta.copy(category = value.trim()) }
            "ORIGINE" -> nextMeta = nextMeta.copy(origin = value.trim())
            "FOURNISSEUR" -> nextMeta = nextMeta.copy(supplier = value.trim())
        }
        db.batchDao().updateBatch(after)
        db.traceDao().updateMetadata(nextMeta)
        // Quality result is recorded even though it does not alter the lot's fields.
        val result = gson.toJson(listOf(snapshot(after, nextMeta), if (action == "QUALITE") value else ""))
        append(operationId, id, action, actor, snapshot(before, meta), result, reason.trim(), fingerprint, timestamp)
    }

    private suspend fun replay(id: String, fingerprint: String): Boolean {
        val old = db.traceDao().operation(id) ?: return false
        check(old.fingerprint == fingerprint) { "Référence d'opération déjà utilisée avec un autre contenu" }
        return true
    }
    private suspend fun nextTime(): Long = maxOf(System.currentTimeMillis(), (db.traceDao().chain().lastOrNull()?.timestamp ?: 0) + 1)
    private fun snapshot(batch: BatchEntity, meta: TraceMetadata) = gson.toJson(listOf(batch, meta))
    private suspend fun stock(id: String, batch: BatchEntity, meta: TraceMetadata, amount: Double,
                              type: String, reason: String, actor: String, timestamp: Long) {
        db.stockEntryDao().insertStockEntry(StockEntryEntity(id, batch.id, type, amount, meta.unit, reason, actor, timestamp))
    }
    private suspend fun append(id: String, batchId: String, action: String, actor: String,
                               before: String, after: String, reason: String, fingerprint: String, timestamp: Long) {
        val previous = db.traceDao().chain().lastOrNull()?.hash ?: "GENESIS"
        val hash = eventHash(id, batchId, action, timestamp, actor, before, after, reason, fingerprint, previous)
        db.traceDao().append(TraceEvent(id, batchId, action, timestamp, actor, before, after, reason, fingerprint, previous, hash))
    }
    suspend fun verifyChain(): Boolean = db.withTransaction {
        var previous = "GENESIS"
        db.traceDao().chain().all { e ->
            val ok = e.previousHash == previous && e.hash == eventHash(e.operationId, e.batchId, e.action, e.timestamp, e.actor, e.beforeValue, e.afterValue, e.reason, e.fingerprint, e.previousHash)
            previous = e.hash
            ok
        }
    }
    private fun eventHash(id: String, batchId: String, action: String, time: Long, actor: String,
                          before: String, after: String, reason: String, fingerprint: String, previous: String) =
        digest(id, batchId, action, time.toString(), actor, before, after, reason, fingerprint, previous)
    private fun digest(vararg fields: String): String = MessageDigest.getInstance("SHA-256")
        .digest(gson.toJson(fields).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
