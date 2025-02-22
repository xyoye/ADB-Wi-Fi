package dev.polek.adbwifi.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import dev.polek.adbwifi.adb.ADB_DISPATCHER
import dev.polek.adbwifi.adb.Adb
import dev.polek.adbwifi.commandexecutor.RuntimeCommandExecutor
import dev.polek.adbwifi.model.Device
import dev.polek.adbwifi.utils.appCoroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class AdbService : Disposable {

    var deviceListListener: ((List<Device>) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                startPollingDevices()
            } else {
                stopPollingDevices()
            }
        }

    private val adb = Adb(RuntimeCommandExecutor(), service())
    private var devicePollingJob: Job? = null
    private val logService by lazy { service<LogService>() }
    private val pinDeviceService by lazy { service<PinDeviceService>() }
    private val properties by lazy { service<PropertiesService>() }

    suspend fun devices(): List<Device> {
        val devices = withContext(ADB_DISPATCHER) {
            adb.devices()
        }
        pinDeviceService.addPreviouslyConnectedDevices(devices)
        return devices
    }

    suspend fun connect(device: Device) {
        adb.connect(device).collect { logEntry ->
            logService.commandHistory.add(logEntry)
        }
    }

    fun connect(ip: String, port: Int = properties.adbPort): String {
        return adb.connect(ip, port)
    }

    suspend fun disconnect(device: Device) {
        adb.disconnect(device).collect { logEntry ->
            logService.commandHistory.add(logEntry)
        }
    }

    suspend fun onChangeLayoutBoundsStatus(device: Device) {
        adb.changeLayoutBoundsStatus(device.id, device.layoutBoundsShowing).collect { logEntry ->
            logService.commandHistory.add(logEntry)
        }
    }

    suspend fun onChangeGpuOverdrawStatus(device: Device) {
        adb.changeGpuOverdrawStatus(device.id, device.gupOverdrawShowing).collect { logEntry ->
            logService.commandHistory.add(logEntry)
        }
    }

    suspend fun onChangeHwuiRenderingStatus(device: Device) {
        adb.changeHwuiRenderingStatus(device.id, device.hwuiRenderingShowing).collect { logEntry ->
            logService.commandHistory.add(logEntry)
        }
    }

    suspend fun onInstallApk(device: Device, apkPath: String) {
        adb.installApk(device.id, apkPath).collect { logEntry ->
            logService.commandHistory.add(logEntry)
        }
    }

    fun getFocusedApplication(device: Device): String {
        return adb.getFocusedApplication(device.id)
    }

    fun onClearApplicationData(device: Device, packageName: String): String {
        return adb.clearApplicationData(device.id, packageName)
    }

    fun restartAdb() {
        appCoroutineScope.launch(Dispatchers.Default) {
            adb.disconnectAllDevices().collect { logEntry ->
                logService.commandHistory.add(logEntry)
            }
            adb.killServer().collect { logEntry ->
                logService.commandHistory.add(logEntry)
            }
        }
    }

    override fun dispose() {
        stopPollingDevices()
    }

    private fun startPollingDevices() {
        devicePollingJob?.cancel()
        devicePollingJob = appCoroutineScope.launch(Main) {
            devicesFlow()
                .collect { devices ->
                    deviceListListener?.invoke(devices)
                }
        }
    }

    private fun stopPollingDevices() {
        devicePollingJob?.cancel()
        devicePollingJob = null
    }

    private fun devicesFlow(): Flow<List<Device>> = flow {
        while (true) {
            emit(devices())
            delay(POLLING_INTERVAL_MILLIS)
        }
    }

    private companion object {
        const val POLLING_INTERVAL_MILLIS = 3000L
    }
}
