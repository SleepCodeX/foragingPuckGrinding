package net.johnceo.sparklingmutuals.safari

data class SafariCatch(val critter: SafariCritter, val personal: Boolean = true, val catcherName: String? = null, val sparkling: Boolean = false) {
    companion object {
        private val catcher = Regex("\\bfrom\\s+(?:\\[[^]]+head]\\s*)?(\\w{1,16})\\s+(?:catching|finding)\\b")
        // Hunting Helper: loot shares from ranked players ("from [MVP+] Name catching ...") and from players whose
        // username is itself a species name. Read the catcher after any rank prefixes and the creature after the verb.
        private val lootShareCatcher = Regex(
            "\\bfrom\\s+(?:\\[[^]\\r\\n]+]\\s*)*([A-Za-z0-9_]{1,16})\\s+(?:catching|finding)\\s+", RegexOption.IGNORE_CASE)
        fun parse(raw: String): SafariCatch? {
            val text = SafariRules.strip(raw)
            if (text.startsWith("LOOT SHARE!")) lootShareCatcher.find(text)?.let { match -> // Hunting Helper: see above
                val target = text.substring(match.range.last + 1)
                SafariRoster.findIn(target)?.let { species ->
                    return SafariCatch(species, false, match.groupValues[1], target.uppercase().contains("SPARKLING "))
                }
            }
            val own = text.startsWith("CAPTURE!")
            if (!own && !text.startsWith("LOOT SHARE!")) return null
            val species = SafariRoster.findIn(text) ?: return null
            val personal = own && Regex("^CAPTURE! You (?:caught|found)\\b").containsMatchIn(text)
            val from = catcher.find(text)?.groupValues?.get(1)
            return if (personal || !own && from != null) SafariCatch(species, personal, from, Regex("\\bSPARKLING\\s+${Regex.escape(species.name)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) else null
        }
    }
}
