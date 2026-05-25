package com.gp.q733.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTemplateDao {

    @Query("SELECT * FROM custom_templates ORDER BY isBuiltIn DESC, sortOrder ASC, createdAt ASC")
    fun getAllSorted(): Flow<List<CustomTemplateEntity>>

    @Query("SELECT * FROM custom_templates WHERE templateId = :templateId")
    suspend fun getByTemplateId(templateId: String): CustomTemplateEntity?

    @Query("SELECT * FROM custom_templates WHERE id = :id")
    suspend fun getById(id: Long): CustomTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<CustomTemplateEntity>)

    /**
     * Upsert: INSERT OR REPLACE on templateId conflict.
     * 比 insert() 更安全：确保同 templateId 只保留一条记录
     */
    @Query("""
        INSERT INTO custom_templates
          (templateId, name, widthMm, heightMm, elementsJson, isBuiltIn, sortOrder, createdAt)
        VALUES
          (:templateId, :name, :widthMm, :heightMm, :elementsJson, :isBuiltIn, :sortOrder, :createdAt)
        ON CONFLICT(templateId) DO UPDATE SET
          name = :name,
          widthMm = :widthMm,
          heightMm = :heightMm,
          elementsJson = :elementsJson,
          isBuiltIn = :isBuiltIn,
          sortOrder = :sortOrder
    """)
    suspend fun upsert(
        templateId: String,
        name: String,
        widthMm: Float,
        heightMm: Float,
        elementsJson: String,
        isBuiltIn: Boolean,
        sortOrder: Int,
        createdAt: Long
    ): Long

    @Delete
    suspend fun delete(template: CustomTemplateEntity)

    @Query("DELETE FROM custom_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_templates WHERE isBuiltIn = 1")
    suspend fun deleteAllBuiltIn()
}
