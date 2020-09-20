package dev.polek.adbwifi.ui.view

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.polek.adbwifi.PluginBundle
import dev.polek.adbwifi.actions.OpenSettingsNotificationAction
import dev.polek.adbwifi.model.LogEntry
import dev.polek.adbwifi.ui.model.DeviceViewModel
import dev.polek.adbwifi.ui.presenter.ToolWindowPresenter
import dev.polek.adbwifi.utils.AbstractMouseListener
import dev.polek.adbwifi.utils.GridBagLayoutPanel
import dev.polek.adbwifi.utils.panel
import dev.polek.adbwifi.utils.setFontSize
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.event.MouseEvent
import javax.swing.*

class AdbWiFiToolWindow(
    private val project: Project,
    private val toolWindow: ToolWindow
) : BorderLayoutPanel(), Disposable, ToolWindowView {

    private val presenter = ToolWindowPresenter()

    private val splitter = JBSplitter(true, "AdbWifi.ShellPaneProportion", DEFAULT_PANEL_PROPORTION)
    private val deviceListPanel = DeviceListPanel(presenter)
    private val pinnedDeviceListPanel = DeviceListPanel(
        presenter,
        showHeader = true,
        title = PluginBundle.message("previouslyConnectedTitle")
    )
    private val logPanel = LogPanel()
    private val topPanel = JBScrollPane().apply {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        panel.add(deviceListPanel)
        panel.add(pinnedDeviceListPanel)
        this.setViewportView(panel)
    }
    private val bottomPanel: JComponent
    private val emptyMessageLabel = JBLabel().apply {
        text = PluginBundle.message("deviceListEmptyMessage")
        icon = IconLoader.getIcon("/icons/devices-lineup.png")
        horizontalAlignment = SwingConstants.CENTER
        horizontalTextPosition = SwingConstants.CENTER
        verticalTextPosition = SwingConstants.BOTTOM
        setFontSize(16f)
        background = JBColor.background()
        foreground = JBColor.gray
        isOpaque = true
        border = BorderFactory.createLineBorder(JBColor.border())
    }
    private val errorMessagePanel = GridBagLayoutPanel().apply {
        background = JBColor.background()
        border = BorderFactory.createLineBorder(JBColor.border())

        val label = JBLabel().apply {
            text = PluginBundle.message("adbPathVerificationErrorMessage", location)
            icon = IconLoader.getIcon("/icons/deviceWarning.png")
            horizontalAlignment = SwingConstants.CENTER
            horizontalTextPosition = SwingConstants.CENTER
            verticalTextPosition = SwingConstants.BOTTOM
            setFontSize(16f)
            foreground = JBColor.gray
        }
        add(
            label,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
            }
        )

        val settingsButton = HyperlinkLabel(PluginBundle.message("goToSettingsButton")).apply {
            setFontSize(16f)
            addMouseListener(object : AbstractMouseListener() {
                override fun mouseClicked(e: MouseEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(
                        null,
                        PluginBundle.message("settingsPageName")
                    )
                }
            })
        }
        add(
            settingsButton,
            GridBagConstraints().apply {
                gridx = 0
                gridy = 1
                insets = Insets(20, 0, 20, 0)
            }
        )
    }

    init {
        val actionManager = ActionManager.getInstance()
        val toolbarActionGroup = actionManager.getAction("AdbWifi.ToolbarActions") as DefaultActionGroup
        val toolbar = actionManager.createActionToolbar(
            ActionPlaces.TOOLWINDOW_TITLE,
            toolbarActionGroup,
            true
        )
        toolbar.setTargetComponent(this)
        addToTop(toolbar.component)
        addToCenter(splitter)

        val logToolbarActionGroup = actionManager.getAction("AdbWifi.LogToolbarActions") as DefaultActionGroup
        val logToolbar = actionManager.createActionToolbar(
            ActionPlaces.TOOLWINDOW_CONTENT,
            logToolbarActionGroup,
            false
        )
        bottomPanel = panel(center = JBScrollPane(logPanel), left = logToolbar.component)

        splitter.firstComponent = topPanel

        project.messageBus
            .connect(this)
            .subscribe(
                ToolWindowManagerListener.TOPIC,
                object : ToolWindowManagerListener {
                    override fun stateChanged() {
                        if (toolWindow.isVisible) {
                            presenter.onViewOpen()
                        } else {
                            presenter.onViewClosed()
                        }
                    }
                }
            )

        presenter.attach(this)
    }

    override fun dispose() {
        presenter.detach()
    }

    override fun showDevices(devices: List<DeviceViewModel>) {
        splitter.firstComponent = topPanel
        deviceListPanel.devices = devices
    }

    override fun showPinnedDevices(devices: List<DeviceViewModel>) {
        pinnedDeviceListPanel.devices = devices
        pinnedDeviceListPanel.isVisible = devices.isNotEmpty()
    }

    override fun showEmptyMessage() {
        splitter.firstComponent = emptyMessageLabel
    }

    override fun showInvalidAdbLocationError() {
        splitter.firstComponent = errorMessagePanel
    }

    override fun openLog() {
        splitter.secondComponent = bottomPanel
    }

    override fun closeLog() {
        splitter.secondComponent = null
    }

    override fun setLogEntries(entries: List<LogEntry>) {
        logPanel.setLogEntries(entries)
    }

    override fun showInvalidScrcpyLocationError() {
        val notification = NOTIFICATION_GROUP.createNotification(
            PluginBundle.message("name"),
            null,
            PluginBundle.message("scrcpyPathVerificationErrorMessage"),
            NotificationType.ERROR
        )
        notification.addAction(OpenSettingsNotificationAction())
        notification.notify(project)
    }

    private companion object {
        private const val DEFAULT_PANEL_PROPORTION = 0.6f
        private val NOTIFICATION_GROUP = NotificationGroup(
            PluginBundle.message("name"),
            NotificationDisplayType.BALLOON,
            false
        )
    }
}
