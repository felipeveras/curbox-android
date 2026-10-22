package neth.iecal.curbox.blockers

import android.view.accessibility.AccessibilityEvent
import neth.iecal.curbox.hardcoded.ReelAppConfig.Companion.INSTAGRAM_DM_REEL_MARKER
import neth.iecal.curbox.services.BaseBlockingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ReelScriptRunnerTest {

    @Test
    fun unavailableRootAndUnsupportedForegroundBothCollapseToNull() {
        val service = Robolectric.buildService(BaseBlockingService::class.java).create().get()
        val runner = ReelScriptRunner().apply { setup(service) }
        val supportedEvent = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
            packageName = "com.google.android.youtube"
        }
        val unsupportedEvent = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = "com.example.foreground"
        }
        try {
            // The service fixture has no active root; real navigation outside YouTube also has no detection.
            assertNull(runner.detect(supportedEvent))
            assertNull(runner.detect(unsupportedEvent))
        } finally {
            supportedEvent.recycle()
            unsupportedEvent.recycle()
            service.onDestroy()
        }
    }

    @Test
    fun regularComparatorHasNoDmInboxFlag() {
        val detection = reelDetectionFromScriptResult("author caption") { it }

        assertEquals("author caption", detection.comparator)
        assertFalse(detection.isInstagramReelOpenedFromDmInbox)
    }

    @Test
    fun dmInboxMarkerIsRemovedBeforeComparatorIsConsumed() {
        val detection = reelDetectionFromScriptResult(
            "${INSTAGRAM_DM_REEL_MARKER}author caption"
        ) { it.uppercase() }

        assertEquals("AUTHOR CAPTION", detection.comparator)
        assertTrue(detection.isInstagramReelOpenedFromDmInbox)
    }
}
