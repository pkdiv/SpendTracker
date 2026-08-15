package dev.pkdiv.spendtracker.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.pkdiv.spendtracker.data.SettingsStore
import dev.pkdiv.spendtracker.data.db.MerchantCategoryDao
import dev.pkdiv.spendtracker.data.db.SpendTrackerDatabase
import dev.pkdiv.spendtracker.data.db.TransactionDao
import dev.pkdiv.spendtracker.data.db.UnrecognizedMessageDao
import dev.pkdiv.spendtracker.parsing.ParserEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpendTrackerDatabase =
        Room.databaseBuilder(context, SpendTrackerDatabase::class.java, "spendtracker.db")
            .addMigrations(
                SpendTrackerDatabase.MIGRATION_1_2,
                SpendTrackerDatabase.MIGRATION_2_3,
                SpendTrackerDatabase.MIGRATION_3_4,
                SpendTrackerDatabase.MIGRATION_4_5,
            )
            .build()

    @Provides
    fun provideTransactionDao(database: SpendTrackerDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideUnrecognizedMessageDao(database: SpendTrackerDatabase): UnrecognizedMessageDao =
        database.unrecognizedMessageDao()

    @Provides
    fun provideMerchantCategoryDao(database: SpendTrackerDatabase): MerchantCategoryDao =
        database.merchantCategoryDao()

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStore(context)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideParserEngine(): ParserEngine = ParserEngine()
}
