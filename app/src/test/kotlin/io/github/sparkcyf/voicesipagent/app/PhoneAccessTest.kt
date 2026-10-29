package io.github.sparkcyf.voicesipagent.app

import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class PhoneAccessTest {
    @Test fun deniedPermissionDoesNotReadPrivateTelephonyData() {
        val c = mock(Context::class.java)
        `when`(c.checkSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_DENIED)
        assertTrue(PhoneAccess.problem(c)!!.contains("权限"))
        verify(c, never()).getSystemService(TelephonyManager::class.java)
    }
    @Test fun grantedButEmptyInformationIsNotReady() {
        val c = mock(Context::class.java)
        val tm = mock(TelephonyManager::class.java)
        val sm = mock(SubscriptionManager::class.java)
        val telecom = mock(TelecomManager::class.java)
        `when`(c.checkSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(c.getSystemService(TelephonyManager::class.java)).thenReturn(tm)
        `when`(c.getSystemService(SubscriptionManager::class.java)).thenReturn(sm)
        `when`(c.getSystemService(TelecomManager::class.java)).thenReturn(telecom)
        `when`(sm.activeSubscriptionInfoList).thenReturn(emptyList())
        `when`(telecom.callCapablePhoneAccounts).thenReturn(emptyList())
        assertTrue(PhoneAccess.problem(c)!!.contains("空白通行证"))
        // Blank subscriber numbers are never used as the permission test.
        verifyNoInteractions(tm)
    }
}
