package com.cannatrace.data.local.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TraceMigration {
    // Original v1 accidentally omitted divergence_reports. Preserve existing data.
    val from1 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS divergence_reports (id TEXT NOT NULL PRIMARY KEY, batchId TEXT NOT NULL, computedStock REAL NOT NULL, declaredStock REAL NOT NULL, divergence REAL NOT NULL, unit TEXT NOT NULL, reportedAt INTEGER NOT NULL, operatorId TEXT NOT NULL, resolved INTEGER NOT NULL, resolutionNotes TEXT NOT NULL)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_divergence_reports_batchId ON divergence_reports(batchId)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS trace_metadata (batchId TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, description TEXT NOT NULL, origin TEXT NOT NULL, supplier TEXT NOT NULL, unit TEXT NOT NULL, revision INTEGER NOT NULL, FOREIGN KEY(batchId) REFERENCES batches(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS trace_events (operationId TEXT NOT NULL PRIMARY KEY, batchId TEXT NOT NULL, action TEXT NOT NULL, timestamp INTEGER NOT NULL, actor TEXT NOT NULL, beforeValue TEXT NOT NULL, afterValue TEXT NOT NULL, reason TEXT NOT NULL, fingerprint TEXT NOT NULL, previousHash TEXT NOT NULL, hash TEXT NOT NULL, FOREIGN KEY(batchId) REFERENCES batches(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trace_events_batchId ON trace_events(batchId)")
            protect(db)
        }
    }
    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) { protect(db) }
        override fun onOpen(db: SupportSQLiteDatabase) { protect(db) }
    }
    private fun protect(db: SupportSQLiteDatabase) {
        for (table in listOf("trace_events", "audit_logs", "stock_entries")) {
            for (op in listOf("UPDATE", "DELETE")) {
                db.execSQL("CREATE TRIGGER IF NOT EXISTS ${table}_no_${op.lowercase()} BEFORE $op ON $table BEGIN SELECT RAISE(ABORT, 'Append-only history'); END")
            }
        }
        db.execSQL("CREATE TRIGGER IF NOT EXISTS batches_no_delete BEFORE DELETE ON batches BEGIN SELECT RAISE(ABORT, 'Archive batches instead'); END")
    }
}
