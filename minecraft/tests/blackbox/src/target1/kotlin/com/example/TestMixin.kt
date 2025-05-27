package com.example

import com.mojang.authlib.GameProfile
import dev.extframework.mixin.api.*
import net.minecraft.client.Camera
import net.minecraft.client.main.Main
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.SkyRenderer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

@Mixin(SkyRenderer::class)
class SkyChanger {
    @InjectCode(
        "renderSunriseAndSunset",
        locals = [4]
    )
    fun inject(
        color: Captured<Int>
    ) {
        color.set(0b000000000011111111110000000000)
    }

//    @InjectCode(
//        "renderSun",
//        point = Select(
//            InjectionBoundary.TAIL
//        ),
//        locals = [6, 7, 8]
//    )
//    fun addTriangles(
//        vertexConsumer: Captured<VertexConsumer>,
//        color: Captured<Int>,
//        mat4: Captured<Matrix4f>,
//    ) {
////        val vertexConsumer by vertexConsumer
////        val mat4 by mat4
//
//        vertexConsumer.get().addVertex(mat4.get(), -40.0F, -200.0F, 20.0F).setUv(0.5f, 0.5f).setColor(color.get())
//        vertexConsumer.get().addVertex(mat4.get(), 40.0F, 200.0F, 20.0F).setUv(0.0f, 0.5f).setColor(color.get())
//    }
}

@Mixin(Main::class)
object TestMixin {
    @InjectCode("main")
    @JvmStatic
    fun injector(flow: MixinFlow) {
        println("This is a mixin")
    }
}

@Mixin(Camera::class)
object CameraMixin : Camera() {
    @InjectCode
    override fun tick() {
        super.move(0.1f, 0.1f, 0.1f)
    }
}

@Mixin(AbstractClientPlayer::class)
abstract class FovModifierMixin(
    level: Level,
    pos: BlockPos,
    f: Float,
    profile: GameProfile
) : Player(
    level, pos, f, profile
) {
    @InjectCode(
        "getFieldOfViewModifier",
        point = Select(
            invoke = Invoke(
                AbstractClientPlayer::class,
                "getAttributeValue(Lnet/minecraft/core/Holder;)D"
            )
        ),
        type = InjectionType.AFTER
    )
    fun walkModifier(
        stack: Stack,
    ) {
        stack.replaceLast(0.1)
    }
}