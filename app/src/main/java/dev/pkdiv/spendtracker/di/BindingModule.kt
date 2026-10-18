package dev.pkdiv.spendtracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.pkdiv.spendtracker.ingestion.sms.ContentResolverSmsBodyResolver
import dev.pkdiv.spendtracker.ingestion.sms.SmsBodyResolver
import dev.pkdiv.spendtracker.widget.WidgetRefresher
import dev.pkdiv.spendtracker.work.WidgetScheduler

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    abstract fun bindSmsBodyResolver(impl: ContentResolverSmsBodyResolver): SmsBodyResolver

    @Binds
    abstract fun bindWidgetRefresher(impl: WidgetScheduler): WidgetRefresher
}
