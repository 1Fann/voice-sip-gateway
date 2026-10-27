package io.github.sparkcyf.voicesipagent.telephony

import android.content.Context
import android.telecom.*
import android.telephony.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class SimAccountsTest {
    private val context = mock(Context::class.java)
    private val telecom = mock(TelecomManager::class.java)
    private val telephony = mock(TelephonyManager::class.java)
    private val subscriptions = mock(SubscriptionManager::class.java)
    private val info = mock(SubscriptionInfo::class.java)
    private val perSim = mock(TelephonyManager::class.java)
    private val handle = mock(PhoneAccountHandle::class.java)
    private val account = mock(PhoneAccount::class.java)

    private fun ready() {
        `when`(context.getSystemService(SubscriptionManager::class.java)).thenReturn(subscriptions)
        `when`(subscriptions.getActiveSubscriptionInfoForSimSlotIndex(1)).thenReturn(info)
        `when`(info.subscriptionId).thenReturn(47) // Deliberately unrelated to slot number.
        `when`(telephony.createForSubscriptionId(47)).thenReturn(perSim)
        `when`(perSim.simState).thenReturn(TelephonyManager.SIM_STATE_READY)
        `when`(perSim.subscriptionId).thenReturn(47)
        `when`(telecom.callCapablePhoneAccounts).thenReturn(listOf(handle))
        `when`(telecom.getPhoneAccount(handle)).thenReturn(account)
        `when`(account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)).thenReturn(true)
        `when`(telephony.createForPhoneAccountHandle(handle)).thenReturn(perSim)
    }
    @Test fun resolvesPhysicalSlotUsingCurrentSubscription() {
        ready()
        assertSame(handle, selectedSimAccount(context, telecom, telephony, 2))
        verify(subscriptions).getActiveSubscriptionInfoForSimSlotIndex(1)
        verify(telecom, never()).getDefaultOutgoingPhoneAccount(anyString())
    }
    @Test fun missingSelectedSimDoesNotFallBack() {
        ready()
        `when`(subscriptions.getActiveSubscriptionInfoForSimSlotIndex(1)).thenReturn(null)
        assertThrows(IllegalStateException::class.java) { selectedSimAccount(context, telecom, telephony, 2) }
    }
    @Test fun lockedSelectedSimDoesNotFallBack() {
        ready(); `when`(perSim.simState).thenReturn(TelephonyManager.SIM_STATE_PIN_REQUIRED)
        assertThrows(IllegalStateException::class.java) { selectedSimAccount(context, telecom, telephony, 2) }
    }
    @Test fun mismatchedPhoneAccountDoesNotFallBack() {
        ready(); `when`(perSim.subscriptionId).thenReturn(99)
        assertThrows(IllegalStateException::class.java) { selectedSimAccount(context, telecom, telephony, 2) }
    }
    @Test fun duplicateAccountsAreRejected() {
        ready(); `when`(telecom.callCapablePhoneAccounts).thenReturn(listOf(handle, handle))
        assertThrows(IllegalStateException::class.java) { selectedSimAccount(context, telecom, telephony, 2) }
    }
}
