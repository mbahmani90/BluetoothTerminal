package com.majidbahmani.bluetoothterminal.feature.terminal.settings.di

import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ObserveServiceSettingsUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ReadCharacteristicUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetListeningUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetSendTargetUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Needs `ChannelRepository` (connection package). */
internal val settingsModule = module {
    factory { ObserveServiceSettingsUseCase(repository = get()) }
    factory { SetSendTargetUseCase(repository = get()) }
    factory { SetListeningUseCase(repository = get()) }
    factory { ReadCharacteristicUseCase(repository = get()) }
    viewModel {
        SettingsViewModel(
            observeServiceSettings = get(),
            setSendTarget = get(),
            setListening = get(),
            readCharacteristic = get(),
        )
    }
}
