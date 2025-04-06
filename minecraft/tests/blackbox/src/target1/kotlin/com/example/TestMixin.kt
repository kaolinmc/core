package com.example

import com.mojang.authlib.GameProfile
import dev.extframework.mixin.api.InjectCode
import dev.extframework.mixin.api.InjectionType
import dev.extframework.mixin.api.Invoke
import dev.extframework.mixin.api.Mixin
import dev.extframework.mixin.api.MixinFlow
import dev.extframework.mixin.api.Select
import dev.extframework.mixin.api.Stack
import net.minecraft.client.main.Main
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

@Mixin(Main::class)
object TestMixin {
    @InjectCode("main")
    @JvmStatic
    fun injector(flow: MixinFlow) {
        println("This is a mixin")
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