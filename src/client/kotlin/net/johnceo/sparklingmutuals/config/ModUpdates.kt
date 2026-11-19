package net.johnceo.sparklingmutuals.config

import net.fabricmc.loader.api.FabricLoader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

/** Only public release metadata; no credentials, player identifiers, downloads or game-thread HTTP. */
object ModUpdates {
    // Hunting Helper: checks Hunting Helper's own releases (SleepCodeX/foragingPuckGrinding) against the installed
    // Hunting Helper version, so players are never sent to upstream Sparkling Mutuals builds.
    const val REPOSITORY="https://github.com/SleepCodeX/foragingPuckGrinding"
    val installed: String=FabricLoader.getInstance().getModContainer("foragingpuckgrinding").map { it.metadata.version.friendlyString }.orElse("dev")
    val minecraftVersion: String=FabricLoader.getInstance().getModContainer("minecraft").map { it.metadata.version.friendlyString }.orElse("")
    private val checker = ReleaseUpdateChecker(installed, minecraftVersion, fetch = {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/SleepCodeX/foragingPuckGrinding/releases?per_page=100"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Hunting-Helper-Update-Check")
            .header("Cache-Control", "no-cache")
            .timeout(Duration.ofSeconds(10)).GET().build()
        val response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
            .send(request, HttpResponse.BodyHandlers.ofString())
        kotlin.check(response.statusCode() == 200)
        response.body()
    }, execute = { task -> CompletableFuture.runAsync { task() }; Unit },
        asset = { version, minecraft -> "hunting-helper-$version+mc$minecraft.jar" })
    val release get() = checker.state.release
    val label get() = checker.state.label
    fun check(force: Boolean = false) = checker.check(force)
    fun compareVersions(a: String, b: String) = ReleaseUpdateChecker.compareVersions(a, b)
}
