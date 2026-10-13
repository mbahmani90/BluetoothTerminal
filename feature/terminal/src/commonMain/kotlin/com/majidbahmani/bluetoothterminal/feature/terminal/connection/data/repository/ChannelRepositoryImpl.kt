package com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository

import com.majidbahmani.bluetooth.BleConnection
import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.GattService
import com.majidbahmani.bluetooth.model.SessionState
import com.majidbahmani.bluetooth.model.find
import com.majidbahmani.bluetooth.serial.SerialProfiles
import com.majidbahmani.bluetooth.serial.canNotify
import com.majidbahmani.bluetooth.serial.canWrite
import com.majidbahmani.bluetooth.serial.writeChunked
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceCharacteristic
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceService
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.IncomingData
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update

/** [ChannelRepository] on top of the session's current [BleConnection]; library types stop here. */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
internal class ChannelRepositoryImpl(private val session: BluetoothSession) : ChannelRepository {

    /** The user's changes, tagged with the connection they belong to; a new connection starts from its default. */
    private val changes = MutableStateFlow<Pair<BleConnection, ChannelSelection>?>(null)

    private val connection: Flow<BleConnection?> = session.state
        .map { (it as? SessionState.Connected)?.connection }
        .distinctUntilChanged { old, new -> old === new }

    override val services: Flow<List<DeviceService>> =
        connection.map { it?.services.orEmpty().map { service -> service.toDomain() } }

    override val selection: Flow<ChannelSelection> = connection.flatMapLatest { connection ->
        if (connection == null) flowOf(ChannelSelection()) else selectionOf(connection)
    }

    override val incoming: Flow<IncomingData> = connection.flatMapLatest { connection ->
        if (connection == null) {
            emptyFlow()
        } else {
            selectionOf(connection)
                .map { it.listening }
                .distinctUntilChanged()
                .flatMapLatest { refs -> merge(*refs.map { connection.incomingFrom(it) }.toTypedArray()) }
        }
    }

    override fun setSendTarget(ref: CharacteristicRef?) {
        change { connection, selection ->
            if (ref != null && connection.services.find(ref.toId())?.canWrite() != true) return@change selection
            selection.copy(sendTarget = ref)
        }
    }

    override fun setListening(ref: CharacteristicRef, listening: Boolean) {
        change { connection, selection ->
            if (connection.services.find(ref.toId())?.canNotify() != true) return@change selection
            selection.copy(listening = if (listening) selection.listening + ref else selection.listening - ref)
        }
    }

    override suspend fun send(bytes: ByteArray): Result<Unit> {
        val connection = session.connection ?: return failure(ChannelError.NotConnected)
        val target = selectionOf(connection).first().sendTarget ?: return failure(ChannelError.NoSendTarget)
        return connection.writeChunked(target.toId(), bytes).mapFailure()
    }

    override suspend fun read(ref: CharacteristicRef): Result<ByteArray> {
        val connection = session.connection ?: return failure(ChannelError.NotConnected)
        if (connection.services.find(ref.toId())?.properties?.contains(CharacteristicProperty.Read) != true) {
            return failure(ChannelError.NotSupported)
        }
        return connection.read(ref.toId()).mapFailure()
    }

    private fun selectionOf(connection: BleConnection): Flow<ChannelSelection> = changes
        .map { change -> if (change?.first === connection) change.second else connection.defaultSelection() }
        .distinctUntilChanged()

    /** Applies [transform] to the current connection's selection; does nothing while not connected. */
    private fun change(transform: (BleConnection, ChannelSelection) -> ChannelSelection) {
        val connection = session.connection ?: return
        changes.update { current ->
            val selection = if (current?.first === connection) current.second else connection.defaultSelection()
            connection to transform(connection, selection)
        }
    }

    /** One characteristic's notifications; an error there (e.g. can't subscribe) only stops that one. */
    private fun BleConnection.incomingFrom(ref: CharacteristicRef): Flow<IncomingData> =
        observe(ref.toId()).map { IncomingData(ref, it) }.catch { }
}

/** The detected serial channel, or nothing selected if the device has none. */
internal fun BleConnection.defaultSelection(): ChannelSelection =
    SerialProfiles.detect(services)
        ?.let { ChannelSelection(sendTarget = it.write.toRef(), listening = setOf(it.notify.toRef())) }
        ?: ChannelSelection()

internal fun GattService.toDomain(): DeviceService = DeviceService(
    uuid = uuid,
    characteristics = characteristics.map {
        DeviceCharacteristic(
            ref = CharacteristicRef(service = uuid, characteristic = it.uuid),
            canRead = CharacteristicProperty.Read in it.properties,
            canWrite = it.canWrite(),
            canNotify = it.canNotify(),
        )
    },
)

private fun CharacteristicRef.toId(): CharacteristicId = CharacteristicId(service, characteristic)

private fun CharacteristicId.toRef(): CharacteristicRef = CharacteristicRef(service, characteristic)

private fun <T> failure(error: ChannelError): Result<T> = Result.failure(ChannelException(error))

private fun <T> Result<T>.mapFailure(): Result<T> = fold(
    onSuccess = { this },
    onFailure = { error ->
        failure(
            when (error) {
                is BluetoothException.NotConnected -> ChannelError.NotConnected
                is BluetoothException.CharacteristicNotFound -> ChannelError.NotSupported
                else -> ChannelError.Failed
            },
        )
    },
)
