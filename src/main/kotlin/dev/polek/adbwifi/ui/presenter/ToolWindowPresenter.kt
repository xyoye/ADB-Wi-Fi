package dev.polek.adbwifi.ui.presenter

import com.intellij.openapi.components.service
import dev.polek.adbwifi.model.CommandHistory
import dev.polek.adbwifi.model.LogEntry
import dev.polek.adbwifi.model.PinnedDevice
import dev.polek.adbwifi.services.*
import dev.polek.adbwifi.ui.model.DeviceViewModel
import dev.polek.adbwifi.ui.model.DeviceViewModel.Companion.toViewModel
import dev.polek.adbwifi.ui.view.ToolWindowView
import dev.polek.adbwifi.utils.BasePresenter
import dev.polek.adbwifi.utils.copyToClipboard
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ToolWindowPresenter : BasePresenter<ToolWindowView>() {

    private val adbService by lazy { service<AdbService>() }
    private val scrcpyService by lazy { service<ScrcpyService>() }
    private val logService by lazy { service<LogService>() }
    private val propertiesService by lazy { service<PropertiesService>() }
    private val pinDeviceService by lazy { service<PinDeviceService>() }

    private var isViewOpen: Boolean = false
    private var isAdbValid: Boolean = true
    private var devices: List<DeviceViewModel> = emptyList()
    private var pinnedDevices: List<DeviceViewModel> = pinDeviceService.pinnedDevices.toViewModel()

    private var connectingDevices = mutableSetOf<String/*Device's unique ID*/>()

    override fun attach(view: ToolWindowView) {
        super.attach(view)
        view.showEmptyMessage()
        subscribeToDeviceList()
        subscribeToLogEvents()
        subscribeToAdbLocationChanges()
    }

    override fun detach() {
        unsubscribeFromDeviceList()
        unsubscribeFromLogEvents()
        unsubscribeFromAdbLocationChanges()
        super.detach()
    }

    fun onViewOpen() {
        isViewOpen = true
        if (isAdbValid) {
            subscribeToDeviceList()
        }
    }

    fun onViewClosed() {
        isViewOpen = false
        unsubscribeFromDeviceList()
    }

    fun onConnectButtonClicked(device: DeviceViewModel) {
        connectingDevices.add(device.uniqueId)
        updateDeviceLists()

        launch(Main) {
            withContext(IO) {
                adbService.connect(device.device)
            }
        }.invokeOnCompletion {
            connectingDevices.remove(device.uniqueId)
            updateDeviceLists()
        }
    }

    fun onDisconnectButtonClicked(device: DeviceViewModel) {
        connectingDevices.add(device.uniqueId)
        updateDeviceLists()

        launch(Main) {
            withContext(IO) {
                adbService.disconnect(device.device)
            }
        }.invokeOnCompletion {
            connectingDevices.remove(device.uniqueId)
            updateDeviceLists()
        }
    }

    fun onShareScreenButtonClicked(device: DeviceViewModel) {
        if (scrcpyService.isScrcpyValid()) {
            scrcpyService.share(device.device)
        } else {
            view?.showInvalidScrcpyLocationError()
        }
    }

    fun onRemoveDeviceButtonClicked(device: DeviceViewModel) {
        pinDeviceService.removePreviouslyConnectedDevice(device.device)
        pinnedDevices = pinDeviceService.pinnedDevices.toViewModel()
        view?.showPinnedDevices(pinnedDevices)
    }

    fun onCopyDeviceIdClicked(device: DeviceViewModel) {
        copyToClipboard(device.device.id)
    }

    fun onCopyDeviceAddressClicked(device: DeviceViewModel) {
        val address = device.device.address ?: return
        copyToClipboard(address)
    }

    private fun updateDeviceLists() {
        devices.forEach {
            it.isInProgress = connectingDevices.contains(it.uniqueId)
        }
        pinnedDevices.forEach {
            it.isInProgress = connectingDevices.contains(it.uniqueId)
        }

        if (devices.isEmpty() && pinnedDevices.isEmpty()) {
            view?.showEmptyMessage()
        } else {
            view?.showDevices(devices)
            view?.showPinnedDevices(pinnedDevices)
        }
    }

    private fun subscribeToDeviceList() {
        if (adbService.deviceListListener != null) {
            // Already subscribed
            return
        }
        adbService.deviceListListener = { model ->
            devices = model.map { it.toViewModel() }
            pinnedDevices = pinDeviceService.pinnedDevices.toViewModel()

            updateDeviceLists()
        }
    }

    private fun unsubscribeFromDeviceList() {
        if (adbService.deviceListListener == null) {
            // Already unsubscribed
            return
        }
        adbService.deviceListListener = null
    }

    private fun subscribeToLogEvents() {
        logService.logVisibilityListener = ::updateLogVisibility
    }

    private fun unsubscribeFromLogEvents() {
        logService.logVisibilityListener = null
    }

    private fun updateLogVisibility(isLogVisible: Boolean) {
        if (isLogVisible) {
            view?.openLog()
            logService.commandHistory.listener = object : CommandHistory.Listener {
                override fun onLogEntriesModified(entries: List<LogEntry>) {
                    view?.setLogEntries(entries)
                }
            }
        } else {
            view?.closeLog()
            logService.commandHistory.listener = null
        }
    }

    private fun subscribeToAdbLocationChanges() {
        propertiesService.adbLocationListener = { isValid ->
            isAdbValid = isValid
            if (!isValid) {
                unsubscribeFromDeviceList()
                devices = emptyList()
                view?.showInvalidAdbLocationError()
            } else {
                if (devices.isEmpty() && pinnedDevices.isEmpty()) {
                    view?.showEmptyMessage()
                } else {
                    view?.showDevices(devices)
                    view?.showPinnedDevices(pinnedDevices)
                }
                if (isViewOpen) {
                    subscribeToDeviceList()
                }
            }
        }
    }

    private fun unsubscribeFromAdbLocationChanges() {
        propertiesService.adbLocationListener = null
    }

    private fun List<PinnedDevice>.toViewModel(): List<DeviceViewModel> {
        return this.asSequence()
            .filter { pinnedDevice ->
                devices.find { device ->
                    device.androidId == pinnedDevice.androidId && device.address == pinnedDevice.address
                } == null
            }
            .sortedBy { it.name }
            .map { it.toViewModel() }
            .toList()
    }
}
