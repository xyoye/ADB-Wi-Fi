package dev.polek.adbwifi.commandexecutor

import dev.polek.adbwifi.ERROR_TAG
import dev.polek.adbwifi.LOG
import dev.polek.adbwifi.utils.output
import kotlinx.coroutines.delay
import java.io.IOException

class RuntimeCommandExecutor : CommandExecutor {

    private val runtime = Runtime.getRuntime()

    override fun exec(command: String, vararg envp: String): Sequence<String> {
        log { "exec> $command ${envp.joinToString()}" }

        val process = runtime.exec(command, envp)
        val stdInput = process.inputStream.bufferedReader().lineSequence()
        val stdError = process.errorStream.bufferedReader().lineSequence().map { "$ERROR_TAG$it" }
        return stdInput + stdError
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun execAsync(command: String, vararg envp: String): String {
        log { "execAsync> $command ${envp.joinToString()}" }

        val process = runtime.exec(command, envp)
        while (process.isAlive) {
            delay(500L)
        }
        return process.output()
    }

    override fun testExec(command: String, vararg envp: String): Boolean {
        log { "testExec> $command ${envp.joinToString()}" }

        val process = try {
            runtime.exec(command, envp)
        } catch (e: IOException) {
            LOG.debug(e)
            return false
        }
        process.waitFor()
        return process.exitValue() == 0
    }

    private companion object {

        private const val DEBUG_ENABLED = false

        private inline fun log(lazyMessage: () -> String) {
            if (DEBUG_ENABLED) {
                LOG.warn(lazyMessage())
            }
        }
    }
}
