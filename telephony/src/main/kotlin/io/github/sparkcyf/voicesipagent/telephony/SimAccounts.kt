package io.github.sparkcyf.voicesipagent.telephony

import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/** Resolve the current physical slot, never a saved subscription ID or list ordering. */
internal fun selectedSimAccount(context: Context, telecom: TelecomManager, telephony: TelephonyManager,
    slot: Int): PhoneAccountHandle {
    require(slot in 1..2)
    val subscription = context.getSystemService(SubscriptionManager::class.java)
        .getActiveSubscriptionInfoForSimSlotIndex(slot - 1) ?: error("SIM absent")
    check(telephony.createForSubscriptionId(subscription.subscriptionId).simState == TelephonyManager.SIM_STATE_READY)
    val accounts = telecom.callCapablePhoneAccounts.filter { handle ->
        telecom.getPhoneAccount(handle)?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) == true &&
            telephony.createForPhoneAccountHandle(handle)?.subscriptionId == subscription.subscriptionId
    }
    return accounts.singleOrNull() ?: error("SIM phone account unavailable or ambiguous")
}

/** Incoming calls resolve their actual Telecom account, never the default outgoing SIM. */
internal fun incomingSimSlot(context: Context, telecom: TelecomManager, telephony: TelephonyManager,
    handle: PhoneAccountHandle?): Int? = runCatching {
    if (handle == null) return@runCatching null
    if (telecom.getPhoneAccount(handle)?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) != true)
        return@runCatching null
    val subId = telephony.createForPhoneAccountHandle(handle)?.subscriptionId ?: return@runCatching null
    val info = context.getSystemService(SubscriptionManager::class.java)
        .getActiveSubscriptionInfo(subId) ?: return@runCatching null
    (info.simSlotIndex + 1).takeIf { it in 1..2 }
}.getOrNull()
