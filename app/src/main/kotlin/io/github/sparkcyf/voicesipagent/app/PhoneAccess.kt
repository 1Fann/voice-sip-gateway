package io.github.sparkcyf.voicesipagent.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/** Never reads phone numbers. A missing own-number is normal and is not a privacy failure. */
object PhoneAccess {
    val required = arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.RECORD_AUDIO)
    fun missing(context: Context) = required.filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    fun problem(context: Context): String? {
        if (missing(context).isNotEmpty()) return "请允许电话、电话信息和麦克风权限 / Phone permissions required"
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java)
            val subs = context.getSystemService(SubscriptionManager::class.java).activeSubscriptionInfoList.orEmpty()
            val telecom = context.getSystemService(TelecomManager::class.java)
            val accounts = telecom.callCapablePhoneAccounts
            val active = subs.filter { it.subscriptionId >= 0 && it.simSlotIndex >= 0 }
            when {
                active.isEmpty() -> "没有可用的 SIM 信息：请检查插卡、SIM 开关及系统权限。MIUI/澎湃 OS 请关闭电话信息的空白通行证，选择允许。"
                active.none { sub -> accounts.any { handle ->
                    telecom.getPhoneAccount(handle)?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) == true &&
                        tm.createForPhoneAccountHandle(handle)?.subscriptionId == sub.subscriptionId
                } } -> "SIM 已识别，但系统未提供可用的拨号账户；请检查电话权限，关闭空白通行证后重试。"
                else -> null
            }
        } catch (_: SecurityException) { "系统阻止读取电话信息，请在权限设置中选择允许真实信息，关闭空白通行证。" }
          catch (_: Exception) { "SIM 服务尚未就绪，请稍后刷新权限检查。" }
    }
}
