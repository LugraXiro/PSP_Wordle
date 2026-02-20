package logging

/**
 * Logger multiplataforma con soporte para cuatro niveles de severidad.
 *
 * Declarado como `expect` para que cada plataforma aporte su propia implementación `actual`.
 * En JVM (servidor y cliente desktop) escribe las entradas en `wordle-logs.txt` en la raíz
 * del proyecto, con rotación automática cuando se superan [MAX_LOGS] líneas.
 *
 * Uso típico:
 * ```kotlin
 * FileLogger.info("SERVER", "Cliente conectado")
 * FileLogger.error("CLIENT", "Fallo de red: ${e.message}")
 * ```
 *
 * @see logging.FileLogger (implementación JVM en jvmMain)
 */
expect object FileLogger {
    /** Registra un mensaje informativo. */
    fun info(source: String, message: String)

    /** Registra un mensaje de error. */
    fun error(source: String, message: String)

    /** Registra una advertencia no crítica. */
    fun warning(source: String, message: String)

    /** Registra un mensaje de depuración (verbose). */
    fun debug(source: String, message: String)

    /** Borra el contenido del fichero de log. */
    fun clear()
}
