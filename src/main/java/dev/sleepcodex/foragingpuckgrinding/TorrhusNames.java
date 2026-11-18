package dev.sleepcodex.foragingpuckgrinding;

import java.util.regex.Pattern;

/** Matches location labels, never chat or arbitrary text containing a mob name. */
public final class TorrhusNames {
    private static final Pattern FORMATTING = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATION = Pattern.compile(
            "(?:Area:|Zone:|⏣)\\s*Torrhus (?:Canyon|Heights|Springs)(?:\\s|$)", Pattern.CASE_INSENSITIVE);

    private TorrhusNames() { }

    public static boolean isLocationLabel(String text) {
        return text != null && LOCATION.matcher(FORMATTING.matcher(text).replaceAll("")).find();
    }
}
