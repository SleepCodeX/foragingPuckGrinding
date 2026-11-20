// Hunting Helper: quest-item filter for Sparkling Mutuals' floor-drop highlights (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

/**
 * "Quest items only" and "Hide collected quest items" for floor-drop highlights.
 *
 * Quest items are the floor drops a Safari objective consumes, with how many one run needs (Sparkling Mutuals'
 * objectives and CritterMod's encounter notes): one each of the Lime, Purple and Orange Gem (Gemzie chamber),
 * three each of Wriggleworm, Yogi Berry and Bag of Seeds (nine feeder foods), four Soothing Incense (four
 * Doomspiral candles), Shining Coins (Gimmiegold) and one Icebreaker (Wumpa). XP, essence and shard drops are
 * not quest items. Shining Coins have no fixed amount: every coin floor of the run is Gimmiegold demand
 * ([net.johnceo.sparklingmutuals.safari.completion.GimmiegoldSparklingCheck]), so they are never hidden as collected. "Collected" is the run ledger's own count: starting grants plus floor pickups
 * ([net.johnceo.sparklingmutuals.safari.resources.SafariResources.discoveredCount]).
 *
 * Exception: an item an active Hunter trade asks for (found by you or shared by the party) stays highlighted.
 */
object QuestDropFilter {
    private val caps: Map<FloorDropType, Int> = mapOf(
        FloorDropType.LIME to 1, FloorDropType.PURPLE to 1, FloorDropType.ORANGE to 1,
        FloorDropType.WORM to 3, FloorDropType.BERRY to 3, FloorDropType.SEEDS to 3,
        FloorDropType.INCENSE to 4, FloorDropType.ICEBREAKER to 1)

    fun quest(type: FloorDropType) = type == FloorDropType.COIN || type in caps

    /** Whether a trade item name ("Lime Gem", "a Yogi Berry", "Bags of Seeds") is this floor drop. */
    fun matches(type: FloorDropType, item: String): Boolean {
        val name = item.trim().removePrefix("an ").removePrefix("a ").removePrefix("the ").trim().trimEnd('.', '!', '?', '…')
        return quest(type) && (name.equals(type.label, true) || name.equals(type.label + "s", true) ||
            type == FloorDropType.SEEDS && name.equals("Bags of Seeds", true))
    }

    fun wantedByTrade(type: FloorDropType, tradeItems: Collection<String>) = tradeItems.any { matches(type, it) }

    /**
     * The filter decision for one drop: true forces a highlight (a trade wants it), false hides it, null leaves the
     * decision to Sparkling Mutuals' own floor-drop rules.
     */
    fun decide(type: FloorDropType, questOnly: Boolean, hideCollected: Boolean, collected: Int, tradeItems: Collection<String>): Boolean? {
        if (wantedByTrade(type, tradeItems)) return true
        if (questOnly && !quest(type)) return false
        val cap = caps[type]
        if (hideCollected && cap != null && collected >= cap) return false
        return null
    }

    /** [decide] with the live settings, run ledger and tracked trades. */
    fun decide(type: FloorDropType): Boolean? {
        val trades = if (HuntingOutput.trades()) TraderWatch.found().map { it.item() } else emptyList()
        if (!HuntingConfig.questItemsOnly && !HuntingConfig.hideCollectedQuestItems && trades.isEmpty()) return null
        val collected = SafariTracking.ledger.current?.resources?.discoveredCount(type) ?: 0
        return decide(type, HuntingConfig.questItemsOnly, HuntingConfig.hideCollectedQuestItems, collected, trades)
    }

    /** Writes the two settings; true when they changed. */
    fun apply(questOnly: Boolean, hideCollected: Boolean): Boolean {
        if (HuntingConfig.questItemsOnly == questOnly && HuntingConfig.hideCollectedQuestItems == hideCollected) return false
        HuntingConfig.questItemsOnly = questOnly
        HuntingConfig.hideCollectedQuestItems = hideCollected
        HuntingConfig.save()
        return true
    }
}
