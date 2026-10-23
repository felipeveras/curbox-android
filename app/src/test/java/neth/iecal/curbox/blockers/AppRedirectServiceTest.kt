package neth.iecal.curbox.blockers

import android.app.Application
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import neth.iecal.curbox.data.models.*
import neth.iecal.curbox.services.BaseBlockingService
import neth.iecal.curbox.utils.TimerNotification
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AppRedirectServiceTest {
    class ForegroundService : BaseBlockingService() {
        var foreground: android.view.accessibility.AccessibilityNodeInfo? = null
        override fun getRootInActiveWindow() = foreground
    }

    @Test fun `background content events cannot replace warning but real reopen remains blocked`() {
        val service = Robolectric.buildService(ForegroundService::class.java).create().get()
        service.lastBackPressTimeStamp = -10_000L
        val blocker = AppBlocker()
        fun set(name: String, value: Any) = AppBlocker::class.java.getDeclaredField(name).apply { isAccessible = true }.set(blocker, value)
        set("service", service)
        set("notificationManager", TimerNotification(service, 1001))
        val constructor = Class.forName("neth.iecal.curbox.blockers.AppBlocker\$AppGroupEntry").declaredConstructors.first().apply { isAccessible = true }
        val entry = constructor.newInstance("reading", AppGroupConfig(), listOf("social"), AppBlockerWarningScreenConfig(isOnOpenConfig = true), "com.amazon.kindle", true)
        val field = AppBlocker::class.java.getDeclaredField("blockedAppsList").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (field.get(blocker) as MutableMap<String, MutableList<Any>>)["social"] = mutableListOf(entry)
        fun event(type: Int) = AccessibilityEvent.obtain(type).apply { packageName = "social" }
        blocker.doAppBlockerCheck(event(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100))
        assertNotNull(shadowOf(service).nextStartedActivity)
        val root = android.view.accessibility.AccessibilityNodeInfo.obtain().apply { packageName = service.packageName }
        service.foreground = root
        // Video/content updates from an underlying app after the 1s gate must not
        // press Home again and CLEAR_TASK the running five-second warning.
        blocker.doAppBlockerCheck(event(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertNull("Stale background content must not restart the warning", shadowOf(service).nextStartedActivity)
        root.packageName = "social"
        blocker.doAppBlockerCheck(event(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertNotNull("A genuinely foreground blocked app is still enforced", shadowOf(service).nextStartedActivity)
    }

    @Test fun `blocking event forwards group alternative and all blocked destinations to screen`() {
        val service = Robolectric.buildService(BaseBlockingService::class.java).create().get()
        service.lastBackPressTimeStamp = -10_000L
        val blocker = AppBlocker()
        fun set(name: String, value: Any) = AppBlocker::class.java.getDeclaredField(name).apply { isAccessible = true }.set(blocker, value)
        set("service", service)
        set("notificationManager", TimerNotification(service, 1001))
        val warning = AppBlockerWarningScreenConfig(isOnOpenConfig = true)
        val constructor = Class.forName("neth.iecal.curbox.blockers.AppBlocker\$AppGroupEntry").declaredConstructors.first().apply { isAccessible = true }
        val args = mutableListOf<Any?>("reading", AppGroupConfig(), listOf("social"), warning)
        if (constructor.parameterCount > 4) args.add("com.amazon.kindle")
        args.add(true)
        val entry = constructor.newInstance(*args.toTypedArray())
        val field = AppBlocker::class.java.getDeclaredField("blockedAppsList").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = field.get(blocker) as MutableMap<String, MutableList<Any>>
        map["social"] = mutableListOf(entry)
        map["otherBlocked"] = mutableListOf(entry)
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply { packageName = "social" }
        blocker.doAppBlockerCheck(event)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        val intent = shadowOf(service).nextStartedActivity
        assertNotNull("The normal block screen still opens", intent)
        assertEquals("com.amazon.kindle", intent.getStringExtra("redirect_package"))
        assertTrue(intent.getStringArrayListExtra("redirect_blocked_packages")!!.containsAll(listOf("social", "otherBlocked")))
        assertEquals("reading", intent.getStringExtra("result_id"))
        assertTrue(intent.getBooleanExtra("redirect_auto", false))
        event.recycle()
    }
}
