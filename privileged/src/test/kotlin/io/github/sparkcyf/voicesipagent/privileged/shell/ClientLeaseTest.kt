package io.github.sparkcyf.voicesipagent.privileged.shell

import android.os.IBinder
import android.os.RemoteException
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ClientLeaseTest {
    private class Peer {
        val binder = mock(IBinder::class.java)
        var alive = true
        lateinit var death: IBinder.DeathRecipient
        init {
            `when`(binder.isBinderAlive).thenAnswer { alive }
            doAnswer { death = it.getArgument(0); null }.`when`(binder).linkToDeath(any(), eq(0))
        }
        fun die() { alive = false; death.binderDied() }
    }

    @Test fun oldProcessQueuedBinderCallsCannotUseTheNewClientsLease() {
        val lease = ClientLease { fail("idle service should remain warm") }
        val old = Peer(); lease.attach(old.binder, 101); old.die()
        lease.attach(Peer().binder, 202)
        // Entry checks cover configuration/health calls; audio checks again after its lock wait.
        assertThrows(IllegalStateException::class.java) { lease.requireLive(101) }
        assertThrows(IllegalStateException::class.java) { lease.audio(101) { fail("old call accepted") } }
        lease.requireLive(202)
        lease.audio(202) { }
    }

    @Test fun idleDeathAllowsNewClientAndLateOldDeathCannotCloseItsAudio() {
        var retired = 0
        val lease = ClientLease { retired++ }
        val first = Peer(); val next = Peer()
        lease.attach(first.binder, 101)
        first.die()
        assertEquals(0, retired)
        lease.attach(next.binder, 101)
        lease.audio(101) { lease.resourcesHeld(true) }
        first.death.binderDied()
        assertEquals(0, retired)
        next.die()
        assertEquals(1, retired)
        assertThrows(IllegalStateException::class.java) { lease.attach(Peer().binder, 101) }
        assertThrows(IllegalStateException::class.java) { lease.audio(101) { fail("dead client opened audio") } }
    }

    @Test fun deathDuringBlockedOpenRetiresWithoutWaitingForAudioMonitor() {
        val retired = AtomicInteger()
        val lease = ClientLease { retired.incrementAndGet() }
        val peer = Peer(); lease.attach(peer.binder, 101)
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val opening = Thread {
            lease.audio(101) {
                entered.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                lease.resourcesHeld(false)
            }
        }.apply { start() }
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            peer.die()
            assertEquals(1, retired.get())
            assertThrows(IllegalStateException::class.java) { lease.attach(Peer().binder, 101) }
        } finally { release.countDown(); opening.join(2_000) }
        assertFalse(opening.isAlive)
        // Completion of the stale open cannot make the retiring process reusable.
        assertThrows(IllegalStateException::class.java) { lease.attach(Peer().binder, 101) }
    }

    @Test fun completedAudioCleanupAllowsWarmReuseButPendingResourcesDoNot() {
        var retired = 0
        val lease = ClientLease { retired++ }
        val peer = Peer(); lease.attach(peer.binder, 101)
        lease.audio(101) { lease.resourcesHeld(true) }
        lease.audio(101) { lease.resourcesHeld(false) }
        peer.die()
        assertEquals(0, retired)
        val next = Peer(); lease.attach(next.binder, 101)
        // A failed output restoration remains owned even after the socket/tap has closed.
        lease.audio(101) { lease.resourcesHeld(true) }
        next.die()
        assertEquals(1, retired)
    }

    @Test fun queuedOperationRechecksDeathAfterAcquiringTheServiceMonitor() {
        val lease = ClientLease { }
        val peer = Peer(); lease.attach(peer.binder, 101)
        lease.audio(101) {
            // Simulate losing the client while waiting behind an earlier audio Binder call.
            peer.die()
            assertFalse(lease.isLive)
            assertThrows(IllegalStateException::class.java) { lease.requireLive(101) }
        }
    }

    @Test fun detectsDeadPreviousClientBeforeItsQueuedDeathCallback() {
        val lease = ClientLease { fail("idle process should remain warm") }
        val previous = Peer(); lease.attach(previous.binder, 101)
        previous.alive = false
        lease.attach(Peer().binder, 101)
        previous.death.binderDied()
        lease.audio(101) { }
    }

    @Test fun cannotReplaceLiveClientOrAttachDeadClient() {
        val lease = ClientLease { fail("unexpected retirement") }
        val peer = Peer(); lease.attach(peer.binder, 101); lease.attach(peer.binder, 101)
        verify(peer.binder, times(1)).linkToDeath(any(), eq(0))
        assertThrows(IllegalStateException::class.java) { lease.attach(Peer().binder, 101) }
        peer.die()
        val dead = Peer().apply { alive = false }
        assertThrows(IllegalStateException::class.java) { lease.attach(dead.binder, 101) }
        lease.attach(Peer().binder, 101)
    }

    @Test fun failedDeathRegistrationDoesNotRetainAnUnmonitoredClient() {
        val lease = ClientLease { fail("unexpected retirement") }
        val failed = Peer()
        doThrow(RemoteException()).`when`(failed.binder).linkToDeath(any(), eq(0))
        assertThrows(RemoteException::class.java) { lease.attach(failed.binder, 101) }
        lease.attach(Peer().binder, 101)
    }

    @Test fun manualStopRetiresEvenIdleServiceExactlyOnce() {
        var retired = 0
        val lease = ClientLease { retired++ }
        val peer = Peer(); lease.attach(peer.binder, 101)
        lease.stop(); lease.stop(); peer.die()
        assertEquals(1, retired)
        assertThrows(IllegalStateException::class.java) { lease.attach(Peer().binder, 101) }
    }
}
