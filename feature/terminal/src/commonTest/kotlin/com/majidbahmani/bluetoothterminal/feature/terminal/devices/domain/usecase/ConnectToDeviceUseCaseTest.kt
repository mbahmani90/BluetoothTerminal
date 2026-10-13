package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.fake.FakeDeviceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectToDeviceUseCaseTest {

    private val devices = FakeDeviceRepository()
    private val connection = FakeConnectionRepository()
    private val connectToDevice = ConnectToDeviceUseCase(devices, connection)

    @Test
    fun nothingConnected_connects() = runTest {
        connectToDevice(HM10)

        assertEquals(listOf(HM10), devices.connectRequests)
    }

    @Test
    fun sameDeviceConnecting_doesNothing() = runTest {
        connection.status.value = ConnectionStatus.Connecting(HM10_INFO)

        connectToDevice(HM10)

        assertEquals(emptyList(), devices.connectRequests)
    }

    @Test
    fun sameDeviceConnected_doesNothing() = runTest {
        connection.status.value = ConnectionStatus.Connected(HM10_INFO)

        connectToDevice(HM10)

        assertEquals(emptyList(), devices.connectRequests)
    }

    @Test
    fun otherDeviceConnected_connects() = runTest {
        connection.status.value = ConnectionStatus.Connected(DeviceInfo(id = "other", name = "Nordic"))

        connectToDevice(HM10)

        assertEquals(listOf(HM10), devices.connectRequests)
    }

    @Test
    fun sameDeviceFailedBefore_retries() = runTest {
        connection.status.value = ConnectionStatus.Failed(HM10_INFO, ConnectionError.Timeout)

        connectToDevice(HM10)

        assertEquals(listOf(HM10), devices.connectRequests)
    }

    private companion object {
        val HM10 = Device(id = "1", name = "HM-10", signalStrength = -60)
        val HM10_INFO = DeviceInfo(id = "1", name = "HM-10")
    }
}
