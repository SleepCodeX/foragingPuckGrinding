package net.johnceo.sparklingmutuals.compat

import java.util.concurrent.CompletableFuture
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs

/** Uses the native dialog library shipped by each Minecraft target. */
object NativeCsvDialog {
    fun choose(): CompletableFuture<String?> {
        val directory = System.getProperty("user.home") + "/Downloads/"
        return CompletableFuture.supplyAsync {
            MemoryStack.stackPush().use { stack ->
                val filters = stack.mallocPointer(1).put(0, stack.UTF8("*.csv"))
                TinyFileDialogs.tinyfd_openFileDialog("Import Warp List CSV (replaces current list)",
                    directory, filters, "CSV files", false)
            }
        }
    }
}
