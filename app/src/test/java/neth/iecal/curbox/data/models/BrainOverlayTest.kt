package neth.iecal.curbox.data.models

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import neth.iecal.curbox.ui.overlay.ReelsOverlayManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class BrainOverlayTest {
    @Test fun dragConsumesGestureWithoutClickingCloseAndCancelDoesNotSave() {
        val manager = ReelsOverlayManager(RuntimeEnvironment.getApplication())
        var saved=0
        var savedX=0f
        manager.onPositionChanged={ x, _ -> saved++; savedX=x }
        manager.startDisplaying()
        val root=manager.binding!!.root
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(0,0),android.view.View.MeasureSpec.makeMeasureSpec(0,0))
        root.layout(0,0,root.measuredWidth,root.measuredHeight)
        fun touch(action: Int,x: Float,y: Float) {
            val e=android.view.MotionEvent.obtain(0,0,action,x,y,0)
            assertTrue(manager.binding!!.dragHandle.dispatchTouchEvent(e)); e.recycle()
        }
        touch(android.view.MotionEvent.ACTION_DOWN,10f,10f)
        touch(android.view.MotionEvent.ACTION_MOVE,120f,50f)
        touch(android.view.MotionEvent.ACTION_UP,120f,50f)
        assertEquals(1,saved)
        assertTrue(savedX in 0f..1f)
        assertEquals(android.view.View.VISIBLE,root.visibility)
        touch(android.view.MotionEvent.ACTION_DOWN,10f,10f)
        touch(android.view.MotionEvent.ACTION_CANCEL,20f,20f)
        assertEquals(1,saved)
        manager.removeOverlay()
    }

    @Test fun overlayHasNoDismissControlAndVisibilityCanBeRestoredByLifecycle() {
        val manager = ReelsOverlayManager(RuntimeEnvironment.getApplication())
        manager.startDisplaying()
        assertNull("Counter must not contain a close or dismiss control", manager.binding!!.root.findViewWithTag<android.view.View>("close"))
        manager.setCounterVisible(false)
        assertEquals(android.view.View.GONE, manager.binding!!.root.visibility)
        manager.setCounterVisible(true)
        assertEquals(android.view.View.VISIBLE, manager.binding!!.root.visibility)
        manager.removeOverlay()
    }

    @Test fun overlayHasCompactBrainAboveReadableCount() {
        val manager = ReelsOverlayManager(RuntimeEnvironment.getApplication())
        manager.reelsScrolledThisSession = 42
        manager.startDisplaying()
        val root = manager.binding!!.root
        assertNotNull("Brain view must exist", root.findViewWithTag<android.view.View>("brain"))
        assertEquals("42", manager.binding!!.reelCounter.text.toString())
        assertTrue(manager.binding!!.reelCounter.textSize <= 28 * root.resources.displayMetrics.scaledDensity)
        assertTrue(root.contentDescription.toString().contains("42"))
        manager.removeOverlay()
    }
}
