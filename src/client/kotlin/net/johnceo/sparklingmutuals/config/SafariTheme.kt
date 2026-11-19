package net.johnceo.sparklingmutuals.config

/** Stable chat/waypoint accents. Settings surfaces use SettingsPalette. */
// Hunting Helper: Hunting Helper's matte charcoal theme (MenuTheme). Upstream: ACCENT 0xC9A75D, BACKGROUND 0x0D0D10,
// SURFACE 0x19191E, HOVER 0x292930, BORDER 0x605238, TEXT 0xE6E6EB, MUTED 0xA6A6B0, CHAT_TEXT 0xF1F1F5, CHAT_ACCENT 0xE6C878.
object SafariTheme {
    const val ACCENT = 0xB7C7D6
    const val BACKGROUND = 0x181A1E
    const val SURFACE = 0x212328
    const val HOVER = 0x272A30
    const val BORDER = 0x41454D
    /** Body text and muted text, as the settings screen draws them. */
    const val TEXT = 0xF0F1F4
    const val MUTED = 0xA4A8B1
    const val CHAT_TEXT = 0xF0F1F4
    const val CHAT_ACCENT = 0xB7C7D6
    /** Hunting Helper: the upstream defaults, so panels saved with them move to the new theme. */
    val UPSTREAM_BORDER = 0x605238
    val UPSTREAM_BACKGROUND = 0x0D0D10
}
