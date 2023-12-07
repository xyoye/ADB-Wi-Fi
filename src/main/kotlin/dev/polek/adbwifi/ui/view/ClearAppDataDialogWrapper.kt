package dev.polek.adbwifi.ui.view

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import dev.polek.adbwifi.PluginBundle
import dev.polek.adbwifi.model.Device
import dev.polek.adbwifi.services.AdbService
import dev.polek.adbwifi.utils.GridBagLayoutPanel
import dev.polek.adbwifi.utils.appCoroutineScope
import dev.polek.adbwifi.utils.makeMonospaced
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ClearAppDataDialogWrapper(
    private val device: Device,
    private val focusedPackageName: String
) : DialogWrapper(true) {

    private lateinit var packageNameLabel: JBLabel
    private lateinit var packageNameTextField: JBTextField
    private lateinit var refreshButton: JButton
    private lateinit var clearButton: JButton
    private lateinit var outputLabel: JBLabel

    private var clearJob: Job? = null
    private var refreshJob: Job? = null

    init {
        init()
        isResizable = false
        title = PluginBundle.message("clearApplicationDataDialogTitle")
    }

    override fun createCenterPanel(): JComponent {
        val panel = GridBagLayoutPanel()

        packageNameLabel = JBLabel(PluginBundle.message("packageNameLabel"))
        panel.add(
            packageNameLabel,
            GridBagConstraints().apply {
                gridx = 0
            }
        )

        packageNameTextField = JBTextField(25)
        packageNameTextField.text = focusedPackageName
        packageNameTextField.makeMonospaced()
        packageNameTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updateClearButton()
            override fun removeUpdate(e: DocumentEvent) = updateClearButton()
            override fun changedUpdate(e: DocumentEvent) = updateClearButton()
        })
        packageNameTextField.addActionListener {
            clearApplicationData()
        }
        panel.add(
            packageNameTextField,
            GridBagConstraints().apply {
                gridx = 1
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = Insets(0, 5, 0, 5)
            }
        )

        refreshButton = JButton(AllIcons.Actions.Refresh)
        refreshButton.addActionListener {
            refreshFocusedApplication()
        }
        panel.add(
            refreshButton,
            GridBagConstraints().apply {
                gridx = 2
            }
        )

        clearButton = JButton(PluginBundle.message("clearButton"))
        clearButton.addActionListener {
            clearApplicationData()
        }
        panel.add(
            clearButton,
            GridBagConstraints().apply {
                gridx = 3
            }
        )

        outputLabel = JBLabel()
        outputLabel.componentStyle = UIUtil.ComponentStyle.SMALL
        outputLabel.foreground = OUTPUT_TEXT_COLOR
        outputLabel.isVisible = false
        panel.add(
            outputLabel,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                gridwidth = 3
                fill = GridBagConstraints.BOTH
                weighty = 1.0
                insets = Insets(10, 0, 0, 0)
            }
        )

        updateClearButton()

        return panel
    }

    override fun dispose() {
        clearJob?.cancel()
        clearJob = null
        refreshJob?.cancel()
        refreshJob = null
        super.dispose()
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun refreshFocusedApplication() {
        val adbService = service<AdbService>()
        refreshJob = appCoroutineScope.launch(Dispatchers.IO) {
            val output = adbService.getFocusedApplication(device)
            if (output.isNotEmpty()) {
                packageNameTextField.text = output
                packageNameTextField.requestFocusInWindow()
            }
        }
    }

    private fun clearApplicationData() {
        showConnectionProgress()
        outputLabel.text = ""

        val packageName = packageNameTextField.text.trim()
        packageNameTextField.text = packageName

        val adbService = service<AdbService>()
        clearJob = appCoroutineScope.launch(Dispatchers.IO) {
            val output = adbService.onClearApplicationData(device, packageName)
            hideConnectionProgress()
            outputLabel.text = output
            outputLabel.isVisible = true
            packageNameTextField.requestFocusInWindow()
        }
    }

    private fun showConnectionProgress() {
        packageNameLabel.isEnabled = false
        packageNameTextField.isEnabled = false
        clearButton.isEnabled = false
        clearButton.icon = AnimatedIcon.Default()
    }

    private fun hideConnectionProgress() {
        packageNameLabel.isEnabled = true
        packageNameTextField.isEnabled = true
        clearButton.isEnabled = true
        clearButton.icon = null
    }

    private fun updateClearButton() {
        clearButton.isEnabled = packageNameTextField.text.isNotBlank()
    }

    private companion object {
        private val OUTPUT_TEXT_COLOR = JBColor(0x787878, 0xBBBBBB)
    }
}