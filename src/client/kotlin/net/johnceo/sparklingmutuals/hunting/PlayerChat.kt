// Hunting Helper: CritterMod's player-line rules, with the channel prefix matched case-insensitively.
package net.johnceo.sparklingmutuals.hunting

/**
 * Lines a player typed, as opposed to lines the game sent. Anything a player can type, a player can quote, so
 * watchers that react to game wording must skip these. Channel prefixes ("Party >", "guild >", "CO-OP >") are
 * matched case-insensitively, the CritterMod PR #5 fix; `[NPC]` and `[MOB]` lines are the game talking.
 */
object PlayerChat {
    private val said = Regex(
        "^(?i:Party|Guild|Officer|Co-op|Coop|Team|Friend)\\s*>.*" +
            "|^(?i:From|To) .*" +
            "|^(?:\\[(?!NPC]|MOB])[^]]+]\\s*)?\\w{1,16}: .*")
    /** `Party > [VIP] Someone: text`, any capitalisation of "Party"; groups are the sender and the text. */
    private val party = Regex("^(?i:Party)\\s*>\\s*(?:\\[[^]]+]\\s*)*(\\w{1,16}): (.+)$")
    private val formatting = Regex("§.")
    private val compacted = Regex("\\s*(?:\\(\\s*[x×]?\\s*\\d+\\s*\\)|\\[\\s*[x×]?\\s*\\d+\\s*])$")

    fun said(line: String) = said.matches(line)

    /** The sender and text of a party line, or null. */
    fun party(line: String): Pair<String, String>? =
        party.matchEntire(line)?.let { it.groupValues[1] to it.groupValues[2] }

    /** Strips colour codes and the duplicate counters chat-compacting mods append (" (3)", " [x3]"). */
    fun clean(raw: String) = net.johnceo.sparklingmutuals.safari.SafariRules.strip(raw.replace(formatting, ""))
        .replace(compacted, "").trim()

    private val separators = Regex("\\r?\\n|\\\\n")
    /** Hypixel sends some banners as one multi-line component. */
    fun lines(raw: String) = raw.split(separators).map(::clean).filter(String::isNotEmpty)
}
