package net.johnceo.sparklingmutuals.compat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks
import org.lwjgl.glfw.GLFW

val Minecraft.smScreen get() = gui.screen()
val Minecraft.smOverlay get() = gui.overlay()
fun Minecraft.smSetScreen(screen: Screen?) = gui.setScreen(screen)
val Options.smHideGui get() = Minecraft.getInstance().gui.hud.isHidden
val GameRenderer.smCamera get() = mainCamera()
fun Gui.smSetTitle(title: Component) = hud.setTitle(title)
fun Gui.smSetSubtitle(title: Component) = hud.setSubtitle(title)
fun Gui.smSetTimes(fadeIn: Int, stay: Int, fadeOut: Int) = hud.setTimes(fadeIn, stay, fadeOut)
val redCandle get() = Blocks.DYED_CANDLE.red()

fun physicalLeftMouseDown(client: Minecraft): Boolean {
    return GLFW.glfwGetMouseButton(client.window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
}

fun shiftDown(client: Minecraft): Boolean {
    return InputConstants.isKeyDown(client.window, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(client.window, InputConstants.KEY_RSHIFT)
}
