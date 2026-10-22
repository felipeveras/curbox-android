package neth.iecal.curbox.ui.activity

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import neth.iecal.curbox.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class BlockedAppsSearchTest {
    @Test fun `blocked app search matches package as well as human name and displays both`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val ai = ApplicationInfo().apply { packageName = "com.social.reader"; nonLocalizedLabel = "Reader" }
        shadowOf(app.packageManager).installPackage(PackageInfo().apply { packageName = ai.packageName; applicationInfo = ai })
        val c = Robolectric.buildActivity(SelectAppsActivity::class.java, Intent(app, SelectAppsActivity::class.java)
            .putStringArrayListExtra("APP_LIST", arrayListOf(ai.packageName))).setup()
        val a = c.get()
        val search = a.findViewById<SearchView>(R.id.searchView)
        val list = a.findViewById<RecyclerView>(R.id.app_list)
        search.setQuery("com.social", false)
        assertEquals(1, list.adapter!!.itemCount)
        val holder = list.adapter!!.createViewHolder(list, 0)
        @Suppress("UNCHECKED_CAST")
        (list.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>).onBindViewHolder(holder, 0)
        assertEquals("Reader", holder.itemView.findViewById<TextView>(R.id.app_name).text.toString())
        val id = a.resources.getIdentifier("app_package", "id", a.packageName)
        assertNotEquals("Package must be secondary", 0, id)
        assertEquals(ai.packageName, holder.itemView.findViewById<TextView>(id).text.toString())
        search.setQuery("reader", false); assertEquals(1, list.adapter!!.itemCount)
        search.setQuery("no match", false); assertEquals(0, list.adapter!!.itemCount)
        c.pause().stop().destroy()
    }
}
