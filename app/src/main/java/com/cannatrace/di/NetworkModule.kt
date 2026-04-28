package com.cannatrace.di

import com.cannatrace.BuildConfig
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.data.remote.MockInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * URL de base du serveur CannaTrace.
     * Configurer via build variant ou fichier de configuration.
     * En mode debug, le MockInterceptor intercepte toutes les requêtes.
     */
    private const val BASE_URL = "https://api.cannatrace.fr/v1/"
    private val USE_MOCK = BuildConfig.DEBUG

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (USE_MOCK) {
            // Mode démonstration : intercepteur de mock
            builder.addInterceptor(MockInterceptor())
        }

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        // En-tête d'authentification injecté automatiquement
        builder.addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-App-Version", BuildConfig.VERSION_NAME)
                .header("X-Platform", "Android")
            chain.proceed(requestBuilder.build())
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideCannaTraceApi(retrofit: Retrofit): CannaTraceApi =
        retrofit.create(CannaTraceApi::class.java)
}
