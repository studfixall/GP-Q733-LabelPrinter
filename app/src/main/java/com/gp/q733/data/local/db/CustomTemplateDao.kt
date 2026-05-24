package com.gp.q733.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTemplateDao {

    @Query("SELECT * FROM custom_templates ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CustomTemplateEntity>>

    @Query("SELECT * FROM custom_templates WHERE id = :id")
    suspend fun getById(id: Long): CustomTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CustomTemplateEntity): Long

    @Delete
    suspend fun delete(template: CustomTemplateEntity)

    @Query("DELETE FROM custom_templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
