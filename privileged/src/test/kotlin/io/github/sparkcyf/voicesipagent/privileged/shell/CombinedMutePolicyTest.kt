package io.github.sparkcyf.voicesipagent.privileged.shell

import io.github.sparkcyf.voicesipagent.privileged.AudioMethod
import org.junit.Assert.*
import org.junit.Test

class CombinedMutePolicyTest {
    @Test fun combinedModeKeepsDirectCaptureAndLeavesSystemRedirectionUntouched() {
        assertEquals(AudioMethod.LEGACY, AudioMethod.captureMethod(AudioMethod.DIRECT_RECEIVER_MUTE))
        assertFalse(AudioMethod.usesRedirectMute(AudioMethod.DIRECT_RECEIVER_MUTE,"direct"))
        assertFalse(AudioMethod.usesRedirectMute(AudioMethod.LEGACY,"direct"))
        assertTrue(AudioMethod.usesRedirectMute(AudioMethod.OFFICIAL,"redirection-manual"))
        assertEquals(AudioMethod.AUTO,AudioMethod.captureMethod(AudioMethod.AUTO))
    }
}
