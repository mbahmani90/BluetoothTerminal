package com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceService
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.IncomingData
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** [ChannelRepository] the test drives: set services and selection, push incoming data, record what's sent. */
internal class FakeChannelRepository(
    selection: ChannelSelection = ChannelSelection(),
) : ChannelRepository {

    override val services = MutableStateFlow<List<DeviceService>>(emptyList())
    override val selection = MutableStateFlow(selection)
    override val incoming = MutableSharedFlow<IncomingData>(extraBufferCapacity = 64)

    /** When set, `send` and `read` fail with it. */
    var error: ChannelException? = null

    /** Values `read` returns. */
    val readValues = mutableMapOf<CharacteristicRef, ByteArray>()

    val sent = mutableListOf<ByteArray>()

    override fun setSendTarget(ref: CharacteristicRef?) {
        selection.value = selection.value.copy(sendTarget = ref)
    }

    override fun setListening(ref: CharacteristicRef, listening: Boolean) {
        val current = selection.value.listening
        selection.value = selection.value.copy(listening = if (listening) current + ref else current - ref)
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> {
        error?.let { return Result.failure(it) }
        sent += bytes
        return Result.success(Unit)
    }

    override suspend fun read(ref: CharacteristicRef): Result<ByteArray> {
        error?.let { return Result.failure(it) }
        return Result.success(readValues.getValue(ref))
    }
}
