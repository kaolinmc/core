package com.kaolinmc.minecraft.task

import com.durganmcbroom.resources.KtorInstance
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.ConnectException

public abstract class ReloadExtension : DefaultTask() {
    @TaskAction
    public open fun doReload() {
        runBlocking {
            try {
                val response = KtorInstance.client.post("http://localhost:7654/reload")

                if (response.status == HttpStatusCode.OK) {
                    logger.info("Successfully reloaded!")
                } else {
                    logger.error("Failed to reload! ${response.bodyAsText()}")
                }
            } catch (_: ConnectException) {
                throw Exception("The dev server is not launched! Make sure you launch before you try to reload.")
            }
        }
    }
}