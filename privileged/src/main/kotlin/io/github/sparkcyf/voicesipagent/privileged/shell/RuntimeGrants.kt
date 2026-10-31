package io.github.sparkcyf.voicesipagent.privileged.shell

import android.content.pm.PackageManager

/** Queries the gateway package, never the privileged process's own permissions. */
internal class RuntimeGrants(private val packages: PackageManager) {
    fun ensure(packageName: String, permission: String, grant: () -> Unit): Boolean {
        val granted = runCatching {
            packages.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (granted) return false
        grant()
        return true
    }
}
