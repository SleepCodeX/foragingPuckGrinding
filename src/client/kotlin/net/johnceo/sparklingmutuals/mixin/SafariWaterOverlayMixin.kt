package net.johnceo.sparklingmutuals.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.mojang.blaze3d.vertex.PoseStack
import net.johnceo.sparklingmutuals.safari.ClearWater
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.ScreenEffectRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At

@Mixin(ScreenEffectRenderer::class)
class SafariWaterOverlayMixin {
    @WrapOperation(method=["submit"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V")])
    private fun noWaterOverlay(client: Minecraft, pose: PoseStack, buffers: SubmitNodeCollector, original: Operation<Void>) {
        if (!ClearWater.active || !net.johnceo.sparklingmutuals.safari.SafariAssist.visualsActive()) original.call(client,pose,buffers)
    }
}
