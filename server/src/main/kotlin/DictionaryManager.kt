import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logging.FileLogger
import protocol.WordData
import java.io.File
import java.text.Normalizer
import kotlin.random.Random

@Serializable
private data class DictionaryFile(
    val palabras: List<WordData>
)

// Formato nuevo: array de { "word": "...", "hint": "..." }
@Serializable
private data class SolutionEntry(
    val word: String,
    val hint: String
)

class DictionaryManager(private val dictionaryDir: String = "dictionaries") {

    private val json = Json { ignoreUnknownKeys = true }

    // Map: wordLength -> lista de palabras solución (con pistas)
    private var solutionsByLength: Map<Int, List<WordData>> = emptyMap()

    // Map: wordLength -> set de palabras válidas (para validación O(1))
    private var validWordsByLength: Map<Int, Set<String>> = emptyMap()

    companion object {
        val SUPPORTED_LENGTHS = listOf(4, 5, 6, 7)

        /**
         * Normaliza una palabra: convierte a mayúsculas y elimina tildes,
         * pero preserva la Ñ (letra distinta en español).
         */
        fun normalizeWord(word: String): String {
            val upper = word.uppercase()
            val normalized = Normalizer.normalize(upper, Normalizer.Form.NFD)
            return normalized
                .replace("Ñ".toRegex(), "{{N_TILDE}}")
                .replace("\\p{M}".toRegex(), "")
                .replace("{{N_TILDE}}", "Ñ")
        }
    }

    init {
        loadAllDictionaries()
    }

    private fun loadAllDictionaries() {
        val solutions = mutableMapOf<Int, List<WordData>>()
        val validWords = mutableMapOf<Int, Set<String>>()

        val dir = File(dictionaryDir)

        // Intentar cargar diccionarios por longitud
        if (dir.exists() && dir.isDirectory) {
            for (length in SUPPORTED_LENGTHS) {
                // Formato nuevo: 20_solutions_N.json (array de {word, hint})
                val newFormatFile = File(dir, "20_solutions_$length.json")
                if (newFormatFile.exists()) {
                    try {
                        val content = newFormatFile.readText()
                        val entries = json.decodeFromString<List<SolutionEntry>>(content)
                        // Convertir a WordData (normalizar palabra a mayúsculas)
                        val wordDataList = entries.map { entry ->
                            WordData(
                                palabra = normalizeWord(entry.word),
                                pista = entry.hint
                            )
                        }
                        solutions[length] = wordDataList
                        validWords[length] = wordDataList.map { it.palabra }.toSet()
                        FileLogger.info("SERVER", "✅ Diccionario $length letras: ${wordDataList.size} palabras")
                    } catch (e: Exception) {
                        FileLogger.error("SERVER", "❌ Error cargando 20_solutions_$length.json: ${e.message}")
                    }
                } else {
                    // Fallback: formato antiguo soluciones_N.json
                    val solutionsFile = File(dir, "soluciones_$length.json")
                    if (solutionsFile.exists()) {
                        try {
                            val content = solutionsFile.readText()
                            val dictionary = json.decodeFromString<DictionaryFile>(content)
                            solutions[length] = dictionary.palabras
                            validWords[length] = dictionary.palabras.map { normalizeWord(it.palabra) }.toSet()
                            FileLogger.info("SERVER", "✅ Diccionario $length letras: ${dictionary.palabras.size} palabras")
                        } catch (e: Exception) {
                            FileLogger.error("SERVER", "❌ Error cargando soluciones_$length.json: ${e.message}")
                        }
                    }
                }

                // Cargar palabras válidas adicionales si existen
                val validFile = File(dir, "palabras_validas_$length.json")
                if (validFile.exists()) {
                    try {
                        val content = validFile.readText()
                        val dictionary = json.decodeFromString<DictionaryFile>(content)
                        val existingValid = validWords[length] ?: emptySet()
                        val additionalValid = dictionary.palabras.map { normalizeWord(it.palabra) }.toSet()
                        validWords[length] = existingValid + additionalValid
                        FileLogger.info("SERVER", "✅ Palabras válidas adicionales $length letras: ${additionalValid.size}")
                    } catch (e: Exception) {
                        FileLogger.warning("SERVER", "⚠️  Error cargando palabras_validas_$length.json: ${e.message}")
                    }
                }
            }
        }

        // Fallback: si no hay diccionarios por longitud, intentar cargar el archivo legacy
        if (solutions.isEmpty()) {
            loadLegacyDictionary(solutions, validWords)
        }

        solutionsByLength = solutions
        validWordsByLength = validWords

        // Resumen
        val summary = SUPPORTED_LENGTHS
            .filter { solutions.containsKey(it) }
            .joinToString(", ") { "$it letras: ${solutions[it]?.size ?: 0}" }

        if (summary.isNotEmpty()) {
            FileLogger.info("SERVER", "📚 Diccionarios cargados: $summary")
        } else {
            FileLogger.error("SERVER", "❌ No se encontraron diccionarios. Crea archivos en '$dictionaryDir/' o 'palabras.json'")
        }
    }

    private fun loadLegacyDictionary(
        solutions: MutableMap<Int, List<WordData>>,
        validWords: MutableMap<Int, Set<String>>
    ) {
        val legacyFile = File("palabras.json")
        if (legacyFile.exists()) {
            try {
                val content = legacyFile.readText()
                val dictionary = json.decodeFromString<DictionaryFile>(content)

                // Agrupar por longitud de palabra
                val byLength = dictionary.palabras.groupBy { it.palabra.length }

                for (length in SUPPORTED_LENGTHS) {
                    byLength[length]?.let { words ->
                        solutions[length] = words
                        validWords[length] = words.map { normalizeWord(it.palabra) }.toSet()
                    }
                }

                FileLogger.info("SERVER", "📖 Diccionario legacy cargado: ${dictionary.palabras.size} palabras totales")
            } catch (e: Exception) {
                FileLogger.error("SERVER", "❌ Error cargando palabras.json legacy: ${e.message}")
            }
        }
    }

    fun getRandomWord(length: Int): WordData? {
        val words = solutionsByLength[length]
        if (words.isNullOrEmpty()) {
            FileLogger.warning("SERVER", "⚠️  No hay palabras de $length letras disponibles")
            return null
        }
        val selected = words[Random.nextInt(words.size)]
        FileLogger.debug("SERVER", "🎲 Palabra seleccionada: '${selected.palabra}' (de ${words.size} palabras de $length letras)")
        return selected
    }

    fun isValidWord(word: String, length: Int): Boolean {
        val normalized = normalizeWord(word)
        return validWordsByLength[length]?.contains(normalized) ?: false
    }

    fun getWordCount(length: Int): Int {
        return solutionsByLength[length]?.size ?: 0
    }

    fun getSupportedLengths(): List<Int> {
        return SUPPORTED_LENGTHS.filter { solutionsByLength[it]?.isNotEmpty() == true }
    }

    fun hasDictionaryFor(length: Int): Boolean {
        return solutionsByLength[length]?.isNotEmpty() == true
    }
}
