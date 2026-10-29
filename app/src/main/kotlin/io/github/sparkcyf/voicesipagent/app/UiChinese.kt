package io.github.sparkcyf.voicesipagent.app

import io.github.sparkcyf.voicesipagent.privileged.ShizukuConnector
import io.github.sparkcyf.voicesipagent.telephony.CallSnapshot

/** Presentation only: keep protocol values, diagnostic records and monitor contracts unchanged. */
internal object UiChinese {
    fun calls(c: CallSnapshot) = when {
        c.count == 0 -> "当前没有手机通话"
        c.incoming -> "有手机来电，共 ${c.count} 通；网关处理 ${c.owned} 通"
        else -> "当前手机通话 ${c.count} 通；网关处理 ${c.owned} 通"
    }

    fun shizuku(s: ShizukuConnector.State) = when (s) {
        ShizukuConnector.State.STOPPED -> "已停止"
        ShizukuConnector.State.SHIZUKU_NOT_RUNNING -> "Shizuku 未运行，请先在助手中启动"
        ShizukuConnector.State.PERMISSION_REQUIRED -> "尚未授权，请点击允许 Shizuku 授权"
        ShizukuConnector.State.PERMISSION_DENIED -> "授权被拒绝，请前往 Shizuku 允许网关访问"
        ShizukuConnector.State.BINDING -> "正在连接权限服务"
        ShizukuConnector.State.CONNECTED -> "权限服务已连接"
        ShizukuConnector.State.ERROR -> "权限服务连接异常，请重试或打开助手修复"
    }

    fun sip(ready: Boolean, value: String): String {
        if (ready) return if (value.contains("refresh", true)) "已连接，正在续期注册" else "已连接并注册"
        val s = value.lowercase()
        return when {
            "not configured" in s || "missing" in s -> "尚未填写完整 SIP 账号"
            "unregister" in s || "stopped" in s -> "注册已停止"
            "held" in s || "hold" in s -> "等待手机通话功能就绪后注册"
            "403" in s || "forbidden" in s -> "服务器拒绝注册，请核对账号权限"
            "401" in s || "407" in s || "auth" in s -> "正在验证账号；若持续失败，请核对用户名和密码"
            "timeout" in s || "timed out" in s || "408" in s -> "连接服务器超时，请检查网络、地址与端口"
            "dns" in s || "resolve" in s -> "无法解析服务器地址，请检查网络和域名"
            "registering" in s || "starting" in s -> "正在连接并注册"
            "network" in s -> "网络暂时不可用，等待连接恢复"
            else -> "SIP 尚未连接，具体返回信息见原始诊断日志"
        }
    }

    fun readiness(h: HealthState): String = when {
        h.dialerChangeRunning -> "正在切换默认电话应用"
        h.takeoverOffPending -> "当前通话结束后关闭网关"
        h.takeoverOnPending -> "当前通话结束后启用网关"
        !h.takeover -> "网关已关闭"
        h.dialerChangePending -> "等待完成默认电话应用切换"
        !h.gatewayRunning -> "后台服务尚未运行，请启用网关或等待助手恢复"
        h.shizuku != ShizukuConnector.State.CONNECTED -> shizuku(h.shizuku)
        h.phoneAccessProblem != null -> h.phoneAccessProblem
        !h.callAccessReady -> "通话访问权限尚未就绪，请重新准备通话权限"
        !h.audioReady -> "通话音频尚未就绪，请检查音频方式及权限服务"
        !h.loopbackTest && !h.sip.ready -> sip(false, h.sip.detail)
        h.loopbackTest -> "手机通话功能已就绪，当前启用了来电回声测试"
        else -> "网关已就绪，可以接打电话"
    }

    fun detail(value: String): String {
        if (value.isBlank() || value.any { it in '\u4e00'..'\u9fff' }) return value
        return when (value) {
            "Direct telephony" -> "直接读取通话音频"
            "Cellular call redirection" -> "系统通话音频重定向"
            "Unavailable" -> "暂不可用"
            "ok", "OK" -> "正常"
            else -> when {
                value.contains("restored", true) -> "已恢复本机通话声音"
                value.contains("cancel", true) -> "操作已取消"
                value.contains("denied", true) -> "权限被拒绝，请检查系统授权"
                value.contains("timeout", true) -> "操作超时，请重试"
                value.contains("pending", true) -> "操作正在等待完成"
                value.contains("failed", true) || value.contains("error", true) -> "操作未完成，详情见原始诊断日志"
                else -> "系统返回了技术详情，请展开原始诊断日志查看"
            }
        }
    }
}
