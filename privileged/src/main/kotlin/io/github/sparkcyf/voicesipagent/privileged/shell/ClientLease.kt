package io.github.sparkcyf.voicesipagent.privileged.shell

import android.os.IBinder

/** Keeps only an idle UserService warm. Audio work belongs to one live app process. */
internal class ClientLease(private val retire: () -> Unit) {
    private class Client(val binder: IBinder, val pid: Int, val death: IBinder.DeathRecipient)
    private val lock = Any()
    private var client: Client? = null
    private var audioOperations = 0
    private var resourcesHeld = false
    private var retiring = false

    fun attach(binder: IBinder, pid: Int) {
        require(pid > 0) { "invalid client process" }
        // A new process can arrive before Binder dispatches the old process's death callback.
        val previous = synchronized(lock) { client }
        if (previous != null && !previous.binder.isBinderAlive) lost(previous.binder)
        synchronized(lock) {
            check(!retiring) { "privileged service is retiring" }
            if (client?.binder === binder) {
                check(client?.pid == pid) { "client process changed" }
                return
            }
            check(client == null) { "another client is still attached" }
            check(binder.isBinderAlive) { "client is already dead" }
            val death = IBinder.DeathRecipient { lost(binder) }
            val attached = Client(binder, pid, death)
            client = attached
            try {
                binder.linkToDeath(death, 0)
            } catch (failure: Throwable) {
                if (client === attached) client = null
                throw failure
            }
        }
    }

    private fun lost(binder: IBinder) {
        val mustRetire = synchronized(lock) {
            val current = client ?: return
            if (current.binder !== binder) return
            client = null
            runCatching { binder.unlinkToDeath(current.death, 0) }
            if (!retiring && (audioOperations != 0 || resourcesHeld)) {
                retiring = true
                true
            } else false
        }
        if (mustRetire) retire()
    }

    fun <T> audio(pid: Int, block: () -> T): T {
        synchronized(lock) {
            requireLive(pid)
            audioOperations++
        }
        try {
            return block()
        } finally {
            synchronized(lock) { audioOperations-- }
        }
    }

    val isLive: Boolean get() = synchronized(lock) { !retiring && client?.binder?.isBinderAlive == true }

    /** Recheck after waiting for the service monitor; queued stale operations must not open/mute. */
    fun requireLive(pid: Int) = synchronized(lock) {
        check(isLive && client?.pid == pid) { "no matching live client process" }
    }

    /** Called while an audio operation is counted, including unsuccessful mute restoration. */
    fun resourcesHeld(held: Boolean) = synchronized(lock) { resourcesHeld = held }

    fun stop() {
        val mustRetire = synchronized(lock) {
            if (retiring) return
            retiring = true
            client?.let { runCatching { it.binder.unlinkToDeath(it.death, 0) } }
            client = null
            true
        }
        if (mustRetire) retire()
    }
}
