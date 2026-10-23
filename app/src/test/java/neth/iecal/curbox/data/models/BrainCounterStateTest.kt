package neth.iecal.curbox.data.models

import org.junit.Assert.*
import org.junit.Test

class BrainCounterStateTest {
    @Test fun restoredSessionCrossesMidnightWithoutResetAndHandlesBackwardClock() {
        val s = BrainCounterState(count=24, lastSeen=1_000_000)
        s.observe(1_000_001,"2026-01-02",true)
        assertEquals(24,s.count)
        s.observe(500_000,"2026-01-01",false)
        assertEquals(24,s.count)
        s.observe(799_999,"2026-01-01",true)
        assertEquals(24,s.count)
        s.observe(1_100_000,"2026-01-01",true)
        assertEquals(0,s.count)
    }
    @Test fun inactivityExpiresAtExactBoundaryAfterRestore() {
        val s = BrainCounterState(count=50,lastSeen=1000)
        s.observe(300999,"today",true)
        assertEquals(50,s.count)
        val restored = BrainCounterState(s.count,s.lastSeen,s.outsideSince)
        restored.observe(600999,"today",true)
        assertEquals(0,restored.count)
    }
    @Test fun countSaturatesWithoutNegativeOverflow() {
        val s = BrainCounterState(Int.MAX_VALUE)
        s.increment()
        assertEquals(Int.MAX_VALUE,s.count)
    }
    @Test fun sessionSurvivesSwitchAndExpiresAtExactFiveMinutes() {
        val s = BrainCounterState()
        s.observe(1000, "2026-01-01", true)
        s.increment()
        s.observe(2000, "2026-01-01", false)
        s.observe(301999, "2026-01-01", true)
        assertEquals(1, s.count)
        s.observe(302000, "2026-01-01", false)
        s.observe(602000, "2026-01-01", true)
        assertEquals(0, s.count)
    }
}
