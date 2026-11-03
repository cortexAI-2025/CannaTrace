package com.cannatrace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cannatrace.data.local.database.*
import com.cannatrace.data.repository.LocalTraceStore
import com.cannatrace.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocalTraceStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun id() = UUID.randomUUID().toString()
    private fun open(name: String) = Room.databaseBuilder(context, CannaTraceDatabase::class.java, name)
        .addMigrations(TraceMigration.from1).addCallback(TraceMigration.callback).build()
    private suspend fun create(store: LocalTraceStore, batch: String, op: String = id()) =
        store.create(op, batch, "FICTIF Alpha", "Fleurs fictives", "Démonstration", "Site fictif", "Fournisseur fictif", "100", "g", "Zone A")

    @Test fun lifecycleIdempotencyHistoryAndPersistence() = runBlocking {
        val name = "test-${id()}.db"
        var db = open(name)
        try {
            val store = LocalTraceStore(db)
            store.enterDemo(DemoRole.OPERATEUR)
            val batch = id(); val op = id()
            create(store, batch, op); create(store, batch, op)
            assertEquals(1, db.traceDao().chain().size)
            assertEquals(batch, TraceRules.parseQr(TraceRules.qr(batch)))
            assertNotNull(db.batchDao().getBatchById(TraceRules.parseQr(TraceRules.qr(batch))!!))
            assertNull(db.batchDao().getBatchById(id()))
            assertTrue(runCatching { create(store, batch) }.isFailure)
            store.change(id(), batch, 1, "ENTREE", "20", "Réception fictive")
            store.change(id(), batch, 2, "TRANSFERT", "Zone B", "Déplacement fictif")
            val out = id()
            store.change(out, batch, 3, "SORTIE", "15", "Sortie fictive")
            store.change(out, batch, 3, "SORTIE", "15", "Sortie fictive")
            assertEquals(105.0, db.batchDao().getBatchById(batch)!!.weight, 0.0)
            assertEquals(105.0, db.stockEntryDao().computeCurrentStock(batch)!!, 0.0)
            assertEquals("Zone B", db.batchDao().getBatchById(batch)!!.locationId)
            assertEquals(4, db.traceDao().chain().size)
            assertTrue(store.verifyChain())
            assertTrue(runCatching { store.change(out, batch, 4, "SORTIE", "1", "Autre contenu") }.isFailure)
            assertTrue(runCatching { store.change(id(), batch, 4, "SORTIE", "106", "Sortie excessive") }.isFailure)
            assertTrue(runCatching { store.change(id(), batch, 1, "ENTREE", "1", "Révision ancienne") }.isFailure)
            assertEquals(4, db.traceDao().chain().size)
            store.change(id(), batch, 4, "QUALITE", "Contrôle visuel fictif", "Contrôle fictif")
            store.change(id(), batch, 5, "CORRECTION", "103.5", "Correction inventaire fictif")
            store.change(id(), batch, 6, "STATUT", "CONTROLE_QUALITE", "Changement fictif")
            store.change(id(), batch, 7, "DESCRIPTION", "Description corrigée", "Correction documentaire")
            store.enterDemo(DemoRole.LECTEUR)
            assertTrue(runCatching { store.change(id(), batch, 8, "ENTREE", "1", "Interdit lecteur") }.isFailure)
            store.enterDemo(DemoRole.OPERATEUR)
            assertTrue(runCatching { store.change(id(), batch, 8, "ARCHIVE", "", "Interdit opérateur") }.isFailure)
            store.enterDemo(DemoRole.ADMIN)
            store.change(id(), batch, 8, "ARCHIVE", "", "Archivage fictif")
            assertTrue(runCatching { store.change(id(), batch, 9, "ENTREE", "1", "Après archivage") }.isFailure)
            assertTrue(runCatching { db.openHelper.writableDatabase.execSQL("DELETE FROM trace_events") }.isFailure)
            assertTrue(runCatching { db.openHelper.writableDatabase.execSQL("UPDATE trace_events SET reason='falsifié'") }.isFailure)
            assertTrue(runCatching { db.openHelper.writableDatabase.execSQL("DELETE FROM batches") }.isFailure)
            store.logout()
            assertTrue(runCatching { create(store, id()) }.isFailure)
            db.close(); db = open(name)
            val reopened = LocalTraceStore(db)
            assertTrue(db.batchDao().getBatchById(batch)!!.isClosed)
            assertEquals(103.5, db.batchDao().getBatchById(batch)!!.weight, 0.0)
            assertEquals(9, db.traceDao().chain().size)
            assertTrue(reopened.verifyChain())
            // Removing SQL guards and tampering must invalidate the hash chain.
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER trace_events_no_update")
            db.openHelper.writableDatabase.execSQL("UPDATE trace_events SET actor='tampered'")
            assertFalse(reopened.verifyChain())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun concurrentWithdrawalsAndAuditFailureAreAtomic() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CannaTraceDatabase::class.java)
            .addCallback(TraceMigration.callback).build()
        try {
            val store = LocalTraceStore(db); store.enterDemo(DemoRole.OPERATEUR)
            val batch = id(); create(store, batch)
            val attempts = (1..2).map { async(Dispatchers.IO) { runCatching { store.change(id(), batch, 1, "SORTIE", "80", "Concurrent fictif") } } }.awaitAll()
            assertEquals(1, attempts.count { it.isSuccess })
            assertEquals(20.0, db.batchDao().getBatchById(batch)!!.weight, 0.0)
            assertEquals(2, db.traceDao().chain().size)
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER test_fail BEFORE INSERT ON trace_events BEGIN SELECT RAISE(ABORT, 'injected'); END")
            assertTrue(runCatching { store.change(id(), batch, 2, "ENTREE", "10", "Échec journal simulé") }.isFailure)
            assertEquals(20.0, db.batchDao().getBatchById(batch)!!.weight, 0.0)
            assertEquals(20.0, db.stockEntryDao().computeCurrentStock(batch)!!, 0.0)
            assertEquals(2, db.traceDao().metadata(batch)!!.revision.toInt())
        } finally { db.close() }
    }

    @Test fun encryptedDatabaseReopensWithSameKeyAndRejectsWrongKey() = runBlocking {
        val name = "encrypted-${id()}.db"
        val key = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        var db = CannaTraceDatabase.create(context, key.copyOf(), name)
        try {
            val store = LocalTraceStore(db); store.enterDemo(DemoRole.ADMIN)
            val batch = id(); create(store, batch)
            db.close(); db = CannaTraceDatabase.create(context, key.copyOf(), name)
            assertNotNull(db.batchDao().getBatchById(batch))
            db.close(); db = CannaTraceDatabase.create(context, ByteArray(32) { 7 }, name)
            assertTrue(runCatching { db.batchDao().getBatchById(batch) }.isFailure)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migrationPreservesExistingBatch() = runBlocking {
        val name = "migration-${id()}.db"
        var db = open(name)
        try {
            val store = LocalTraceStore(db); store.enterDemo(DemoRole.ADMIN)
            val batch = id(); create(store, batch)
            // Reconstruct the predecessor schema with existing batch data.
            db.openHelper.writableDatabase.execSQL("DROP TABLE trace_events")
            db.openHelper.writableDatabase.execSQL("DROP TABLE trace_metadata")
            db.openHelper.writableDatabase.execSQL("DROP TABLE divergence_reports")
            db.openHelper.writableDatabase.version = 1
            db.close(); db = open(name)
            assertNotNull(db.batchDao().getBatchById(batch))
            assertTrue(db.traceDao().chain().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
