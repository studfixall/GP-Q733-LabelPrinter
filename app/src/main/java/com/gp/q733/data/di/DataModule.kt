package com.gp.q733.data.di

import com.gp.q733.data.BluetoothRepositoryImpl
import com.gp.q733.data.local.db.ProductDao
import com.gp.q733.data.local.db.ProductDatabase
import com.gp.q733.data.remote.RmisApiClient
import com.gp.q733.data.remote.RmisProductRepository
import com.gp.q733.data.repository.ProductRepositoryImpl
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
        impl: RmisProductRepository
    ): ProductRepository

    companion object {
        @Provides
        @Singleton
        fun provideProductDao(database: ProductDatabase.Provider): ProductDao {
            return database.get().productDao()
        }

        @Provides
        @Singleton
        fun provideCustomTemplateDao(database: ProductDatabase.Provider): com.gp.q733.data.local.db.CustomTemplateDao {
            return database.get().customTemplateDao()
        }

        @Provides
        @Singleton
        fun provideRmisApiClient(): RmisApiClient {
            // 初始空配置，后续由 SettingsViewModel 更新
            return RmisApiClient(baseUrl = "", userNo = "", masterKey = "")
        }
    }
}
