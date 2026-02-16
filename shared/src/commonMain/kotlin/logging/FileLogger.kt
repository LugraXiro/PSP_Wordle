package logging

expect object FileLogger {
    fun info(source: String, message: String)
    fun error(source: String, message: String)
    fun warning(source: String, message: String)
    fun debug(source: String, message: String)
    fun clear()
}
