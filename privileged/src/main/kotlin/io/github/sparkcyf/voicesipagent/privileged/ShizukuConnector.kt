package io.github.sparkcyf.voicesipagent.privileged

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import io.github.sparkcyf.voicesipagent.privileged.shell.PrivilegedService
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList

/**
 * App-side lifecycle for the Shizuku UserService: waits for the Shizuku binder (it appears ~15 s
 * after boot via wireless debugging), checks permission, binds [PrivilegedService] and reports
 * every transition so the app can re-run setup when the service (re)connects.
 */
class ShizukuConnector(context: Context, private val log: (String) -> Unit) {
    enum class State { STOPPED, SHIZUKU_NOT_RUNNING, PERMISSION_REQUIRED, PERMISSION_DENIED, BINDING, CONNECTED, ERROR }

    fun interface Listener {
        fun onChanged(state: State, service: IPrivilegedService?)
    }

    private val app = context.applicationContext
    private val listeners = CopyOnWriteArrayList<Listener>()
    @Volatile var state = State.SHIZUKU_NOT_RUNNING; private set
    @Volatile var service: IPrivilegedService? = null; private set
    @Volatile var detail: String = ""; private set
    @Volatile private var started = false
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var connection: ServiceConnection? = null
    private var bindTimeout: Runnable? = null
    private var automaticRetries = 0
    private val client = Binder()
    private fun cancelTimeout() { bindTimeout?.let { main.removeCallbacks(it) }; bindTimeout = null }
    /** Retry only our pending request. Never replace a connected helper or shared server. */
    @Synchronized fun retryBinding() {
        if (!started || state == State.CONNECTED) return
        cancelTimeout()
        generation++
        connection?.let { old ->
            runCatching { Shizuku.unbindUserService(args, old, false) }
            runCatching { Shizuku.unbindUserService(args, old, true) }
        }
        connection = null
        set(State.ERROR, null, "retrying connection")
        onBinder()
    }

    private val args = Shizuku.UserServiceArgs(ComponentName(app.packageName, PrivilegedService::class.java.name))
        // The service independently watches client death and retains only an idle instance.
        .daemon(true)
        .processNameSuffix("shell")
        .debuggable(false)
        .tag("vsa-privileged")
        // Restart the shell process whenever a new APK is installed so it never runs stale code.
        .version((app.packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime / 1000 % Int.MAX_VALUE).toInt())

    private fun connection(epoch: Long) = object : ServiceConnection {
        private var clientRegistrationRetries = 0
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            synchronized(this@ShizukuConnector) {
                if (!started || generation != epoch) return
                if (binder == null || !binder.pingBinder()) {
                    set(State.ERROR, null, "UserService binder dead")
                    return
                }
                val svc = IPrivilegedService.Stub.asInterface(binder)
                val uid = try { svc.uid } catch (e: Exception) { -1 }
                if (uid != 2000 && uid != 0) {
                    set(State.ERROR, null, "unexpected privileged uid $uid")
                    return
                }
                try {
                    svc.attachClient(client)
                } catch (failure: Exception) {
                    // A warm helper can observe this new process before the old Binder's death
                    // callback. Retry this registration briefly, without destroying the helper.
                    if (clientRegistrationRetries++ < CLIENT_REGISTRATION_RETRIES) {
                        set(State.BINDING, null, "waiting for previous client cleanup")
                        main.postDelayed({
                            synchronized(this@ShizukuConnector) {
                                if (started && generation == epoch && state == State.BINDING)
                                    onServiceConnected(name, binder)
                            }
                        }, 100)
                    } else {
                        set(State.ERROR, null, "binding_timeout: client lifetime registration failed: ${failure.javaClass.simpleName}")
                    }
                    return
                }
                clientRegistrationRetries = 0
                automaticRetries = 0
                set(State.CONNECTED, svc, "uid $uid")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            synchronized(this@ShizukuConnector) {
                if (!started || generation != epoch) return
                connection = null
                set(State.ERROR, null, "UserService disconnected")
                // Shizuku clears its dead connection after this callback returns. Rebinding
                // inside the callback would reuse the connection it is about to discard.
                main.post {
                    synchronized(this@ShizukuConnector) {
                        if (started && generation == epoch) onBinder()
                    }
                }
            }
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { onBinder() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        synchronized(this) { if (started) set(State.SHIZUKU_NOT_RUNNING, null, "Shizuku binder died") }
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { code, result ->
        synchronized(this) {
            if (!started || code != PERMISSION_REQUEST) return@OnRequestPermissionResultListener
            if (result == PackageManager.PERMISSION_GRANTED) bind() else set(State.PERMISSION_DENIED, null, "denied in Shizuku")
        }
    }

    @Synchronized
    fun start() {
        if (started) return
        started = true
        automaticRetries = 0
        generation++
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
    }

    /** Removes this app's listeners and helper; the shared Shizuku server is left running. */
    @Synchronized
    fun stop() {
        if (!started && state == State.STOPPED) return
        started = false
        cancelTimeout()
        generation++
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        runCatching { service?.closeCallAudio() }.onFailure { log("shizuku: audio cleanup failed: ${it.message}") }
        val bound = connection
        connection = null
        if (bound != null) {
            // Clear the client connection synchronously before killing the helper. Otherwise a
            // rapid restart can reuse the old connection while its death callback is still queued.
            runCatching { Shizuku.unbindUserService(args, bound, false) }
                .onFailure { log("shizuku: connection removal failed: ${it.message}") }
            runCatching { Shizuku.unbindUserService(args, bound, true) }
                .onFailure { log("shizuku: helper removal failed: ${it.message}") }
        }
        set(State.STOPPED, null, "takeover off")
    }

    fun addListener(l: Listener) {
        listeners.addIfAbsent(l)
        l.onChanged(state, service)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    /** Asks Shizuku for permission (shows Shizuku's dialog). Call from the UI. */
    @Synchronized
    fun requestPermission() {
        try {
            if (!Shizuku.pingBinder()) return set(State.SHIZUKU_NOT_RUNNING, null, "start Shizuku first")
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { if (started) bind() }
            else Shizuku.requestPermission(PERMISSION_REQUEST)
        } catch (e: RuntimeException) {
            set(State.ERROR, null, "permission request failed: ${e.message}")
        }
    }

    @Synchronized
    private fun onBinder() {
        if (!started) return
        try {
            if (!Shizuku.pingBinder()) return set(State.SHIZUKU_NOT_RUNNING, null, "start Shizuku first")
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 13) return set(State.ERROR, null, "Shizuku v13+ required")
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) bind()
            else set(State.PERMISSION_REQUIRED, null, "grant access in Shizuku")
        } catch (e: RuntimeException) {
            set(State.ERROR, null, "Shizuku check failed: ${e.message}")
        }
    }

    private fun bind() {
        if (!started || state == State.BINDING) return
        if (state == State.CONNECTED && service?.asBinder()?.pingBinder() == true) return
        set(State.BINDING, null, "binding UserService")
        try {
            val bound = connection(++generation)
            connection = bound
            Shizuku.bindUserService(args, bound)
            val epoch = generation
            val timeout = Runnable {
                synchronized(this) {
                    if (!started || generation != epoch || state != State.BINDING) return@synchronized
                    if (automaticRetries++ < 1) retryBinding()
                    else set(State.ERROR, null, "binding_timeout: open Voice SIP Assistant for compatibility recovery")
                }
            }
            bindTimeout = timeout
            main.postDelayed(timeout, 15_000)
        } catch (e: RuntimeException) {
            set(State.ERROR, null, "bindUserService failed: ${e.message}")
        }
    }

    private fun set(s: State, svc: IPrivilegedService?, why: String) {
        if (s != State.BINDING) cancelTimeout()
        state = s
        service = svc
        detail = why
        log("shizuku: $s ($why)")
        listeners.forEach { it.onChanged(s, svc) }
    }

    companion object {
        private const val PERMISSION_REQUEST = 4101
        private const val CLIENT_REGISTRATION_RETRIES = 5
    }
}
