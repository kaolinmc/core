package com.example

import com.kaolinmc.mixin.api.InjectCode
import com.kaolinmc.mixin.api.Mixin
import com.kaolinmc.mixin.api.MixinFlow
import net.minecraft.client.Minecraft
import net.minecraft.client.main.Main
import net.minecraft.server.Bootstrap

//@Mixin(SkyRenderer::class)
//class SkyChanger {
//    @InjectCode(
//        "renderSunriseAndSunset",
//        locals = [4]
//    )
//    fun inject(
//        color: Captured<Int>
//    ) {
//        color.set(0b000000000011111111110000000000)
//    }
//
////    @InjectCode(
////        "renderSun",
////        point = Select(
////            InjectionBoundary.TAIL
////        ),
////        locals = [6, 7, 8]
////    )
////    fun addTriangles(
////        vertexConsumer: Captured<VertexConsumer>,
////        color: Captured<Int>,
////        mat4: Captured<Matrix4f>,
////    ) {
//////        val vertexConsumer by vertexConsumer
//////        val mat4 by mat4
////
////        vertexConsumer.get().addVertex(mat4.get(), -40.0F, -200.0F, 20.0F).setUv(0.5f, 0.5f).setColor(color.get())
////        vertexConsumer.get().addVertex(mat4.get(), 40.0F, 200.0F, 20.0F).setUv(0.0f, 0.5f).setColor(color.get())
////    }
//}

@Mixin(Main::class)
object TestMixin {
    @InjectCode("main")
    @JvmStatic
    fun injector(flow: MixinFlow) {
        println("This is a mixin")
    }
}

@Mixin(Minecraft::class)
abstract class IntoMinecraft {
    @InjectCode()
    fun runTick(flow: MixinFlow) {
        Bootstrap.realStdoutPrintln("THIS IS GONNA PRINT A BUNCH")
    }
}

//@Mixin(Camera::class)
//object CameraMixin : Camera() {
//    @InjectCode
//    override fun tick() {
//        super.move(0.1f, 0.1f, 0.1f)
//    }
//}
