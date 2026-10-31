package io.github.sparkcyf.voicesipagent.privileged.shell

import java.util.concurrent.atomic.AtomicBoolean

/** A blocked audio/HAL call must not leave an ownerless privileged process holding audio. */
internal class RetiringCleanup(
    private val cleanup: () -> Unit,
    private val exit: () -> Unit,
    private val timeoutMillis: Long = 2_000,
) {
    private val started = AtomicBoolean()
    private val exited = AtomicBoolean()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val deadline = Thread({
            try { Thread.sleep(timeoutMillis); finish() } catch (_: InterruptedException) { }
        }, "privileged-exit-deadline").apply { isDaemon = true }
        deadline.start()
        Thread({
            try { cleanup() } finally { finish(); deadline.interrupt() }
        }, "privileged-client-cleanup").apply { isDaemon = true; start() }
    }

    private fun finish() { if (exited.compareAndSet(false, true)) exit() }
}
