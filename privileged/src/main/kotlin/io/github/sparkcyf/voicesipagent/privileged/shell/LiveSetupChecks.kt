package io.github.sparkcyf.voicesipagent.privileged.shell

import android.app.AppOpsManager
import android.content.pm.PackageManager
import android.os.PowerManager

/** Live target-package observations; no result survives a call or a client generation. */
internal class LiveSetupChecks(
    private val packages: PackageManager,
    private val appOps: AppOpsManager?,
    private val power: PowerManager?,
) {
    fun ongoingCallsAllowed(packageName: String, fallback: () -> Boolean): Boolean {
        val mode = runCatching {
            val uid = packages.getApplicationInfo(packageName, 0).uid
            // Raw mode must not turn a foreground-only grant into a permanent grant.
            appOps?.unsafeCheckOpRawNoThrow("android:manage_ongoing_calls", uid, packageName)
        }.getOrNull()
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_DEFAULT, AppOpsManager.MODE_FOREGROUND -> false
            // Preserve the existing command path on ROM/API failures or an unavailable manager.
            else -> fallback()
        }
    }

    fun dozeAllowlisted(packageName: String, fallback: () -> Boolean): Boolean =
        runCatching { power?.isIgnoringBatteryOptimizations(packageName) }.getOrNull() ?: fallback()
}
