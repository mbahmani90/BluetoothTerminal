package com.majidbahmani.bluetoothterminal.feature.terminal.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ObserveServiceSettingsUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ReadCharacteristicUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetListeningUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetSendTargetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The connected device's services; changes apply to the Terminal right away (shared `ChannelRepository`). */
internal class SettingsViewModel(
    observeServiceSettings: ObserveServiceSettingsUseCase,
    private val setSendTarget: SetSendTargetUseCase,
    private val setListening: SetListeningUseCase,
    private val readCharacteristic: ReadCharacteristicUseCase,
) : ViewModel() {

    private val reads = MutableStateFlow<Map<CharacteristicRef, ReadResult>>(emptyMap())

    val uiState: StateFlow<SettingsUiState> = combine(observeServiceSettings(), reads) { services, reads ->
        SettingsUiState(services = services, reads = reads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun onSendTargetSelect(ref: CharacteristicRef) {
        setSendTarget(ref)
    }

    fun onListeningChange(ref: CharacteristicRef, listening: Boolean) {
        setListening(ref, listening)
    }

    fun onRead(ref: CharacteristicRef) {
        reads.update { it + (ref to ReadResult.Reading) }
        viewModelScope.launch {
            val result = readCharacteristic(ref).fold(
                onSuccess = { ReadResult.Value(it.toDisplayText()) },
                onFailure = { ReadResult.Failed((it as? ChannelException)?.error ?: ChannelError.Failed) },
            )
            reads.update { it + (ref to result) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Text if every byte decodes to a printable character (device name, firmware version, …), else hex. */
internal fun ByteArray.toDisplayText(): String {
    val text = decodeToString()
    val printable = isNotEmpty() && '�' !in text && text.all { !it.isISOControl() || it == '\n' || it == '\r' || it == '\t' }
    return if (printable) text else joinToString(" ") { (it.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0') }
}
