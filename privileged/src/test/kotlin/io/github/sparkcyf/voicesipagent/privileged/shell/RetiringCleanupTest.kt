package io.github.sparkcyf.voicesipagent.privileged.shell

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RetiringCleanupTest {
    @Test fun blockedNativeCleanupCannotPreventExitAndLateCompletionCannotExitAgain() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val exited = CountDownLatch(1); val cleaned = CountDownLatch(1)
        val exits = AtomicInteger(); val starts = AtomicInteger()
        val cleanup = RetiringCleanup(
            cleanup = { starts.incrementAndGet(); entered.countDown(); release.await(); cleaned.countDown() },
            exit = { exits.incrementAndGet(); exited.countDown() },
            timeoutMillis = 100,
        )
        cleanup.start(); cleanup.start()
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertTrue(exited.await(2, TimeUnit.SECONDS))
            assertEquals(1, exits.get())
            assertEquals(1L, cleaned.count)
        } finally { release.countDown() }
        assertTrue(cleaned.await(2, TimeUnit.SECONDS))
        assertEquals(1, starts.get())
        assertEquals(1, exits.get())
    }

    @Test fun successfulCleanupExitsImmediatelyWithoutWaitingForDeadline() {
        val exited = CountDownLatch(1)
        val order = mutableListOf<String>()
        RetiringCleanup(
            cleanup = { order += "closed" },
            exit = { order += "exited"; exited.countDown() },
            timeoutMillis = 10_000,
        ).start()
        assertTrue(exited.await(2, TimeUnit.SECONDS))
        assertEquals(listOf("closed", "exited"), order)
    }
}
