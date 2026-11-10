package io.github.sparkcyf.voicesipagent.privileged.shell

import android.os.Build
import android.os.Process
import java.util.concurrent.TimeUnit

/** Verified fuxi receiver only. A pipe lease restores the amp even when this helper is killed. */
internal class ReceiverAmpMute {
    private var worker: java.lang.Process? = null
    var requested = false; private set
    var lastMuteResult = ""; private set
    var lastRestoreResult = ""; private set
    val ownsOutputChange get() = worker != null
    val restorationPending get() = worker != null && lastRestoreResult.startsWith("failed:")

    fun beginCall() {
        check(worker == null) { "Receiver restoration is still pending" }
        requested = false
        lastMuteResult = ""
        lastRestoreResult = ""
    }

    fun observedMute(): String = when {
        worker?.isAlive == true -> if (ShellEnv.run("/system/bin/tinymix", AMP, timeoutSec = 2).out.trim() == "$AMP: Off") "applied" else "not_applied"
        requested -> "not_requested"
        else -> "not_requested"
    }

    fun mute(): String {
        requested = true
        if (worker?.isAlive == true) return lastMuteResult
        check(worker == null) { "Previous receiver lease needs restoration" }
        check(supported) { "Receiver mute requires fuxi and root Shizuku" }
        val p = ProcessBuilder("/system/bin/sh", "-c", SCRIPT).redirectErrorStream(true).start()
        worker = p // Own cleanup before the command can touch the mixer.
        // Mixer calls are bounded in the script; never block the app's Binder indefinitely.
        val reply = java.util.concurrent.CompletableFuture.supplyAsync { p.inputStream.bufferedReader().readLine() }
        val line = runCatching { reply.get(4, TimeUnit.SECONDS) }.getOrNull()
        if (line != "READY") {
            restore("startup failed")
            error("Receiver amp lease could not start")
        }
        return "muted: receiver amplifier (root)".also { lastMuteResult = it }
    }

    fun restore(why: String): String {
        val p = worker ?: return "not muted"
        runCatching { p.outputStream.close() } // EOF also occurs automatically on helper death.
        if (!p.waitFor(4, TimeUnit.SECONDS)) {
            return "failed: receiver restore pending ($why)".also { lastRestoreResult = it }
        }
        return if (p.exitValue() in 0..127) {
            worker = null
            "restored receiver amplifier ($why)".also { lastRestoreResult = it }
        } else {
            "failed: receiver restore ($why); restoration pending".also { lastRestoreResult = it }
        }
    }

    companion object {
        val supported get() = Build.DEVICE == "fuxi" && Process.myUid() == 0
        private const val AMP = "T AMP Enable Switch"
        // No configuration files, route changes, microphone mute or saved volume changes. Only
        // an active receiver originally On is owned. Route teardown may already have restored Off.
        internal val SCRIPT = """
            mix() { /system/bin/timeout 2 /system/bin/tinymix "${'$'}@"; }
            owned=0
            restore() {
              [ "${'$'}owned" = 1 ] || return 0
              rcv=${'$'}(mix "T Enable RCV Pin Control") || return 1
              case "${'$'}rcv" in
                "T Enable RCV Pin Control: On") mix "T AMP Enable Switch" 1 || return 1 ;;
                "T Enable RCV Pin Control: Off") return 0 ;;
                *) return 1 ;;
              esac
              [ "${'$'}(mix "T AMP Enable Switch")" = "T AMP Enable Switch: On" ]
            }
            # EXIT must finish restoration before returning, including startup failures.
            trap 'until restore; do sleep 1; done' EXIT
            trap 'exit 0' HUP INT TERM
            [ "${'$'}(mix "T Enable RCV Pin Control")" = "T Enable RCV Pin Control: On" ] || exit 1
            [ "${'$'}(mix "T AMP Enable Switch")" = "T AMP Enable Switch: On" ] || exit 1
            owned=1
            mix "T AMP Enable Switch" 0 || exit 1
            [ "${'$'}(mix "T AMP Enable Switch")" = "T AMP Enable Switch: Off" ] || exit 1
            echo READY
            # The owner keeps stdin open. EOF is guaranteed when it exits or dies.
            read -r release
            exit 0
        """.trimIndent()
    }
}
