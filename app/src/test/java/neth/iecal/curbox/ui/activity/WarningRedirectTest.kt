package neth.iecal.curbox.ui.activity

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.view.View
import android.widget.Button
import com.google.gson.Gson
import neth.iecal.curbox.Constants
import neth.iecal.curbox.data.models.AppBlockerWarningScreenConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WarningRedirectTest {
    @Test fun `missing app leaves warning open and does not unlock`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val intent = Intent(app, WarningActivity::class.java)
            .putExtra("mode", Constants.WARNING_SCREEN_MODE_APP_BLOCKER)
            .putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(isProceedDisabled = true)))
            .putExtra("redirect_package", "missing.app")
        val activity = Robolectric.buildActivity(WarningActivity::class.java, intent).setup().get()
        val dialog = ShadowAlertDialog.getLatestDialog()
        val button = dialog.findViewById<Button>(neth.iecal.curbox.R.id.btn_redirect)
        button.performClick()
        assertFalse(activity.isFinishing)
        assertTrue(dialog.isShowing)
        assertFalse(button.isEnabled)
        assertNull(shadowOf(activity).nextStartedActivity)
        assertEquals(activity.getString(neth.iecal.curbox.R.string.redirect_unavailable), org.robolectric.shadows.ShadowToast.getTextOfLatestToast())
        assertTrue(shadowOf(app).broadcastIntents.none { it.action?.endsWith("appblocker.cooldown") == true })
    }

    @Test fun `unconfigured block screen has no redirect button`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val intent = Intent(app, WarningActivity::class.java)
            .putExtra("mode", Constants.WARNING_SCREEN_MODE_APP_BLOCKER)
            .putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig()))
        Robolectric.buildActivity(WarningActivity::class.java, intent).setup()
        val button = ShadowAlertDialog.getLatestDialog().findViewById<Button>(neth.iecal.curbox.R.id.btn_redirect)
        assertEquals(View.GONE, button.visibility)
    }

    @Test fun `blocked screen offers installed alternative without granting a cooldown`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_INFO).setPackage("com.amazon.kindle")
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.amazon.kindle"
                name = "ReaderActivity"
                applicationInfo = ApplicationInfo().apply { packageName = "com.amazon.kindle" }
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(launchIntent, info)
        val intent = Intent(app, WarningActivity::class.java)
            .putExtra("mode", Constants.WARNING_SCREEN_MODE_APP_BLOCKER)
            .putExtra("warning_config", Gson().toJson(AppBlockerWarningScreenConfig(isProceedDisabled = true)))
            .putExtra("redirect_package", "com.amazon.kindle")
            .putExtra("launch_package", "social")
        val activity = Robolectric.buildActivity(WarningActivity::class.java, intent).setup().get()
        val dialog = ShadowAlertDialog.getLatestDialog()
        val id = activity.resources.getIdentifier("btn_redirect", "id", activity.packageName)
        val button = dialog.findViewById<Button>(id)
        assertNotNull("A separate redirect button must exist", button)
        assertEquals(View.VISIBLE, button.visibility)
        assertFalse("A missing label must not expose a package as the app name", button.text.toString().contains("com.amazon.kindle"))
        button.performClick()
        assertEquals("com.amazon.kindle", shadowOf(activity).nextStartedActivity.component?.packageName)
        val unlocks = shadowOf(app).broadcastIntents.filter { it.action?.endsWith("appblocker.cooldown") == true }
        assertTrue("Redirect must not grant access to the blocked group", unlocks.isEmpty())
    }
}
