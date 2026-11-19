package net.johnceo.sparklingmutuals.compat

import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.BlendFunction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorldRenderPipelineTest {
    @Test fun `line and filled guides each declare one blended color attachment`() {
        for (filled in listOf(false, true)) {
            val pipeline = WorldRenderCompat.renderType("test_color_$filled", filled = filled).pipeline()
            val color = pipeline.colorTargetState!!
            assertEquals(BlendFunction.TRANSLUCENT, color.blendFunction().orElse(null))
        }
    }

    @Test fun `occluded guides use the target depth convention without writing depth`() {
        for (filled in listOf(false, true)) {
            val pipeline = WorldRenderCompat.renderType("test_depth_$filled", filled = filled, depth = WorldDepthMode.OCCLUDED).pipeline()
            assertEquals(DepthStencilState.DEFAULT.depthTest(), pipeline.depthStencilState!!.depthTest())
            assertFalse(pipeline.depthStencilState!!.writeDepth())
        }
    }
}
