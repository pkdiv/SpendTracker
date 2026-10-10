package dev.pkdiv.spendtracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pkdiv.spendtracker.ingestion.sms.ContentResolverSmsBodyResolver
import dev.pkdiv.spendtracker.ingestion.sms.SmsBodyResolver

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    abstract fun bindSmsBodyResolver(impl: ContentResolverSmsBodyResolver): SmsBodyResolver
}
