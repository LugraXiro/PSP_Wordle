package view

import viewmodel.LogLevel
import viewmodel.LogLine
import viewmodel.ServerViewModel
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.text.*
import java.awt.FlowLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Ventana principal de logs del servidor, implementada con Swing.
 *
 * Se suscribe al [ServerViewModel] para recibir actualizaciones de logs y las
 * renderiza con colores según el nivel semántico de cada línea. Solo contiene
 * lógica de presentación; toda la lógica de datos reside en el ViewModel.
 *
 * @param viewModel ViewModel al que suscribirse para recibir datos y enviar comandos.
 */
class LogsWindow(private val viewModel: ServerViewModel) {
    private val frame: JFrame
    private val textPane: JTextPane
    private val styledDoc: StyledDocument

    private val clientColors = listOf(
        Color(0x00, 0xD9, 0xFF),
        Color(0xFF, 0x6B, 0x9D),
        Color(0xFF, 0xC1, 0x45),
        Color(0xB5, 0x65, 0xFF),
        Color(0x00, 0xFF, 0xA3),
        Color(0xFF, 0x6B, 0x6B),
        Color(0x4E, 0xCD, 0xC4),
        Color(0xF7, 0xB7, 0x31),
        Color(0x57, 0xC7, 0xFF),
        Color(0xFF, 0x85, 0xA2),
        Color(0x5F, 0xD0, 0x68),
        Color(0xFF, 0xD9, 0x3D)
    )

    private val serverColor        = Color(0x00, 0xFF, 0x41)
    private val errorColor         = Color(0xFF, 0x6B, 0x6B)
    private val warnColor          = Color(0xFF, 0xA5, 0x00)
    private val infoColor          = Color(0x95, 0xE1, 0xD3)
    private val debugColor         = Color(0x88, 0x88, 0x88)
    private val defaultColor       = Color(0xCC, 0xCC, 0xCC)
    private val defaultClientColor = Color(0x4E, 0xCD, 0xC4)
    private val bgDark             = Color(0x1E, 0x1E, 0x1E)
    private val headerBg           = Color(0x2D, 0x2D, 0x2D)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
        clearButton.addActionListener { viewModel.clearLogs() }

        val shutdownBtn = JButton("\uD83D\uDED1 Apagar servidor")
        shutdownBtn.background = Color(0x8B, 0x00, 0x00)
        shutdownBtn.foreground = Color.WHITE
        shutdownBtn.isFocusPainted = false
        shutdownBtn.border = BorderFactory.createEmptyBorder(6, 12, 6, 12)
        shutdownBtn.addActionListener {
            shutdownBtn.isEnabled = false
            shutdownBtn.text = "Apagando..."
            viewModel.shutdown()
        }

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
    }

    private fun colorForLine(line: LogLine): Color = when (line.level) {
        LogLevel.SERVER  -> serverColor
        LogLevel.CLIENT  -> line.clientId?.let { id ->
            clientColors[Math.floorMod(id.hashCode(), clientColors.size)]
        } ?: defaultClientColor
        LogLevel.ERROR   -> errorColor
        LogLevel.WARN    -> warnColor
        LogLevel.INFO    -> infoColor
        LogLevel.DEBUG   -> debugColor
        LogLevel.DEFAULT -> defaultColor
    }

    fun show() {
        SwingUtilities.invokeLater { frame.isVisible = true }
        scope.launch {
            viewModel.logs.collect { lines ->
                SwingUtilities.invokeLater {
                    styledDoc.remove(0, styledDoc.length)
                    for (line in lines) {
                        val style = SimpleAttributeSet()
                        StyleConstants.setForeground(style, colorForLine(line))
                        StyleConstants.setFontFamily(style, "Monospaced")
                        StyleConstants.setFontSize(style, 12)
                        styledDoc.insertString(styledDoc.length, line.text + "\n", style)
                    }
                    textPane.caretPosition = styledDoc.length
                }
            }
        }
    }

    fun close() {
        scope.cancel()
        SwingUtilities.invokeLater {
            frame.isVisible = false
            frame.dispose()
        }
    }

    fun addCloseListener(onClose: () -> Unit) {
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                onClose()
            }
        })
    }
}
