package neth.iecal.curbox.ui.activity

import android.app.Application
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import neth.iecal.curbox.R
import neth.iecal.curbox.ui.fragments.main.reducers.blockertools.appBlocker.CreateAppGroupFragment
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RedirectSettingsTest {
    private fun assertEditorAndPickerTheme() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Curbox)
        val activity = controller.setup().get()
        val fragment = CreateAppGroupFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        val primary = com.google.android.material.color.MaterialColors.getColor(activity, com.google.android.material.R.attr.colorPrimary, 0)
        val button = fragment.requireView().findViewById<Button>(R.id.btn_redirect_app)
        assertEquals(primary, com.google.android.material.color.MaterialColors.getColor(button, com.google.android.material.R.attr.colorPrimary))
        button.performClick()
        val search = ShadowAlertDialog.getLatestDialog().findViewById<android.widget.EditText>(R.id.redirect_search)
        assertEquals(primary, com.google.android.material.color.MaterialColors.getColor(search, com.google.android.material.R.attr.colorPrimary))
        ShadowAlertDialog.getLatestDialog().dismiss()
        controller.pause().stop().destroy()
    }
    @Test @Config(qualifiers = "notnight") fun `editor and picker use original light Material palette`() = assertEditorAndPickerTheme()
    @Test @Config(qualifiers = "night") fun `editor and picker use original dark Material palette`() = assertEditorAndPickerTheme()

    @Test fun `picker selection is saved into the group settings`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Curbox)
        val activity = controller.setup().get()
        activity.intent.putExtra("prefill_package", "social")
        val launch = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val info = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.amazon.kindle"
                name = "Reader"
                nonLocalizedLabel = "Kindle"
                applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = "com.amazon.kindle" }
            }
        }
        info.activityInfo.applicationInfo.nonLocalizedLabel = "Kindle"
        org.robolectric.Shadows.shadowOf(activity.packageManager).installPackage(android.content.pm.PackageInfo().apply {
            packageName = "com.amazon.kindle"; applicationInfo = info.activityInfo.applicationInfo
        })
        org.robolectric.Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(launch, info)
        val fragment = CreateAppGroupFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        fragment.requireView().findViewById<android.widget.EditText>(R.id.et_group_name).setText("Reading")
        fragment.requireView().findViewById<Button>(R.id.btn_redirect_app).performClick()
        val dialog = ShadowAlertDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        dialog.findViewById<android.widget.ListView>(R.id.redirect_apps)!!.performItemClick(dialog.findViewById<android.widget.ListView>(R.id.redirect_apps)!!.getChildAt(1), 1, 1)
        assertTrue(fragment.requireView().findViewById<Button>(R.id.btn_redirect_app).text.toString().contains("Kindle"))
        val autoId = activity.resources.getIdentifier("switch_redirect_auto", "id", activity.packageName)
        assertNotEquals("Editor must expose opt in", 0, autoId)
        val auto = fragment.requireView().findViewById<android.widget.CompoundButton>(autoId)
        assertFalse(auto.isChecked)
        auto.performClick()
        fragment.requireView().findViewById<android.view.View>(R.id.fab_save_group).performClick()
        val manager = neth.iecal.curbox.utils.DataStoreManager(activity)
        var saved: neth.iecal.curbox.data.models.AppGroup? = null
        repeat(100) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            saved = kotlinx.coroutines.runBlocking { manager.settings.first().blockedAppGroups.firstOrNull { it.name == "Reading" } }
            if (saved != null) return@repeat
            Thread.sleep(10)
        }
        assertNotNull("The group was persisted", saved)
        assertEquals("com.amazon.kindle", saved!!.redirectPackage)
        assertTrue(saved!!.redirectAfter5Seconds)
        controller.pause().stop().destroy()
    }

    @Test fun `alternative search matches name and package and keeps selection stable`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Curbox)
        val activity = controller.setup().get()
        fun choice(pkg: String, name: String) = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = pkg; this.name = "Main"; nonLocalizedLabel = name
                applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = name }
            }
        }
        var selected: String? = null
        RedirectAppPicker.show(activity, listOf(choice("com.amazon.kindle", "Kindle"), choice("audio.app", "Audio")), null) { selected = it }
        val dialog = ShadowAlertDialog.getLatestDialog()
        val search = dialog.findViewById<android.widget.EditText>(R.id.redirect_search)
        val list = dialog.findViewById<android.widget.ListView>(R.id.redirect_apps)
        search.setText("  KINDLE  ")
        assertEquals(1, list.adapter.count)
        val row = list.adapter.getView(0, null, list)
        assertEquals("Kindle", row.findViewById<android.widget.TextView>(R.id.redirect_choice_name).text.toString())
        assertEquals("com.amazon.kindle", row.findViewById<android.widget.TextView>(R.id.redirect_choice_package).text.toString())
        search.setText("amazon")
        assertEquals(1, list.adapter.count)
        list.performItemClick(row, 0, 0)
        assertEquals("com.amazon.kindle", selected)
        controller.pause().stop().destroy()
    }

    @Test fun `alternative picker exposes search with empty results`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Curbox)
        val activity = controller.setup().get()
        val fragment = CreateAppGroupFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        fragment.requireView().findViewById<Button>(R.id.btn_redirect_app).performClick()
        val dialog = ShadowAlertDialog.getLatestDialog()
        val id = activity.resources.getIdentifier("redirect_search", "id", activity.packageName)
        assertNotEquals("Search must be present", 0, id)
        val search = dialog.findViewById<android.widget.EditText>(id)
        search.setText("no such app")
        val listId = activity.resources.getIdentifier("redirect_apps", "id", activity.packageName)
        assertEquals(0, dialog.findViewById<android.widget.ListView>(listId).adapter.count)
        controller.pause().stop().destroy()
    }

    @Test fun `new group has an optional app picker disabled by default`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.Theme_Curbox)
        val activity = controller.setup().get()
        val fragment = CreateAppGroupFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment).commitNow()
        val id = activity.resources.getIdentifier("btn_redirect_app", "id", activity.packageName)
        val button = fragment.requireView().findViewById<Button>(id)
        assertNotNull("Group editor must expose optional destination", button)
        assertTrue(button.text.toString().contains("Off"))
        button.performClick()
        val dialog = ShadowAlertDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        assertNotNull(dialog.findViewById<android.widget.ListView>(R.id.redirect_apps))
        assertEquals("Off", dialog.findViewById<android.widget.ListView>(R.id.redirect_apps)!!.adapter.getItem(0))
        dialog.dismiss()
        controller.pause().stop().destroy()
    }
}
