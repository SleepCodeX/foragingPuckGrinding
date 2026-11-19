package dev.sleepcodex.foragingpuckgrinding.mixin;

import java.util.regex.Pattern;
import net.johnceo.sparklingmutuals.safari.SafariCatch;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SafariCatch.Companion.class, remap = false)
abstract class SafariLootShareMixin {
    @Unique private static final Pattern huntingHelper$catcher = Pattern.compile(
            "\\bfrom\\s+(?:\\[[^]\\r\\n]+]\\s*)*([A-Za-z0-9_]{1,16})\\s+(?:catching|finding)\\s+",
            Pattern.CASE_INSENSITIVE);

    @Inject(method = "parse", at = @At("HEAD"), cancellable = true)
    private void huntingHelper$lootShare(String raw, CallbackInfoReturnable<SafariCatch> cir) {
        String line = SafariRules.INSTANCE.strip(raw);
        if (!line.startsWith("LOOT SHARE!")) return;
        var match = huntingHelper$catcher.matcher(line);
        if (!match.find()) return;
        // A player's username can itself be a species name. Use the creature after the verb.
        String target = line.substring(match.end());
        var species = SafariRoster.INSTANCE.findIn(target);
        if (species != null) cir.setReturnValue(new SafariCatch(species, false, match.group(1),
                target.toUpperCase(java.util.Locale.ROOT).contains("SPARKLING ")));
    }
}
