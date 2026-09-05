package com.nocta.app.di

import android.content.Context
import androidx.room.Room
import com.nocta.app.data.local.NoctaDatabase
import com.nocta.app.data.remote.NoctaApi
import com.nocta.app.data.repository.AiCoachRepositoryImpl
import com.nocta.app.data.repository.SleepRepositoryImpl
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.CalculateSleepScore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Base URL is environment-specific — set via BuildConfig, not hardcoded, so
// debug/staging/prod can point at different backends. See build.gradle.kts.
private const val BASE_URL_PLACEHOLDER = "https://api.nocta.example.com/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // NEVER set BODY in release builds — avoid logging user sleep data
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(60, TimeUnit.SECONDS) // generous timeout for streamed AI responses
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL_PLACEHOLDER)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideNoctaApi(retrofit: Retrofit): NoctaApi = retrofit.create(NoctaApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoctaDatabase =
        Room.databaseBuilder(context, NoctaDatabase::class.java, "nocta.db").build()

    @Provides
    fun provideSleepDao(db: NoctaDatabase) = db.sleepDao()

    @Provides
    fun provideAiMessageDao(db: NoctaDatabase) = db.aiMessageDao()
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    fun provideCalculateSleepScore(): CalculateSleepScore = CalculateSleepScore()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAiCoachRepository(impl: AiCoachRepositoryImpl): AiCoachRepository

    @Binds
    abstract fun bindSleepRepository(impl: SleepRepositoryImpl): SleepRepository
}
