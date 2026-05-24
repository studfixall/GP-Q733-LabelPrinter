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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CustomTemplateEntity): Long

    @Delete
    suspend fun delete(template: CustomTemplateEntity)

    @Query("DELETE FROM custom_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_templates WHERE isBuiltIn = 1")
    suspend fun deleteAllBuiltIn()
}
