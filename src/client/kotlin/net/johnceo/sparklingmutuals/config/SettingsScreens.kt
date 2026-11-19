// Hunting Helper: lets Hunting Helper's settings UI replace SafariConfigScreen everywhere it is opened.
package net.johnceo.sparklingmutuals.config

import net.minecraft.client.gui.screens.Screen

/** Opens the settings workspace. Hunting Helper installs [factory]; upstream's screen is the fallback. */
object SettingsScreens {
    @JvmField var factory: ((Screen?, SettingsTarget?) -> Screen)? = null
    fun create(returnTo: Screen?, target: SettingsTarget?): Screen =
        factory?.invoke(returnTo, target) ?: SafariConfigScreen(initialTarget = target, returnTo = returnTo)
}
