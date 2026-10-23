package neth.iecal.curbox.data.models

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import neth.iecal.curbox.services.BaseBlockingService
import neth.iecal.curbox.trackers.ReelsCountTracker
import neth.iecal.curbox.ui.overlay.ReelsOverlayManager
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class BrainTrackerTest {
    private fun field(tracker: ReelsCountTracker, name: String): java.lang.reflect.Field = ReelsCountTracker::class.java.getDeclaredField(name).apply { isAccessible=true }
    private fun session(tracker: ReelsCountTracker) = field(tracker,"session").get(tracker) as BrainCounterState
    private fun event(tracker: ReelsCountTracker, text: String, pkg: String = "com.google.android.youtube") {
        val e = AccessibilityEvent.obtain(neth.iecal.curbox.hardcoded.ReelAppConfig.reelData[pkg]!!.eventType).apply { packageName=pkg }
        tracker.onEvent(e,text); e.recycle()
    }
    @Test fun restartAndAppSwitchPreserveSessionWithoutDuplicateAnalytics() {
        val service = Robolectric.buildService(BaseBlockingService::class.java).create().get()
        shadowOf(service.getSystemService(Context.POWER_SERVICE) as PowerManager).setIsInteractive(true)
        var callbacks=0
        val first=ReelsCountTracker()
        first.setup(service,ReelsOverlayManager(service)) { callbacks++ }
        val baseline=first.getTodayCount()
        event(first,"initial unique caption")
        event(first,"second different story")
        assertEquals(1,session(first).count)
        first.onDestroy()
        val second=ReelsCountTracker()
        second.setup(service,ReelsOverlayManager(service)) { callbacks++ }
        assertEquals(1,session(second).count)
        event(second,"second different story")
        assertEquals(1,callbacks)
        assertEquals(baseline+1,second.getTodayCount())
        event(second,"instagram first author","com.instagram.android")
        event(second,"new independent account","com.instagram.android")
        assertEquals(2,session(second).count)
        assertEquals(2,callbacks)
        field(second,"overlayConfig").set(second,ReelCounterOverlayConfig(dailyMode=true))
        val display=ReelsCountTracker::class.java.getDeclaredMethod("displayCount").apply { isAccessible=true }
        assertEquals(baseline+2,display.invoke(second))
        field(second,"overlayConfig").set(second,ReelCounterOverlayConfig(dailyMode=false))
        assertEquals(2,display.invoke(second))
        second.onDestroy()
    }
    @Test fun localDateChangeLoadsExistingDayInsteadOfDiscardingAnalytics() {
        val old=java.util.TimeZone.getDefault()
        val service = Robolectric.buildService(BaseBlockingService::class.java).create().get()
        shadowOf(service.getSystemService(Context.POWER_SERVICE) as PowerManager).setIsInteractive(true)
        val tracker=ReelsCountTracker()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("GMT-12:00"))
            tracker.setup(service,ReelsOverlayManager(service)) { }
            val dayOne=neth.iecal.curbox.utils.TimeTools.getCurrentDate()
            event(tracker,"initial unique clip"); event(tracker,"other independent movie")
            val firstDayCount=tracker.getTodayCount()
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("GMT+14:00"))
            val dayTwo=neth.iecal.curbox.utils.TimeTools.getCurrentDate()
            assertNotEquals(dayOne,dayTwo)
            val dao=neth.iecal.curbox.data.db.AppDatabase.getInstance(service).reelStatsDao()
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { dao.upsert(neth.iecal.curbox.data.db.ReelStatsEntity(date=dayTwo,count=17)) }
            event(tracker,"another fresh caption")
            assertEquals(18,tracker.getTodayCount())
            assertEquals(2,session(tracker).count)
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("GMT-12:00"))
            event(tracker,"different distinct video")
            assertEquals(firstDayCount+1,tracker.getTodayCount())
        } finally { tracker.onDestroy(); java.util.TimeZone.setDefault(old) }
    }

    @Test fun restartRetainsBoundedDeduplicationHistory() {
        val service=Robolectric.buildService(BaseBlockingService::class.java).create().get()
        shadowOf(service.getSystemService(Context.POWER_SERVICE) as PowerManager).setIsInteractive(true)
        var callbacks=0
        val first=ReelsCountTracker()
        first.setup(service,ReelsOverlayManager(service)) { callbacks++ }
        event(first,"alpha movie initial")
        event(first,"bravo story second")
        event(first,"charlie clip third")
        assertEquals(2,callbacks)
        first.onDestroy()
        val restored=ReelsCountTracker()
        restored.setup(service,ReelsOverlayManager(service)) { callbacks++ }
        try {
            event(restored,"bravo story second")
            assertEquals("Previously counted clip must remain deduplicated after restart",2,callbacks)
        } finally { restored.onDestroy() }
    }
    @Test fun overlayPositionCallbackPersistsThroughRealSettingsStore() {
        shadowOf(org.robolectric.RuntimeEnvironment.getApplication()).declareComponentUnbindable(
            android.content.ComponentName(org.robolectric.RuntimeEnvironment.getApplication(), androidx.room.MultiInstanceInvalidationService::class.java))
        val service=Robolectric.buildService(BaseBlockingService::class.java).create().get()
        val manager=ReelsOverlayManager(service)
        val tracker=ReelsCountTracker()
        tracker.setup(service,manager) { }
        try {
            manager.onPositionChanged(0.2f,0.8f)
            var config=ReelCounterOverlayConfig()
            repeat(100) {
                shadowOf(android.os.Looper.getMainLooper()).idle()
                config=kotlinx.coroutines.runBlocking { service.dataStoreManager.settings.first().reelCounterOverlayConfig }
                if(config.positionX != 0.2f) Thread.sleep(10)
            }
            assertEquals(0.2f,config.positionX)
            assertEquals(0.8f,config.positionY)
        } finally { tracker.onDestroy() }
    }
    @Test fun screenOffCannotCountStaleComparator() {
        val service = Robolectric.buildService(BaseBlockingService::class.java).create().get()
        val tracker = ReelsCountTracker()
        var callbacks = 0
        tracker.setup(service, ReelsOverlayManager(service)) { callbacks++ }
        val power = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIsInteractive(true)
        fun event(text: String) {
            val e = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply { packageName = "com.google.android.youtube" }
            tracker.onEvent(e, text)
            e.recycle()
        }
        event("alpha first movie")
        event("beta second story")
        assertEquals(1, callbacks)
        shadowOf(power).setIsInteractive(false)
        event("gamma third clip")
        assertEquals("Screen off cannot count unseen clips", 1, callbacks)
        tracker.onDestroy()
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED)
    fun queuedOverlayCreationIsDiscardedAfterSwitchingToUnsupportedApp() {
        shadowOf(org.robolectric.RuntimeEnvironment.getApplication()).declareComponentUnbindable(
            android.content.ComponentName(org.robolectric.RuntimeEnvironment.getApplication(), androidx.room.MultiInstanceInvalidationService::class.java))
        val service = Robolectric.buildService(BaseBlockingService::class.java).create().get()
        val power = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIsInteractive(true)
        ShadowSettings.setCanDrawOverlays(true)
        val manager = ReelsOverlayManager(service)
        val tracker = ReelsCountTracker()
        tracker.setup(service, manager) { }
        try {
            // Queue a valid supported-app render while the main looper is paused.
            event(tracker, "first distinct video")
            // A foreground app switch arrives before the queued render runs.
            val other = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
                packageName = "com.example.foreground"
            }
            tracker.onEvent(other, null)
            other.recycle()

            shadowOf(android.os.Looper.getMainLooper()).idleFor(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            assertFalse("A stale queued render must not appear over an unsupported app", manager.isOverlayVisible)
        } finally {
            tracker.onDestroy()
            ShadowSettings.setCanDrawOverlays(false)
        }
    }
}
