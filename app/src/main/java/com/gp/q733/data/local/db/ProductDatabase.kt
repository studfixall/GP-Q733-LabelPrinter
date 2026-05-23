package com.gp.q733.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商品数据库
 * Room本地SQLite存储
 */
@Database(
    entities = [ProductEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ProductDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    @Singleton
    class Provider @Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        @Volatile
        private var INSTANCE: ProductDatabase? = null

        fun get(): ProductDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    ProductDatabase::class.java,
                    "product_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
