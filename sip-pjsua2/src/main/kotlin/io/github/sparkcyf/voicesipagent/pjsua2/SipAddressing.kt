package io.github.sparkcyf.voicesipagent.pjsua2

import java.net.InetAddress
import java.net.Inet6Address

/** Keep the OS resolver order, which reflects the default network's usable families. */
internal object SipAddressing {
    fun useIpv6(addresses: Collection<InetAddress>): Boolean = addresses.firstOrNull() is Inet6Address
    fun uriHost(host: String): String {
        val bare = host.removeSurrounding("[", "]")
        return if (':' in bare) "[$bare]" else bare
    }
}
