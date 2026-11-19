// Hunting Helper: settings page for the CritterMod features, built from Sparkling Mutuals' settings metadata.
package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.compat.smSetScreen
import net.johnceo.sparklingmutuals.config.Accordion
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.ConfigEditorBoolean
import net.johnceo.sparklingmutuals.config.ConfigEditorButton
import net.johnceo.sparklingmutuals.config.ConfigEditorColour
import net.johnceo.sparklingmutuals.config.ConfigEditorDropdown
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ConfigOption
import net.johnceo.sparklingmutuals.config.PanelAppearanceSettings
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/** Screens owned by Hunting Helper's UI, installed at startup so this package does not depend on it. */
object HuntingScreens {
    @JvmField var report: ((Screen?) -> Screen)? = null
    fun openReport() {
        val client = Minecraft.getInstance()
        val factory = report ?: return
        client.execute { client.smSetScreen(factory(client.smScreen)) }
    }
}

private fun placement() = Runnable { val client = Minecraft.getInstance(); client.smSetScreen(ContestGui(client.smScreen)) }
private val audiences = Audience.labels

/** View model; [apply] writes it to [HuntingConfig]. Field names are stable navigation keys. */
class HuntingSettings {
    @JvmField @ConfigOption(name = "Run announcements", desc = "Once per run, counted like the missing panel extras.")
    @Accordion val announcements = Announcements()
    @JvmField @ConfigOption(name = "Hunter trades", desc = "Offers from the roaming Hunters, shared with CritterMod's party format.")
    @Accordion val trades = Trades()
    @JvmField @ConfigOption(name = "World marks", desc = "")
    @Accordion val marks = Marks()
    @JvmField @ConfigOption(name = "Encounter broadcasts", desc = "CritterMod party/all-chat lines. Sparkling Mutuals already shows the banners.")
    @Accordion val broadcasts = Broadcasts()
    @JvmField @ConfigOption(name = "Subtract Bazaar tax", desc = "Shard values in the run report, after the 1.25% sell tax. Price side: History → Coins at insta-sell.")
    @ConfigEditorBoolean var subtractTax = HuntingConfig.subtractTax
    @JvmField @ConfigOption(name = "This run", desc = "Who caught what, capsule attempts, escapes and shard value.")
    @ConfigEditorButton(buttonText = "Open report") val report = Runnable { HuntingScreens.openReport() }
    @JvmField @ConfigOption(name = "Import CritterMod data", desc = "Settings and run history from config/crittermod*.json and Hunting Helper 1.6 field journal files.")
    @ConfigEditorButton(buttonText = "Import again") val importData = Runnable { HuntingImport.runManual() }

    class Announcements {
        @JvmField @ConfigOption(name = "Everything except Macaw done!", desc = "") @ConfigEditorBoolean var allButMacaw = HuntingConfig.allButMacawAlert
        @JvmField @ConfigOption(name = "Everything Done!", desc = "") @ConfigEditorBoolean var allDone = HuntingConfig.allDoneAlert
        @JvmField @ConfigOption(name = "Also tell", desc = "") @ConfigEditorDropdown(values = ["Nobody", "Party", "All"])
        var audience = HuntingConfig.milestoneAudience.ordinal
    }
    class Trades {
        @JvmField @ConfigOption(name = "Trades HUD", desc = "Trades found this run, nearest first.") @ConfigEditorBoolean var hud = HuntingConfig.tradeHud
        @JvmField @ConfigOption(name = "Trade waypoints", desc = "") @ConfigEditorBoolean var marks = HuntingConfig.tradeMarks
        @JvmField @ConfigOption(name = "Chat report", desc = "") @ConfigEditorBoolean var chat = HuntingConfig.tradeChat
        @JvmField @ConfigOption(name = "Accept partymates' trades", desc = "Read offers other Hunting Helper and CritterMod users post.")
        @ConfigEditorBoolean var shared = HuntingConfig.acceptSharedTrades
        @JvmField @ConfigOption(name = "Share trades with", desc = "Offers and \"Trade taken\".") @ConfigEditorDropdown(values = ["Nobody", "Party", "All"])
        var audience = HuntingConfig.tradeAudience.ordinal
        @JvmField @ConfigOption(name = "Waypoint color", desc = "") @ConfigEditorColour var color = HuntingConfig.tradeColor
        @JvmField @ConfigOption(name = "HUD appearance", desc = "") @Accordion
        var appearance = PanelAppearanceSettings(AppearanceConfig.panels.getValue("hunting_trades")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Position and size", desc = "") @ConfigEditorButton(buttonText = "Edit HUDs") val placement = placement()
    }
    class Marks {
        @JvmField @ConfigOption(name = "Hideonwall perches", desc = "Eight perches; a mark clears once seen empty or a capsule passes through.")
        @ConfigEditorBoolean var hideonwall = HuntingConfig.hideonwallMarks
        @JvmField @ConfigOption(name = "Unchecked perch color", desc = "") @ConfigEditorColour var perchColor = HuntingConfig.hideonwallColor
        @JvmField @ConfigOption(name = "Occupied perch color", desc = "") @ConfigEditorColour var foundColor = HuntingConfig.hideonwallFoundColor
        @JvmField @ConfigOption(name = "Recatch pins", desc = "Where a critter was when your capsule hit it, until it is caught or runs off.")
        @ConfigEditorBoolean var recatch = HuntingConfig.recatchMarks
        @JvmField @ConfigOption(name = "Recatch color", desc = "") @ConfigEditorColour var recatchColor = HuntingConfig.recatchColor
        @JvmField @ConfigOption(name = "Troodon walls", desc = "The three breakable Icy walls, until each is seen broken.")
        @ConfigEditorBoolean var troodon = HuntingConfig.troodonWallMarks
        @JvmField @ConfigOption(name = "Troodon wall color", desc = "") @ConfigEditorColour var troodonColor = HuntingConfig.troodonWallColor
    }
    class Broadcasts {
        @JvmField @ConfigOption(name = "Gemzie", desc = "") @ConfigEditorDropdown(values = ["Nobody", "Party", "All"]) var gemzie = HuntingConfig.gemzieAudience.ordinal
        @JvmField @ConfigOption(name = "Wumpa", desc = "") @ConfigEditorDropdown(values = ["Nobody", "Party", "All"]) var wumpa = HuntingConfig.wumpaAudience.ordinal
        @JvmField @ConfigOption(name = "Doomspiral", desc = "") @ConfigEditorDropdown(values = ["Nobody", "Party", "All"]) var doomspiral = HuntingConfig.doomspiralAudience.ordinal
        @JvmField @ConfigOption(name = "Macaw spawned", desc = "") @ConfigEditorDropdown(values = ["Nobody", "Party", "All"]) var macaw = HuntingConfig.macawAudience.ordinal
    }

    /** Writes the view model to [HuntingConfig]; returns true when anything changed. */
    fun apply(): Boolean {
        val before = HuntingConfig.write()
        fun color(value: String, fallback: String) = SafariEspConfig.validColor(value, fallback)
        with(HuntingConfig) {
            allButMacawAlert = announcements.allButMacaw; allDoneAlert = announcements.allDone
            milestoneAudience = Audience.of(announcements.audience)
            tradeHud = trades.hud; tradeMarks = trades.marks; tradeChat = trades.chat; acceptSharedTrades = trades.shared
            tradeAudience = Audience.of(trades.audience); tradeColor = color(trades.color, tradeColor)
            hideonwallMarks = marks.hideonwall; hideonwallColor = color(marks.perchColor, hideonwallColor)
            hideonwallFoundColor = color(marks.foundColor, hideonwallFoundColor)
            recatchMarks = marks.recatch; recatchColor = color(marks.recatchColor, recatchColor)
            troodonWallMarks = marks.troodon; troodonWallColor = color(marks.troodonColor, troodonWallColor)
            gemzieAudience = Audience.of(broadcasts.gemzie); wumpaAudience = Audience.of(broadcasts.wumpa)
            doomspiralAudience = Audience.of(broadcasts.doomspiral); macawAudience = Audience.of(broadcasts.macaw)
            subtractTax = this@HuntingSettings.subtractTax
        }
        val changed = before != HuntingConfig.write()
        if (changed) HuntingConfig.save()
        if (trades.appearance.apply()) ConfigManager.save()
        return changed
    }
}

/** "Missing panel extras" under HUDs → Missing / Unchecked. Field names are stable navigation keys. */
class MissingExtrasSettings {
    @JvmField @ConfigOption(name = "Enabled", desc = "Quota rows, located counts and \"also here\" on the missing panel.")
    @ConfigEditorBoolean var enabled = HuntingConfig.missingExtras
    @JvmField @ConfigOption(name = "Uniques only", desc = "On: exactly Sparkling Mutuals' list. Off: Gemzie, Troodon, Gazer and Scrappy stay listed until their quota (\"Gazer 2/4\").")
    @ConfigEditorBoolean var uniquesOnly = HuntingConfig.uniquesOnly
    @JvmField @ConfigOption(name = "Located counts", desc = "\"· 1 located\" for creatures the run has seen but not caught.")
    @ConfigEditorBoolean var located = HuntingConfig.missingLocated
    @JvmField @ConfigOption(name = "Also here", desc = "Species already done that still have located creatures.")
    @ConfigEditorBoolean var alsoHere = HuntingConfig.missingAlsoHere
    @JvmField @ConfigOption(name = "Troodon walls left", desc = "In Icy, the breakable walls still standing.")
    @ConfigEditorBoolean var troodonWalls = HuntingConfig.troodonWallCount

    fun apply(): Boolean {
        val before = HuntingConfig.write()
        HuntingConfig.missingExtras = enabled; HuntingConfig.uniquesOnly = uniquesOnly
        HuntingConfig.missingLocated = located; HuntingConfig.missingAlsoHere = alsoHere; HuntingConfig.troodonWallCount = troodonWalls
        val changed = before != HuntingConfig.write()
        if (changed) HuntingConfig.save()
        return changed
    }
}
