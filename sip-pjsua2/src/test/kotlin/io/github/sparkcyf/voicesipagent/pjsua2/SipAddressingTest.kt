package io.github.sparkcyf.voicesipagent.pjsua2

import java.net.InetAddress
import org.junit.Assert.*
import org.junit.Test

class SipAddressingTest {
    @Test fun ipv6OnlyDnsRequiresIpv6TransportAndMedia() {
        assertTrue(SipAddressing.useIpv6(listOf(InetAddress.getByName("2001:db8::1"))))
    }
    @Test fun ipv4LoopbackAndLanKeepIpv4Transport() {
        assertFalse(SipAddressing.useIpv6(listOf(InetAddress.getByName("127.0.0.1"))))
        assertFalse(SipAddressing.useIpv6(listOf(InetAddress.getByName("192.0.2.1"))))
    }
    @Test fun mixedDnsRespectsDefaultNetworkResolverOrder() {
        val v4 = InetAddress.getByName("192.0.2.1")
        val v6 = InetAddress.getByName("2001:db8::1")
        assertFalse(SipAddressing.useIpv6(listOf(v4,v6)))
        assertTrue(SipAddressing.useIpv6(listOf(v6,v4)))
    }
    @Test fun uriHostsBracketIpv6LiteralsExactlyOnce() {
        assertEquals("[2001:db8::1]",SipAddressing.uriHost("2001:db8::1"))
        assertEquals("[2001:db8::1]",SipAddressing.uriHost("[2001:db8::1]"))
        assertEquals("sip.example.com",SipAddressing.uriHost("sip.example.com"))
    }
}
