package model.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logging.FileLogger
import protocol.GameMode
import protocol.RecordEntry
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Estructura de datos que almacena los récords de puntuación de partidas PVE y PVP,
 * organizados por longitud de palabra. Incluye campos legacy para migración desde
 * versiones anteriores del formato sin longitud de palabra.
 */
@Serializable
data class Records(
    // Nueva estructura: wordLength -> lista de records
    var pveRecordsByLength: MutableMap<Int, MutableList<RecordEntry>> = mutableMapOf(),
    var pvpRecordsByLength: MutableMap<Int, MutableList<RecordEntry>> = mutableMapOf(),
    // Campos legacy para migración (se eliminarán después de migrar)
    var pveRecords: MutableList<RecordEntry>? = null,
    var pvpRecords: MutableList<RecordEntry>? = null
)

/**
 * Persiste y gestiona los récords globales de puntuación en disco.
 *
 * Mantiene un top 10 de jugadores por modo de juego (PVE/PVP) y longitud de palabra.
 * Incluye migración automática desde el formato legacy de récords sin longitud de palabra.
 *
 * @param recordsFile Ruta al archivo JSON donde se guardan los récords.
 */
class RecordsManager(recordsFile: String = "records.json") {

    companion object {
        val SUPPORTED_LENGTHS = listOf(4, 5, 6, 7)
    }

    private val file: File = if (File("server").isDirectory) File("server", recordsFile) else File(recordsFile)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private var records = Records()
    private val lock = ReentrantReadWriteLock()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        if (file.exists()) {
            try {
                val content = file.readText()
                records = json.decodeFromString<Records>(content)

                // Migración: si existen records legacy, moverlos a wordLength=5
                migrateFromLegacy()

                val totalPve = records.pveRecordsByLength.values.sumOf { it.size }
                val totalPvp = records.pvpRecordsByLength.values.sumOf { it.size }
                FileLogger.info("SERVER", "✅ Records cargados desde '${file.path}': $totalPve récords PVE, $totalPvp récords PVP")
            } catch (e: Exception) {
                FileLogger.warning("SERVER", "⚠️  Error parseando archivo de records '${file.path}': ${e.javaClass.simpleName}: ${e.message}. Creando archivo nuevo")
                saveRecords()
            }
        } else {
            FileLogger.info("SERVER", "📝 Archivo de records '${file.path}' no existe. Creando nuevo vacío")
            initializeEmptyRecords()
            saveRecords()
        }
    }

    private fun initializeEmptyRecords() {
        SUPPORTED_LENGTHS.forEach { length ->
            records.pveRecordsByLength[length] = mutableListOf()
            records.pvpRecordsByLength[length] = mutableListOf()
        }
    }

    private fun migrateFromLegacy() {
        var migrated = false

        records.pveRecords?.let { legacyList ->
            if (legacyList.isNotEmpty()) {
                // Migrar a wordLength=5 con el nuevo campo
                val migratedRecords = legacyList.map { RecordEntry(it.playerName, it.score, 5) }.toMutableList()
                records.pveRecordsByLength[5] = migratedRecords
                records.pveRecords = null
                FileLogger.info("SERVER", "🔄 Migrados ${legacyList.size} records PVE legacy a wordLength=5")
                migrated = true
            }
        }

        records.pvpRecords?.let { legacyList ->
            if (legacyList.isNotEmpty()) {
                val migratedRecords = legacyList.map { RecordEntry(it.playerName, it.score, 5) }.toMutableList()
                records.pvpRecordsByLength[5] = migratedRecords
                records.pvpRecords = null
                FileLogger.info("SERVER", "🔄 Migrados ${legacyList.size} records PVP legacy a wordLength=5")
                migrated = true
            }
        }

        // Asegurar que existen listas para todas las longitudes
        SUPPORTED_LENGTHS.forEach { length ->
            if (!records.pveRecordsByLength.containsKey(length)) {
                records.pveRecordsByLength[length] = mutableListOf()
            }
            if (!records.pvpRecordsByLength.containsKey(length)) {
                records.pvpRecordsByLength[length] = mutableListOf()
            }
        }

        if (migrated) {
            saveRecords()
        }
    }

    private fun saveRecords() {
        try {
            val content = json.encodeToString(Records.serializer(), records)
            file.writeText(content)
            val totalPve = records.pveRecordsByLength.values.sumOf { it.size }
            val totalPvp = records.pvpRecordsByLength.values.sumOf { it.size }
            FileLogger.debug("SERVER", "💾 Records guardados en '${file.path}': $totalPve PVE, $totalPvp PVP")
        } catch (e: Exception) {
            FileLogger.error("SERVER", "❌ Error crítico al guardar records en '$recordsFile': ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getRecords(): Records = lock.read {
        Records(
            pveRecordsByLength = records.pveRecordsByLength.mapValues { it.value.toMutableList() }.toMutableMap(),
            pvpRecordsByLength = records.pvpRecordsByLength.mapValues { it.value.toMutableList() }.toMutableMap()
        )
    }

    fun getRecordsByLength(mode: GameMode, wordLength: Int): List<RecordEntry> = lock.read {
        val recordsMap = when (mode) {
            GameMode.PVE -> records.pveRecordsByLength
            GameMode.PVP -> records.pvpRecordsByLength
        }
        recordsMap[wordLength]?.toList() ?: emptyList()
    }

    fun updateScore(mode: GameMode, playerName: String, score: Int, wordLength: Int): Boolean = lock.write {
        val recordsMap = when (mode) {
            GameMode.PVE -> records.pveRecordsByLength
            GameMode.PVP -> records.pvpRecordsByLength
        }

        // Obtener o crear la lista para esta longitud
        val recordsList = recordsMap.getOrPut(wordLength) { mutableListOf() }

        // Añadir nueva entrada con wordLength
        recordsList.add(RecordEntry(playerName, score, wordLength))

        // Ordenar por puntuación descendente y mantener solo top 10
        recordsList.sortByDescending { it.score }
        if (recordsList.size > 10) {
            recordsList.subList(10, recordsList.size).clear()
        }

        // Verificar si es un nuevo récord (top 10)
        val isNewRecord = recordsList.any { it.playerName == playerName && it.score == score }
        val position = recordsList.indexOfFirst { it.playerName == playerName && it.score == score } + 1

        if (isNewRecord) {
            FileLogger.info("SERVER", "🏆 ¡${mode} RÉCORD ($wordLength letras)! $playerName: $score puntos (Posición #$position)")
            saveRecords()
        } else {
            FileLogger.debug("SERVER", "📊 Puntuación ${mode} ($wordLength letras): $playerName = $score puntos (no entró en top 10)")
        }

        isNewRecord
    }
}
