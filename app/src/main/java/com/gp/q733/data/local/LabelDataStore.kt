package com.gp.q733.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gp.q733.domain.model.Label
import com.gp.q733.domain.model.LabelElement
import com.gp.q733.domain.model.BarcodeFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.labelDataStore: DataStore<Preferences> by preferencesDataStore(name = "labels")

@Singleton
class LabelDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.labelDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val SAVED_LABELS = stringPreferencesKey("saved_labels")
    }
    val savedLabelsFlow: Flow<List<Label>> = dataStore.data.map { prefs ->
        val labelsJson = prefs[SAVED_LABELS] ?: "[]"
        try {
            val labelDtos = json.decodeFromString<List<LabelDto>>(labelsJson)
            labelDtos.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveLabel(label: Label) {
        dataStore.edit { prefs ->
            val currentJson = prefs[SAVED_LABELS] ?: "[]"
            val currentLabels = try {
                json.decodeFromString<List<LabelDto>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }

            // Update or add
            val updated = currentLabels.toMutableList()
            val existingIndex = updated.indexOfFirst { it.id == label.id }
            val dto = label.toDto()

            if (existingIndex >= 0) {
                updated[existingIndex] = dto
            } else {
                updated.add(dto)
            }

            prefs[SAVED_LABELS] = json.encodeToString(updated)
        }
    }

    suspend fun deleteLabel(labelId: String) {
        dataStore.edit { prefs ->
            val currentJson = prefs[SAVED_LABELS] ?: "[]"
            val currentLabels = try {
                json.decodeFromString<List<LabelDto>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updated = currentLabels.filter { it.id != labelId }
            prefs[SAVED_LABELS] = json.encodeToString(updated)
        }
    }

    suspend fun getLabel(labelId: String): Label? {
        return try {
            val prefs = dataStore.data.first()
            val jsonStr = prefs[SAVED_LABELS] ?: "[]"
            val labels = json.decodeFromString<List<LabelDto>>(jsonStr)
            labels.find { it.id == labelId }?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    // Extension functions for conversion
    private fun Label.toDto(): LabelDto = LabelDto(
        id = id,
        widthMm = widthMm,
        heightMm = heightMm,
        elements = elements.map { it.toDto() }
    )

    private fun LabelElement.toDto(): LabelElementDto = when (this) {
        is LabelElement.Text -> LabelElementDto(
            type = "text",
            x = x,
            y = y,
            text = text,
            fontSize = fontSize,
            isBold = isBold
        )
        is LabelElement.Barcode -> LabelElementDto(
            type = "barcode",
            x = x,
            y = y,
            text = content,
            format = format.name,
            height = height
        )
        is LabelElement.QRCode -> LabelElementDto(
            type = "qrcode",
            x = x,
            y = y,
            text = content,
            size = size
        )
        is LabelElement.Line -> LabelElementDto(
            type = "line",
            x = x,
            y = y,
            width = width,
            height = height
        )
    }

    private fun LabelDto.toDomain(): Label = Label(
        id = id,
        widthMm = widthMm,
        heightMm = heightMm,
        elements = elements.map { it.toDomain() }
    )

    private fun LabelElementDto.toDomain(): LabelElement = when (type) {
        "text" -> LabelElement.Text(
            x = x,
            y = y,
            text = text ?: "",
            fontSize = fontSize ?: 12f,
            isBold = isBold ?: false
        )
        "barcode" -> LabelElement.Barcode(
            x = x,
            y = y,
            content = text ?: "",
            format = format?.let { BarcodeFormat.valueOf(it) } ?: BarcodeFormat.CODE128,
            height = height ?: 20f
        )
        "qrcode" -> LabelElement.QRCode(
            x = x,
            y = y,
            content = text ?: "",
            size = size ?: 20f
        )
        "line" -> LabelElement.Line(
            x = x,
            y = y,
            width = width ?: 10f,
            height = height ?: 1f
        )
        else -> LabelElement.Text(x = 0f, y = 0f, text = "", fontSize = 12f, isBold = false)
    }
}

@Serializable
data class LabelDto(
    val id: String,
    val widthMm: Float,
    val heightMm: Float,
    val elements: List<LabelElementDto>
)

@Serializable
data class LabelElementDto(
    val type: String,
    val x: Float,
    val y: Float,
    val text: String? = null,
    val fontSize: Float? = null,
    val isBold: Boolean? = null,
    val format: String? = null,
    val height: Float? = null,
    val size: Float? = null,
    val width: Float? = null
)
