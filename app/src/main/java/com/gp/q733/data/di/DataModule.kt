package com.gp.q733.data.di

import com.gp.q733.data.BluetoothRepositoryImpl
import com.gp.q733.data.local.LabelDataStore
import com.gp.q733.data.local.SettingsDataStore
import com.gp.q733.data.repository.ProductRepositoryImpl
import com.gp.q733.domain.print.PrintService
import com.gp.q733.domain.repository.BluetoothRepository
import com.gp.q733.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBluetoothRepository(
        impl: BluetoothRepositoryImpl
    ): BluetoothRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        impl: ProductRepositoryImpl
    ): ProductRepository

    companion object {
        @Provides
        @Singleton
        fun providePrintService(
            bluetoothRepository: BluetoothRepository,
            settingsDataStore: SettingsDataStore
        ): PrintService {
            return PrintService(bluetoothRepository, settingsDataStore)
        }
    }
}
