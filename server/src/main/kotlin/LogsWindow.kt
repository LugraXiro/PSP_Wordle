import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import javax.swing.text.*
import java.awt.FlowLayout

class LogsWindow {
    private val frame: JFrame
    private val textPane: JTextPane
    private val styledDoc: StyledDocument
    private var logFile: File? = null
    private var lastLogs: List<String> = emptyList()
    private var pollTimer: Timer? = null
    private var shutdownCallback: (() -> Unit)? = null
    private var shutdownButton: JButton? = null

    // Colores para diferentes clientes (basado en hash del ID)
    private val clientColors = listOf(
        Color(0x00, 0xD9, 0xFF), // Cian
        Color(0xFF, 0x6B, 0x9D), // Rosa
        Color(0xFF, 0xC1, 0x45), // Naranja
        Color(0xB5, 0x65, 0xFF), // Púrpura
        Color(0x00, 0xFF, 0xA3), // Verde agua
        Color(0xFF, 0x6B, 0x6B), // Rojo claro
        Color(0x4E, 0xCD, 0xC4), // Turquesa
        Color(0xF7, 0xB7, 0x31), // Amarillo
        Color(0x57, 0xC7, 0xFF), // Azul cielo
        Color(0xFF, 0x85, 0xA2), // Rosa salmón
        Color(0x5F, 0xD0, 0x68), // Verde lima
        Color(0xFF, 0xD9, 0x3D)  // Amarillo oro
    )

    private val serverColor = Color(0x00, 0xFF, 0x41)        // Verde Matrix
    private val errorColor = Color(0xFF, 0x6B, 0x6B)         // Rojo
    private val warnColor = Color(0xFF, 0xA5, 0x00)          // Naranja
    private val infoColor = Color(0x95, 0xE1, 0xD3)          // Cyan claro
    private val debugColor = Color(0x88, 0x88, 0x88)         // Gris
    private val defaultColor = Color(0xCC, 0xCC, 0xCC)       // Gris claro
    private val defaultClientColor = Color(0x4E, 0xCD, 0xC4) // Turquesa

    private val bgDark = Color(0x1E, 0x1E, 0x1E)
    private val headerBg = Color(0x2D, 0x2D, 0x2D)

    private val clientIdRegex = """\[CLIENT-([a-f0-9]{4})\]""".toRegex()

    init {
        frame = JFrame("Logs del Sistema")
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.setSize(900, 600)
        frame.setLocation(870, 50)

        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = bgDark

        // Header
        val headerPanel = JPanel(BorderLayout())
        headerPanel.background = headerBg
        headerPanel.border = BorderFactory.createEmptyBorder(10, 16, 10, 16)

        val titleLabel = JLabel("\uD83D\uDCCB Logs del Sistema")
        titleLabel.foreground = Color.WHITE
        titleLabel.font = Font("SansSerif", Font.BOLD, 16)
        headerPanel.add(titleLabel, BorderLayout.WEST)

        val clearButton = JButton("\uD83D\uDDD1\uFE0F Limpiar")
        clearButton.background = Color(0x44, 0x44, 0x44)
        clearButton.foreground = Color.WHITE
        clearButton.isFocusPainted = false
        clearButton.border = BorderFactory.createEmptyBorder(6, 12, 6, 12)
        clearButton.addActionListener {
            logFile?.writeText("")
            lastLogs = emptyList()
            styledDoc.remove(0, styledDoc.length)
        }

        val shutdownBtn = JButton("\uD83D\uDED1 Apagar servidor")
        shutdownBtn.background = Color(0x8B, 0x00, 0x00)
        shutdownBtn.foreground = Color.WHITE
        shutdownBtn.isFocusPainted = false
        shutdownBtn.border = BorderFactory.createEmptyBorder(6, 12, 6, 12)
        shutdownBtn.addActionListener {
            shutdownBtn.isEnabled = false
            shutdownBtn.text = "Apagando..."
            shutdownCallback?.invoke()
        }
        shutdownButton = shutdownBtn

        val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        buttonsPanel.isOpaque = false
        buttonsPanel.add(clearButton)
        buttonsPanel.add(shutdownBtn)
        headerPanel.add(buttonsPanel, BorderLayout.EAST)

        contentPanel.add(headerPanel, BorderLayout.NORTH)

        // Text pane
        textPane = JTextPane()
        textPane.isEditable = false
        textPane.background = bgDark
        textPane.font = Font("Monospaced", Font.PLAIN, 12)
        styledDoc = textPane.styledDocument

        val scrollPane = JScrollPane(textPane)
        scrollPane.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        scrollPane.viewport.background = bgDark
        contentPanel.add(scrollPane, BorderLayout.CENTER)

        frame.contentPane = contentPanel

        // Buscar archivo de logs
        logFile = findLogFile()
    }

    private fun findLogFile(): File {
        var currentDir: File? = File(System.getProperty("user.dir"))
        while (currentDir != null && !File(currentDir, "settings.gradle.kts").exists()) {
            currentDir = currentDir.parentFile
        }
        val projectRoot = currentDir ?: File(System.getProperty("user.dir"))
        return File(projectRoot, "wordle-logs.txt").also {
            if (!it.exists()) it.createNewFile()
        }
    }

    private fun getColorForLog(log: String): Color {
        val clientMatch = clientIdRegex.find(log)
        val clientId = clientMatch?.groupValues?.get(1)

        return when {
            log.contains("[SERVER]") -> serverColor
            clientId != null -> {
                val colorIndex = clientId.hashCode().let { Math.floorMod(it, clientColors.size) }
                clientColors[colorIndex]
            }
            log.contains("[CLIENT]") -> defaultClientColor
            log.contains("[ERROR]") -> errorColor
            log.contains("[WARN]") -> warnColor
            log.contains("[INFO]") -> infoColor
            log.contains("[DEBUG]") -> debugColor
            else -> defaultColor
        }
    }

    private fun refreshLogs() {
        val file = logFile ?: return
        try {
            if (!file.exists()) return
            val newLogs = file.readLines()
            if (newLogs == lastLogs) return

            lastLogs = newLogs

            SwingUtilities.invokeLater {
                styledDoc.remove(0, styledDoc.length)
                for (line in newLogs) {
                    val color = getColorForLog(line)
                    val style = SimpleAttributeSet()
                    StyleConstants.setForeground(style, color)
                    StyleConstants.setFontFamily(style, "Monospaced")
                    StyleConstants.setFontSize(style, 12)
                    styledDoc.insertString(styledDoc.length, line + "\n", style)
                }
                // Auto-scroll al final
                textPane.caretPosition = styledDoc.length
            }
        } catch (_: Exception) {
            // Ignorar errores de lectura
        }
    }

    fun show() {
        SwingUtilities.invokeLater {
            frame.isVisible = true
        }
        // Iniciar polling cada 500ms
        pollTimer = Timer(500) { refreshLogs() }
        pollTimer?.start()
    }

    fun close() {
        pollTimer?.stop()
        pollTimer = null
        SwingUtilities.invokeLater {
            frame.isVisible = false
            frame.dispose()
        }
    }

    fun setShutdownCallback(callback: () -> Unit) {
        shutdownCallback = callback
    }

    fun addCloseListener(onClose: () -> Unit) {
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                onClose()
            }
        })
    }
}
