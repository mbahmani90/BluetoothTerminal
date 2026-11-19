package com.majidbahmani.bluetoothterminal

import android.content.Context

internal class SharedPrefsCyLinkoPromptStore(context: Context) : CyLinkoPromptStore {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    override val isMuted: Boolean get() = prefs.getBoolean(CYLINKO_PROMPT_MUTED_KEY, false)

    override fun mute() {
        prefs.edit().putBoolean(CYLINKO_PROMPT_MUTED_KEY, true).apply()
    }
}
