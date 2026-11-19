package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

/** Independent of objective completion; global ESP visibility controls still apply. */
class FloorDropToggleSettings {
    // Hunting Helper: quest-item filter (hunting/QuestDropFilter.kt). Items an active Hunter trade wants stay highlighted.
    @JvmField @ConfigOption(name = "Quest items only", desc = "Highlight only gems, bird food, incense, coins and the Icebreaker.")
    @ConfigEditorBoolean var questOnly = net.johnceo.sparklingmutuals.hunting.HuntingConfig.questItemsOnly
    @JvmField @ConfigOption(name = "Hide collected quest items", desc = "Stop once this run has collected what its objectives need (one of each gem).")
    @ConfigEditorBoolean var hideCollected = net.johnceo.sparklingmutuals.hunting.HuntingConfig.hideCollectedQuestItems
    @JvmField @ConfigOption(name = "Lime Gem", desc = "")
    @ConfigEditorBoolean var lime = SafariEspConfig.floorAlwaysOn(FloorDropType.LIME)
    @JvmField @ConfigOption(name = "Purple Gem", desc = "")
    @ConfigEditorBoolean var purple = SafariEspConfig.floorAlwaysOn(FloorDropType.PURPLE)
    @JvmField @ConfigOption(name = "Orange Gem", desc = "")
    @ConfigEditorBoolean var orange = SafariEspConfig.floorAlwaysOn(FloorDropType.ORANGE)
    @JvmField @ConfigOption(name = "Wriggleworm", desc = "")
    @ConfigEditorBoolean var worm = SafariEspConfig.floorAlwaysOn(FloorDropType.WORM)
    @JvmField @ConfigOption(name = "Yogi Berry", desc = "")
    @ConfigEditorBoolean var berry = SafariEspConfig.floorAlwaysOn(FloorDropType.BERRY)
    @JvmField @ConfigOption(name = "Bag of Seeds", desc = "")
    @ConfigEditorBoolean var seeds = SafariEspConfig.floorAlwaysOn(FloorDropType.SEEDS)
    @JvmField @ConfigOption(name = "Soothing Incense", desc = "")
    @ConfigEditorBoolean var incense = SafariEspConfig.floorAlwaysOn(FloorDropType.INCENSE)
    @JvmField @ConfigOption(name = "Shining Coin", desc = "")
    @ConfigEditorBoolean var coin = SafariEspConfig.floorAlwaysOn(FloorDropType.COIN)
    @JvmField @ConfigOption(name = "Icebreaker", desc = "")
    @ConfigEditorBoolean var icebreaker = SafariEspConfig.floorAlwaysOn(FloorDropType.ICEBREAKER)
    @JvmField @ConfigOption(name = "XP / Essence / Shard", desc = "")
    @ConfigEditorBoolean var rewards = SafariEspConfig.floorAlwaysOn(FloorDropType.GENERIC)

    fun apply(): Boolean {
        var changed = net.johnceo.sparklingmutuals.hunting.QuestDropFilter.apply(questOnly, hideCollected) // Hunting Helper
        javaClass.fields.forEach { field ->
            val label = field.getAnnotation(ConfigOption::class.java)?.name ?: return@forEach
            val type = FloorDropType.entries.singleOrNull { it.label == label } ?: return@forEach // Hunting Helper: was single
            val enabled = field.getBoolean(this)
            if (SafariEspConfig.floorAlwaysOnToggles.put(type, enabled) != enabled) changed = true
        }
        return changed
    }
}
