package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.di

import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.ObserveMessagesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.SendMessageUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.presentation.TerminalViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Needs `ChannelRepository` and the connection use cases (connection package). */
internal val terminalModule = module {
    factory { ObserveMessagesUseCase(repository = get()) }
    factory { SendMessageUseCase(repository = get()) }
    viewModel {
        TerminalViewModel(
            observeConnection = get(),
            observeMessages = get(),
            sendMessage = get(),
            disconnect = get(),
        )
    }
}
