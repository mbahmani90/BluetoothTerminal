package com.majidbahmani.bluetoothterminal

import platform.Foundation.NSUserDefaults

internal class UserDefaultsCyLinkoPromptStore : CyLinkoPromptStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override val isMuted: Boolean get() = defaults.boolForKey(CYLINKO_PROMPT_MUTED_KEY)

    override fun mute() {
        defaults.setBool(true, forKey = CYLINKO_PROMPT_MUTED_KEY)
    }
}
