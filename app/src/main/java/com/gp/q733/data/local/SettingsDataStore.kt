package com.gp.q733.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gp.q733.domain.print.PrintProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    companion object {
        val LABEL_WIDTH = floatPreferencesKey("label_width")
        val LABEL_HEIGHT = floatPreferencesKey("label_height")
        val PRINT_COPIES = intPreferencesKey("print_copies")
        val PRINT_PROTOCOL = stringPreferencesKey("print_protocol")
        val PRINT_DENSITY = intPreferencesKey("print_density")
        val PRINT_SPEED = intPreferencesKey("print_speed")
        val GAP_MM = floatPreferencesKey("gap_mm")
        val AUTO_RECONNECT = stringPreferencesKey("auto_reconnect")
        val RECONNECT_INTERVAL = intPreferencesKey("reconnect_interval")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            labelWidth = prefs[LABEL_WIDTH] ?: 50f,
            labelHeight = prefs[LABEL_HEIGHT] ?: 30f,
            printCopies = prefs[PRINT_COPIES] ?: 1,
            printProtocol = prefs[PRINT_PROTOCOL]?.let { protocolName ->
                when (protocolName) {
                    "TSPL" -> PrintProtocol.TSPL
                    "CPCL" -> PrintProtocol.CPCL
                    "ESCPOS" -> PrintProtocol.ESCPOS
                    else -> PrintProtocol.CPCL
                }
            } ?: PrintProtocol.CPCL,
            printDensity = prefs[PRINT_DENSITY] ?: 8,
            printSpeed = prefs[PRINT_SPEED] ?: 4,
            gapMm = prefs[GAP_MM] ?: 2f,
            autoReconnect = prefs[AUTO_RECONNECT]?.toBoolean() ?: true,
            reconnectInterval = prefs[RECONNECT_INTERVAL] ?: 5
        )
    }

    suspend fun saveLabelWidth(width: Float) {
        dataStore.edit { it[LABEL_WIDTH] = width }
    }

    suspend fun saveLabelHeight(height: Float) {
        dataStore.edit { it[LABEL_HEIGHT] = height }
    }

    suspend fun savePrintCopies(copies: Int) {
        dataStore.edit { it[PRINT_COPIES] = copies }
    }

    suspend fun savePrintProtocol(protocol: PrintProtocol) {
        val protocolName = when (protocol) {
            PrintProtocol.TSPL -> "TSPL"
            PrintProtocol.CPCL -> "CPCL"
            PrintProtocol.ESCPOS -> "ESCPOS"
        }
        dataStore.edit { it[PRINT_PROTOCOL] = protocolName }
    }

    suspend fun savePrintDensity(density: Int) {
        dataStore.edit { it[PRINT_DENSITY] = density }
    }

    suspend fun savePrintSpeed(speed: Int) {
        dataStore.edit { it[PRINT_SPEED] = speed }
    }

    suspend fun saveGapMm(gapMm: Float) {
        dataStore.edit { it[GAP_MM] = gapMm }
    }

    suspend fun saveAutoReconnect(enabled: Boolean) {
        dataStore.edit { it[AUTO_RECONNECT] = enabled.toString() }
    }

    suspend fun saveReconnectInterval(seconds: Int) {
        dataStore.edit { it[RECONNECT_INTERVAL] = seconds }
    }

    suspend fun updateLabelSize(width: Float, height: Float) {
        saveLabelWidth(width)
        saveLabelHeight(height)
    }

    suspend fun saveAllSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[LABEL_WIDTH] = settings.labelWidth
            prefs[LABEL_HEIGHT] = settings.labelHeight
            prefs[PRINT_COPIES] = settings.printCopies
            prefs[PRINT_PROTOCOL] = when (settings.printProtocol) {
                PrintProtocol.TSPL -> "TSPL"
                PrintProtocol.CPCL -> "CPCL"
                PrintProtocol.ESCPOS -> "ESCPOS"
            }
            prefs[PRINT_DENSITY] = settings.printDensity
            prefs[PRINT_SPEED] = settings.printSpeed
            prefs[GAP_MM] = settings.gapMm
            prefs[AUTO_RECONNECT] = settings.autoReconnect.toString()
            prefs[RECONNECT_INTERVAL] = settings.reconnectInterval
        }
    }
}

data class AppSettings(
    val labelWidth: Float = 50f,
    val labelHeight: Float = 30f,
    val printCopies: Int = 1,
    val printProtocol: PrintProtocol = PrintProtocol.CPCL,
    val printDensity: Int = 8,
    val printSpeed: Int = 4,
    val gapMm: Float = 2f,
    val autoReconnect: Boolean = true,
    val reconnectInterval: Int = 5
)
