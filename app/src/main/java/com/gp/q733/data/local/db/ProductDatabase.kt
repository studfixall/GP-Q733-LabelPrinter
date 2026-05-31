package com.gp.q733.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gp.q733.domain.model.BarcodeFormat
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.util.TemplateJsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [ProductEntity::class, CustomTemplateEntity::class],
    version = 7,
    exportSchema = false
)
abstract class ProductDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customTemplateDao(): CustomTemplateDao
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
                // 首次创建时插入内置模板
                runBlocking {
                    seedBuiltInTemplates(instance.customTemplateDao())
                }
                instance
            }
        }
    }
}

/**
 * 插入内置模板（id 固定，已存在则跳过）
 */
private suspend fun seedBuiltInTemplates(dao: CustomTemplateDao) {
    // Skip if already seeded (prevents duplicates on DB recreate)
    dao.getByTemplateId("built_in_express")?.let { return }
    val builtIns = listOf(
        CustomTemplateEntity(
            templateId = "built_in_express",
            name = "快递面单",
            widthMm = 50f,
            heightMm = 30f,
            elementsJson = TemplateJsonParser.toJson(listOf(
                LabelElement.Text(x = 2f, y = 2f, text = "收件人：", fontSize = 8f, isBold = true, textName = "name", variable = 1),
                LabelElement.Text(x = 2f, y = 10f, text = "地址：", fontSize = 7f, isBold = false, textName = "area", variable = 1),
                LabelElement.Barcode(x = 2f, y = 18f, content = "", format = BarcodeFormat.CODE128, height = 8f, textName = "barcode", variable = 1)
            )),
            isBuiltIn = true,
            isQuickPrint = true,
            sortOrder = 0
        ),
        CustomTemplateEntity(
            templateId = "built_in_product",
            name = "商品标签",
            widthMm = 40f,
            heightMm = 30f,
            elementsJson = TemplateJsonParser.toJson(listOf(
                LabelElement.Text(x = 2f, y = 2f, text = "", fontSize = 9f, isBold = true, textName = "name", variable = 1),
                LabelElement.Text(x = 2f, y = 10f, text = "规格：", fontSize = 7f, isBold = false, textName = "spec", variable = 1),
                LabelElement.QRCode(x = 25f, y = 2f, content = "", size = 12f),
                LabelElement.Text(x = 2f, y = 20f, text = "￥", fontSize = 10f, isBold = true, textName = "price", variable = 1)
            )),
            isBuiltIn = true,
            isQuickPrint = true,
            sortOrder = 1
        ),
        CustomTemplateEntity(
            templateId = "built_in_price",
            name = "价格标签",
            widthMm = 30f,
            heightMm = 20f,
            elementsJson = TemplateJsonParser.toJson(listOf(
                LabelElement.Text(x = 2f, y = 2f, text = "特价", fontSize = 7f, isBold = false),
                LabelElement.Text(x = 2f, y = 8f, text = "", fontSize = 12f, isBold = true, textName = "price", variable = 1),
                LabelElement.Barcode(x = 2f, y = 16f, content = "", format = BarcodeFormat.EAN13, height = 3f, textName = "barcode", variable = 1)
            )),
            isBuiltIn = true,
            isQuickPrint = true,
            sortOrder = 2
        )
    )
    dao.insertAll(builtIns)
}
