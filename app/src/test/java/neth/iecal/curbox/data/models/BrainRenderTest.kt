package neth.iecal.curbox.data.models

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import neth.iecal.curbox.ui.overlay.ReelsOverlayManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BrainRenderTest {
    @Test @Config(qualifiers = "en-rUS-notnight-xhdpi") fun fiveLightStages() = render("light", Color.rgb(245,245,245))
    @Test @Config(qualifiers = "pt-rBR-night-xhdpi") fun fiveDarkStages() = render("dark", Color.rgb(22,22,22))
    private fun render(theme: String, background: Int) {
        val app = RuntimeEnvironment.getApplication()
        val manager = ReelsOverlayManager(app)
        manager.startDisplaying()
        val b = manager.binding!!
        val counts = listOf(0, 3, 6, 10, 15)
        var priorPinkPixels = Int.MAX_VALUE
        var priorPinkHeight = Int.MAX_VALUE
        counts.forEachIndexed { stage, count ->
            manager.reelsScrolledThisSession = count
            assertEquals(stage, b.brain.stage)
            assertTrue(b.root.contentDescription.contains(count.toString()))
            b.root.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            b.root.layout(0,0,b.root.measuredWidth,b.root.measuredHeight)
            assertTrue(b.reelCounter.top >= b.brain.bottom)
            val bitmap = Bitmap.createBitmap(b.root.width,b.root.height,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap); canvas.drawColor(background); b.root.draw(canvas)
            val pink = Color.rgb(248, 145, 181)
            var pinkPixels = 0
            var minY = bitmap.height
            var maxY = -1
            var whiteEyePixelsOverBody = 0
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                if (bitmap.getPixel(x, y) == pink) { pinkPixels++; minY = minOf(minY, y); maxY = maxOf(maxY, y) }
            }
            if (stage == 4) for (y in minY..maxY) for (x in b.brain.left until b.brain.right) {
                if (bitmap.getPixel(x, y) == Color.WHITE) whiteEyePixelsOverBody++
            }
            val pinkHeight = maxY - minY + 1
            assertTrue("Pink brain body mass must shrink at stage $stage", pinkPixels < priorPinkPixels)
            assertTrue("Brain silhouette height must shrink at stage $stage", pinkHeight < priorPinkHeight)
            if (stage == 4) assertTrue("Fully melted stage keeps its eyes on the puddle", whiteEyePixelsOverBody > 0)
            priorPinkPixels = pinkPixels
            priorPinkHeight = pinkHeight
            val output = File("/home/marcos/builds/curbox-brain-counter-v11-evidence/native-fixture-$theme-stage-$stage.png")
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
        manager.removeOverlay()
    }

    @Test fun exactStageBoundaries() {
        val manager = ReelsOverlayManager(RuntimeEnvironment.getApplication())
        manager.startDisplaying()
        listOf(0 to 0,2 to 0,3 to 1,5 to 1,6 to 2,9 to 2,10 to 3,14 to 3,15 to 4,Int.MAX_VALUE to 4).forEach { (count, stage) ->
            manager.reelsScrolledThisSession=count
            assertEquals(stage,manager.binding!!.brain.stage)
            assertEquals(count.toString(),manager.binding!!.reelCounter.text.toString())
        }
        manager.removeOverlay()
    }
}
