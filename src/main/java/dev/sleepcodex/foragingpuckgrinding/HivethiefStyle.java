package dev.sleepcodex.foragingpuckgrinding;

enum HivethiefStyle {
    FROST("Frost", "frost"), AMBER("Amber", "amber"), GRAPHITE("Graphite", "graphite");
    final String label, texture;
    HivethiefStyle(String label, String texture) { this.label = label; this.texture = texture; }
    static HivethiefStyle parse(String stored) {
        for (var style : values()) if (style.name().equalsIgnoreCase(stored)) return style;
        return FROST;
    }
}
