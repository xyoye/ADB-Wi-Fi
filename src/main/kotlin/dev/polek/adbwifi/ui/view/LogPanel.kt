package dev.polek.adbwifi.ui.view

import com.intellij.ui.JBColor
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.polek.adbwifi.ERROR_TAG
import dev.polek.adbwifi.model.LogEntry
import javax.swing.JTextPane
import javax.swing.border.EmptyBorder

class LogPanel : BorderLayoutPanel() {

    private val textPane = JTextPane().apply {
        isEditable = false
        contentType = "text/html"
        background = JBColor.background()
        border = EmptyBorder(INSET, INSET, INSET, INSET)
    }

    init {
        addToCenter(textPane)
    }

    fun setLogEntries(entries: List<LogEntry>) {
        textPane.text = html(entries)
    }

    private companion object {
        private const val INSET = 10

        private fun html(entries: List<LogEntry>): String {
            return """
                <html>
                    <head>
                        <style>
                        </style>
                    </head>
                    <body>
                        <code>
                            ${entries.joinToString(separator = "", transform = Companion::commandHtml)}
                        </code>
                    </body>
                </html>
            """.trimIndent()
        }

        private fun commandHtml(entry: LogEntry) = buildString {
            when (entry) {
                is LogEntry.Command -> {
                    appendLine("<b>> ${entry.text}</b>")
                    appendLine("<br/>")
                }
                is LogEntry.Output -> {
                    if (entry.text.isNotBlank()) {
                        entry.text.split("\n").forEach {
                            appendLine(formatOutput(it))
                            appendLine("<br/>")
                        }
                    }
                }
            }
        }

        private fun formatOutput(output: String): String {
            if (output.startsWith(ERROR_TAG)) {
                return "<span style=\"color:red;\">${output.substring(ERROR_TAG.length)}</span>"
            }
            return output
        }
    }
}
