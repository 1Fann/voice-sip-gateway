package io.github.sparkcyf.voicesipagent.app

import org.junit.Assert.*
import org.junit.Test
import io.github.sparkcyf.voicesipagent.privileged.ShizukuConnector
import io.github.sparkcyf.voicesipagent.telephony.CallSnapshot

class UiChineseTest {
    @Test fun registrationFailuresKeepActionableDistinctions() {
        assertTrue(UiChinese.sip(false,"registration failed: 403 Forbidden").contains("拒绝"))
        assertTrue(UiChinese.sip(false,"registration timeout").contains("超时"))
        assertTrue(UiChinese.sip(false,"DNS lookup failed").contains("域名"))
        assertTrue(UiChinese.sip(true,"refreshing registration").contains("已连接"))
    }
    @Test fun phonePermissionFailureIsNotShownAsReady() {
        val h=HealthState(gatewayRunning=true,shizuku=ShizukuConnector.State.CONNECTED,phoneAccessProblem="电话信息为空")
        assertEquals("电话信息为空",UiChinese.readiness(h))
        assertTrue(UiChinese.readiness(h.copy(takeover=false)).contains("已关闭"))
    }
    @Test fun callPresentationUsesCountsWithoutExposingRawNumbers() {
        val s=UiChinese.calls(CallSnapshot(count=1,owned=1,summary="caller 123456789"))
        assertFalse(s.contains("123456789"))
        assertTrue(s.contains("1"))
    }
}
