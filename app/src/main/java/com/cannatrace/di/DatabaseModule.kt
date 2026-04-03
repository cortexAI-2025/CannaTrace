package com.cannatrace.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cannatrace.data.local.database.CannaTraceDatabase
import com.cannatrace.data.local.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val PREFS_NAME = "cannatrace_secure_prefs"
    private const val DB_PASSPHRASE_KEY = "db_passphrase"

    /**
     * Fournit la passphrase chiffrée pour SQLCipher, stockée dans EncryptedSharedPreferences.
     * En production, cette passphrase devrait être dérivée des credentials de l'utilisateur.
     */
    @Provides
    @Singleton
    fun provideDatabasePassphrase(@ApplicationContext context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(DB_PASSPHRASE_KEY, null)
        return if (existing != null) {
            android.util.Base64.decode(existing, android.util.Base64.DEFAULT)
        } else {
            // Génère une passphrase aléatoire à la première installation
            val newPassphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
            prefs.edit().putString(
                DB_PASSPHRASE_KEY,
                android.util.Base64.encodeToString(newPassphrase, android.util.Base64.DEFAULT)
            ).apply()
            newPassphrase
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray
    ): CannaTraceDatabase = CannaTraceDatabase.create(context, passphrase)

    @Provides
    fun provideBatchDao(db: CannaTraceDatabase): BatchDao = db.batchDao()

    @Provides
    fun providePlantDao(db: CannaTraceDatabase): PlantDao = db.plantDao()

    @Provides
    fun provideAuditLogDao(db: CannaTraceDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideUserDao(db: CannaTraceDatabase): UserDao = db.userDao()

    @Provides
    fun provideStockEntryDao(db: CannaTraceDatabase): StockEntryDao = db.stockEntryDao()

    @Provides
    fun providePrescriptionDao(db: CannaTraceDatabase): PrescriptionDao = db.prescriptionDao()

    @Provides
    fun provideCultureEntryDao(db: CannaTraceDatabase): CultureEntryDao = db.cultureEntryDao()
}
