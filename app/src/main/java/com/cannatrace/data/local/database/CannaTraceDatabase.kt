package com.cannatrace.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.cannatrace.data.local.database.dao.*
import com.cannatrace.data.local.database.entities.*
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        BatchEntity::class,
        TraceMetadata::class,
        TraceEvent::class,
        CorrectionNoteEntity::class,
        PlantEntity::class,
        AuditLogEntity::class,
        UserEntity::class,
        StockEntryEntity::class,
        DivergenceReportEntity::class,
        PrescriptionEntity::class,
        CultureEntryEntity::class,
        LocationEntity::class,
        HarvestEntity::class,
        TransformationEntity::class,
        PackagingEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class CannaTraceDatabase : RoomDatabase() {

    abstract fun traceDao(): TraceDao

    abstract fun batchDao(): BatchDao
    abstract fun plantDao(): PlantDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userDao(): UserDao
    abstract fun stockEntryDao(): StockEntryDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun cultureEntryDao(): CultureEntryDao

    companion object {
        const val DATABASE_NAME = "cannatrace.db"

        fun create(context: Context, passphrase: ByteArray, databaseName: String = if (com.cannatrace.BuildConfig.DEBUG) "cannatrace-beta.db" else DATABASE_NAME): CannaTraceDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                CannaTraceDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .addMigrations(TraceMigration.from1)
                .addCallback(TraceMigration.callback)
                .build()
        }
    }
}
