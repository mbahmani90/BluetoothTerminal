package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ConnectToDeviceUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ScanDevicesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class DevicesViewModel(
    private val scanDevices: ScanDevicesUseCase,
    private val connectToDevice: ConnectToDeviceUseCase,
    observeConnection: ObserveConnectionUseCase,
) : ViewModel() {

    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val connection: StateFlow<ConnectionStatus> = observeConnection()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConnectionStatus.Disconnected)

    /**
     * Paused while connecting: on Android, scanning during a connect makes connection errors (status 133)
     * more likely. While paused nothing is emitted, so the screen keeps the last list. Retry restarts it.
     */
    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    private val scan: Flow<DeviceScan> = combine(
        connection.map { it is ConnectionStatus.Connecting }.distinctUntilChanged(),
        retryTrigger.onStart { emit(Unit) },
    ) { connecting, _ -> connecting }
        .flatMapLatest { connecting -> if (connecting) emptyFlow() else scanDevices() }

    /** WhileSubscribed: the scan stops a few seconds after the screen is no longer visible (battery). */
    val uiState: StateFlow<DevicesUiState> = combine(scan, connection) { scan, connection ->
        DevicesUiState(scan = scan, connection = connection)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DevicesUiState())

    fun onDeviceClick(device: Device) {
        viewModelScope.launch { connectToDevice(device) }
    }

    fun onRetry() {
        retryTrigger.tryEmit(Unit)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
