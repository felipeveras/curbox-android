package neth.iecal.curbox.utils

import android.content.Context
import android.content.pm.PackageManager
import neth.iecal.curbox.R

object RedirectApps {
    fun label(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            .takeIf { it.isNotBlank() && it != packageName }
            ?: context.getString(R.string.redirect_selected_app)
    } catch (_: PackageManager.NameNotFoundException) {
        context.getString(R.string.redirect_selected_app)
    }
}
