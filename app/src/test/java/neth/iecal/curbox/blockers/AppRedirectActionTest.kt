package neth.iecal.curbox.blockers

import org.junit.Assert.*
import org.junit.Test

class AppRedirectActionTest {
    @Test fun `blank destination is disabled`() {
        assertFalse(AppRedirectAction("  ", "curbox", emptySet()).open { true })
    }

    @Test fun `missing destination or launch failure stays blocked without retry`() {
        for (target in listOf(null, "missing")) {
            val action = AppRedirectAction(target, "curbox", emptySet())
            assertFalse(action.open { throw IllegalStateException("Removed or disabled app") })
            assertFalse(action.open { true })
        }
    }

    @Test fun `self and blocked destinations never launch`() {
        for (target in listOf("curbox", "social", "otherBlocked")) {
            var launched = false
            val action = AppRedirectAction(target, "curbox", setOf("social", "otherBlocked"))
            assertFalse("Unsafe destination $target", action.open { launched = true; true })
            assertFalse(launched)
        }
    }

    @Test fun `button launches selected app once without unlocking blocked app`() {
        val launched = mutableListOf<String>()
        val action = AppRedirectAction("com.amazon.kindle", "curbox", emptySet())
        assertTrue(action.open { launched.add(it); true })
        assertFalse(action.open { launched.add(it); true })
        assertEquals(listOf("com.amazon.kindle"), launched)
    }
}
