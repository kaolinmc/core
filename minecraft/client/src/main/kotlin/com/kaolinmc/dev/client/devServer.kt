package com.kaolinmc.dev.client

import com.kaolinmc.minecraft.client.api.MinecraftExtensionInitializer
import io.ktor.http.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

internal fun devServer(loader: ClientExtensionLoader) {
    embeddedServer(CIO, port = 7654) {
        routing {
            get("/") {
                call.respondText("Hey, you found the Development Server for the Kaolin Gradle plugin. Type `./gradlew reload` or `gradle reload` in your terminal to reload your extension.")
            }
            post("/reload") {
                try {
                    // FIXME This requires more research, but it appears that as long as we are in a coroutine context
                    //   some objects are not able to be garbage collected. Im not sure why, but this is one solution.
                    runBlocking {
                        loader.unload(loader.devExtension)
                    }

                    System.gc()

                    val extensions = loader.load(listOf(loader.devExtension))
                    loader.environment[MinecraftExtensionInitializer].initialize(
                        extensions.filter { it.descriptor == loader.devExtension }
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(wait = false)
}