package neth.iecal.curbox.ui.activity

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.os.Looper
import android.widget.Button
import com.google.gson.Gson
import neth.iecal.curbox.Constants
import neth.iecal.curbox.R
import neth.iecal.curbox.data.models.AppBlockerWarningScreenConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import java.time.Duration

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AutoRedirectTest {
    private fun screen(auto: Boolean = true, configure: (Intent) -> Unit = {}): org.robolectric.android.controller.ActivityController<WarningActivity> {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        shadowOf(app.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).setIsInteractive(true)
        val ai = ApplicationInfo().apply { packageName = "com.amazon.kindle"; nonLocalizedLabel = "Kindle" }
        shadowOf(app.packageManager).installPackage(android.content.pm.PackageInfo().apply { packageName = ai.packageName; applicationInfo = ai })
        val info = ResolveInfo().apply { activityInfo = ActivityInfo().apply {
            packageName = ai.packageName; name = "Reader"; applicationInfo = ai; nonLocalizedLabel = "Kindle"
        } }
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_INFO).setPackage(ai.packageName), info)
        return Robolectric.buildActivity(WarningActivity::class.java, Intent(app, WarningActivity::class.java)
            .putExtra("mode", Constants.WARNING_SCREEN_MODE_APP_BLOCKER)
            .putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(isProceedDisabled = true)))
            .putExtra("redirect_package", ai.packageName)
            .putExtra("redirect_auto", auto)
            .putExtra("launch_package", "social").apply(configure)).setup()
    }
    private fun advance(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    private fun noUnlock() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        assertTrue(shadowOf(app).broadcastIntents.none { it.action?.endsWith("appblocker.cooldown") == true })
    }
    @Test @Config(qualifiers = "pt-rBR-notnight") fun `auto screen separates redirect from unlock and keeps custom message`() {
        val c = screen { it.putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(message = listOf("Minha mensagem"), proceedDelayInSecs = 15))) }
        val d = ShadowAlertDialog.getLatestDialog()
        assertEquals(android.view.View.GONE, d.findViewById<Button>(R.id.btn_proceed).visibility)
        assertEquals("Vamos para o Kindle", d.findViewById<android.widget.TextView>(c.get().resources.getIdentifier("redirect_title", "id", c.get().packageName)).text.toString())
        assertEquals("Minha mensagem", d.findViewById<android.widget.TextView>(R.id.warning_msg).text.toString())
        d.findViewById<Button>(R.id.btn_proceed).performClick()
        noUnlock()
        advance(0)
        val status = d.findViewById<android.widget.TextView>(R.id.redirect_status)
        assertEquals("Abrindo em 5…", status.text.toString())
        advance(1); assertEquals("Abrindo em 4…", status.text.toString())
        advance(1); assertEquals("Abrindo em 3…", status.text.toString())
        assertMaterialSupport(d.findViewById(R.id.proceed_seconds))
        c.pause().stop().destroy()
    }
    @Test fun `auto mode suppresses only untouched legacy placeholder messages`() {
        val c = screen()
        assertEquals(android.view.View.GONE, ShadowAlertDialog.getLatestDialog().findViewById<android.widget.TextView>(R.id.warning_msg).visibility)
        c.pause().stop().destroy()
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "pt-rBR-w360dp-h800dp-notnight-mdpi")
    fun `render actual native automatic dialog`() {
        renderDialog(true, "auto")
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "pt-rBR-w360dp-h800dp-night-mdpi")
    fun `render actual native manual dark dialog`() = renderDialog(false, "manual-dark")
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "pt-rBR-w360dp-h800dp-night-mdpi")
    fun `render actual native automatic dark dialog`() = renderDialog(true, "auto-dark")
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "pt-rBR-w360dp-h800dp-notnight-mdpi")
    fun `render actual native manual light dialog`() = renderDialog(false, "manual-light")
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "pt-rBR-w360dp-h800dp-notnight-mdpi")
    fun `render actual native custom large font dialog`() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        renderDialog(true, "custom-large", true)
    }
    private fun renderDialog(auto: Boolean, name: String, custom: Boolean = false) {
        val c = screen(auto) {
            if (!auto) it.putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(proceedDelayInSecs = 15)))
            if (custom) it.putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(message = listOf("Reserve este momento para uma leitura tranquila."))))
        }
        advance(0)
        val renderedDialog = ShadowAlertDialog.getLatestDialog()
        val status = renderedDialog.findViewById<android.widget.TextView>(R.id.redirect_status)
        assertMaterialSupport(status)
        val button = renderedDialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_redirect)
        val enabled = intArrayOf(android.R.attr.state_enabled)
        assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(button.textColors.getColorForState(enabled, 0), button.backgroundTintList!!.getColorForState(enabled, 0)) >= 4.5)
        val decor = renderedDialog.window!!.decorView
        decor.measure(android.view.View.MeasureSpec.makeMeasureSpec(360, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.AT_MOST))
        decor.layout(0, 0, decor.measuredWidth, decor.measuredHeight)
        val bitmap = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        java.io.File("/home/marcos/.hermes/cache/scratch/curbox-original-native-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(decor.height > 100)
        c.pause().stop().destroy()
    }
    private fun assertOrdinaryWarningUnchanged() {
        val c = screen { it.removeExtra("redirect_package"); it.putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(proceedDelayInSecs = 15))) }
        val d = ShadowAlertDialog.getLatestDialog()
        assertEquals(android.view.View.VISIBLE, d.findViewById<Button>(R.id.btn_proceed).visibility)
        assertEquals(android.view.View.GONE, d.findViewById<Button>(R.id.btn_redirect).visibility)
        assertEquals(android.view.View.GONE, d.findViewById<android.widget.TextView>(R.id.redirect_status).visibility)
        val text = d.findViewById<android.widget.TextView>(R.id.proceed_seconds)
        val surface = com.google.android.material.color.MaterialColors.getColor(text, com.google.android.material.R.attr.colorSurfaceContainerHigh)
        assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(text.currentTextColor, surface) >= 4.5)
        advance(6); assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test @Config(qualifiers = "notnight") fun `ordinary light warning retains unlock and contrast`() = assertOrdinaryWarningUnchanged()
    @Test @Config(qualifiers = "night") fun `ordinary dark warning retains unlock and contrast`() = assertOrdinaryWarningUnchanged()
    private fun assertMaterialSupport(text: android.widget.TextView) {
        val expected = com.google.android.material.color.MaterialColors.getColor(text, com.google.android.material.R.attr.colorOnSurfaceVariant)
        assertEquals("Use original Material supporting ink, not hardcoded redirect palette", expected, text.currentTextColor)
        val surface = com.google.android.material.color.MaterialColors.getColor(text, com.google.android.material.R.attr.colorSurfaceContainerHigh)
        assertTrue("Supporting copy contrast", androidx.core.graphics.ColorUtils.calculateContrast(text.currentTextColor, surface) >= 4.5)
    }
    private fun assertRedirectColors() {
        val c = screen(false)
        val d = ShadowAlertDialog.getLatestDialog()
        val text = d.findViewById<android.widget.TextView>(R.id.proceed_seconds)
        assertFalse("Unlock delay copy must be formatted before the first timer tick", text.text.contains("%1"))
        assertMaterialSupport(text)
        val original = com.google.android.material.color.MaterialColors.getColor(c.get(), com.google.android.material.R.attr.colorPrimary, 0)
        assertEquals("Dialog inherits the original Activity palette", original, com.google.android.material.color.MaterialColors.getColor(text, com.google.android.material.R.attr.colorPrimary))
        c.recreate()
        assertMaterialSupport(ShadowAlertDialog.getLatestDialog().findViewById(R.id.proceed_seconds))
        c.pause().stop().destroy()
    }
    @Test @Config(qualifiers = "notnight") fun `light runtime redirect colors are readable`() = assertRedirectColors()
    @Test @Config(qualifiers = "night") fun `dark runtime redirect colors are readable`() = assertRedirectColors()
    @Test fun `automatic alternative waits five seconds without unlocking`() {
        val c = screen(); val a = c.get()
        advance(4); assertNull(shadowOf(a).nextStartedActivity)
        advance(1); assertEquals("status=" + ShadowAlertDialog.getLatestDialog().findViewById<android.widget.TextView>(R.id.redirect_status).text, "com.amazon.kindle", shadowOf(a).nextStartedActivity?.component?.packageName)
        advance(10); assertNull(shadowOf(a).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `automatic redirect cannot open original blocked or own package`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        for (target in listOf("social", app.packageName, "com.amazon.kindle")) {
            val c = screen { intent ->
                intent.putExtra("redirect_package", target)
                intent.putStringArrayListExtra("redirect_blocked_packages", arrayListOf("com.amazon.kindle"))
            }
            advance(6); assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
            c.pause().stop().destroy()
        }
    }
    @Test fun `automatic missing destination has one failed attempt and never unlocks`() {
        val c = screen { it.putExtra("redirect_package", "missing.app") }
        advance(5)
        val b = ShadowAlertDialog.getLatestDialog().findViewById<Button>(R.id.btn_redirect)
        assertFalse(b.isEnabled)
        assertFalse(c.get().isFinishing)
        advance(20); assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `five second redirect is independent of fifteen second unlock delay`() {
        val c = screen { it.putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(proceedDelayInSecs = 15))) }
        val b = ShadowAlertDialog.getLatestDialog().findViewById<Button>(R.id.btn_proceed)
        assertFalse(b.isEnabled)
        advance(5)
        assertFalse(b.isEnabled)
        assertEquals("com.amazon.kindle", shadowOf(c.get()).nextStartedActivity?.component?.packageName)
        noUnlock(); c.pause().stop().destroy()
    }
    @Test fun `saving lifecycle state cancels pending launch immediately`() {
        val c = screen(); advance(2); c.saveInstanceState(android.os.Bundle()); advance(10)
        assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `failed attempt is not rearmed by recreation`() {
        val c = screen { it.putExtra("redirect_package", "missing.app") }
        advance(5); c.recreate()
        assertFalse(ShadowAlertDialog.getLatestDialog().findViewById<Button>(R.id.btn_redirect).isEnabled)
        advance(10); assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `opt out never launches automatically`() {
        val c = screen(false); advance(10); assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `pause cancels automation permanently for this screen`() {
        val c = screen(); advance(2); c.pause(); advance(10)
        assertNull(shadowOf(c.get()).nextStartedActivity)
        c.resume(); advance(10); assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `cancel returns home without unlocking or delayed launch`() {
        val c = screen(); val a = c.get()
        ShadowAlertDialog.getLatestDialog().findViewById<Button>(R.id.btn_cancel).performClick()
        assertTrue(shadowOf(a).nextStartedActivity.hasCategory(Intent.CATEGORY_HOME))
        advance(10); assertNull(shadowOf(a).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `screen off cancels countdown without launching on return`() {
        val c = screen(); advance(2)
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val power = shadowOf(app.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager)
        power.setIsInteractive(false); advance(2); power.setIsInteractive(true); advance(10)
        assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test fun `recreation never restarts automatic countdown`() {
        val c = screen(); advance(2); c.recreate(); advance(10)
        assertNull(shadowOf(c.get()).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
    @Test @Config(qualifiers = "pt-rBR") fun `Portuguese action names the app and immediate action`() {
        val c = screen(false)
        val b = ShadowAlertDialog.getLatestDialog().findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_redirect)
        assertEquals("Abrir Kindle agora", b.text.toString())
        assertEquals("Cancelar e voltar ao início", ShadowAlertDialog.getLatestDialog().findViewById<Button>(R.id.btn_cancel).text.toString())
        assertNotNull(b.icon)
        assertEquals(com.google.android.material.color.MaterialColors.getColor(c.get(), com.google.android.material.R.attr.colorPrimary, 0), b.backgroundTintList!!.getColorForState(intArrayOf(android.R.attr.state_enabled), 0))
        c.pause().stop().destroy()
    }
    @Test fun `manual alternative has readable name and only one attempt`() {
        val c = screen(); val a = c.get()
        val b = ShadowAlertDialog.getLatestDialog().findViewById<Button>(R.id.btn_redirect)
        assertTrue(b.text.toString().contains("Kindle")); assertFalse(b.text.toString().contains("com.amazon"))
        b.performClick(); advance(10)
        assertEquals("com.amazon.kindle", shadowOf(a).nextStartedActivity?.component?.packageName)
        assertNull(shadowOf(a).nextStartedActivity); noUnlock()
        c.pause().stop().destroy()
    }
}
