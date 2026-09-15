package com.nocta.app.di

import android.content.Context
import androidx.room.Room
import com.nocta.app.data.local.MIGRATION_1_2
import com.nocta.app.data.local.MIGRATION_2_3
import com.nocta.app.data.local.MIGRATION_3_4
import com.nocta.app.data.local.NoctaDatabase
import com.nocta.app.data.remote.NoctaApi
import com.nocta.app.data.repository.AiCoachRepositoryImpl
import com.nocta.app.data.repository.MorningCheckInRepositoryImpl
import com.nocta.app.data.repository.SleepRepositoryImpl
import com.nocta.app.data.repository.SleepTrackingRepositoryImpl
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.SleepTrackingRepository
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

private const val BASE_URL_PLACEHOLDER = "https://api.nocta.example.com/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(60, TimeUnit.SECONDS)
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
    fun provideNoctaApi(retrofit: Retrofit): NoctaApi =
        retrofit.create(NoctaApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NoctaDatabase =
        Room.databaseBuilder(
            context,
            NoctaDatabase::class.java,
            "nocta.db"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4
            )
            .build()

    @Provides
    fun provideSleepDao(db: NoctaDatabase) =
        db.sleepDao()

    @Provides
    fun provideAiMessageDao(db: NoctaDatabase) =
        db.aiMessageDao()

    @Provides
    fun provideSleepTrackingDao(db: NoctaDatabase) =
        db.sleepTrackingDao()

    @Provides
    fun provideMorningCheckInDao(db: NoctaDatabase) =
        db.morningCheckInDao()

    @Provides
    fun provideUserProfileDao(db: NoctaDatabase) =
        db.userProfileDao()
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    fun provideCalculateSleepScore(): CalculateSleepScore =
        CalculateSleepScore()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAiCoachRepository(
        impl: AiCoachRepositoryImpl
    ): AiCoachRepository

    @Binds
    abstract fun bindSleepRepository(
        impl: SleepRepositoryImpl
    ): SleepRepository

    @Binds
    abstract fun bindSleepTrackingRepository(
        impl: SleepTrackingRepositoryImpl
    ): SleepTrackingRepository

    @Binds
    abstract fun bindMorningCheckInRepository(
        impl: MorningCheckInRepositoryImpl
    ): MorningCheckInRepository
}
