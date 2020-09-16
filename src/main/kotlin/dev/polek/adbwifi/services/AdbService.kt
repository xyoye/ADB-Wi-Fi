package dev.polek.adbwifi.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import dev.polek.adbwifi.adb.ADB_DISPATCHER
import dev.polek.adbwifi.adb.Adb
import dev.polek.adbwifi.commandexecutor.RuntimeCommandExecutor
import dev.polek.adbwifi.model.Device
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@OptIn(FlowPreview::class)
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

    fun connect(device: Device) {
        stopPollingDevices()

        GlobalScope.launch(ADB_DISPATCHER) {
            adb.connect(device).collect { logEntry ->
                logService.commandHistory.add(logEntry)
            }
            withContext(Dispatchers.Main) {
                delay(1000)
                startPollingDevices()
            }
        }
    }

    fun disconnect(device: Device) {
        stopPollingDevices()

        GlobalScope.launch(ADB_DISPATCHER) {
            adb.disconnect(device).collect { logEntry ->
                logService.commandHistory.add(logEntry)
            }
            withContext(Dispatchers.Main) {
                delay(1000)
                startPollingDevices()
            }
        }
    }

    fun killServer() {
        GlobalScope.launch(Dispatchers.Default) {
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
        devicePollingJob = GlobalScope.launch(Dispatchers.Main) {
            devicesFlow()
                .flowOn(ADB_DISPATCHER)
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
            emit(adb.devices())
            delay(POLLING_INTERVAL_MILLIS)
        }
    }

    private companion object {
        const val POLLING_INTERVAL_MILLIS = 3000L
    }
}
