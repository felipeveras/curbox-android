package neth.iecal.curbox.data.models

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class AppRedirectTest {
    @Test fun `older backups leave redirects disabled and keep group restrictions`() {
        val group = Gson().fromJson("""{"id":"old","isActive":true,"selectedPackages":["social"]}""", AppGroup::class.java)
        assertNull(group.redirectPackage)
        assertTrue(group.isActive)
        assertEquals(listOf("social"), group.selectedPackages)
        assertNull(AppGroup().redirectPackage)
    }

    @Test fun `group retains chosen redirect app through backup JSON`() {
        val gson = Gson()
        val group = gson.fromJson("""{"id":"reading","redirectPackage":"com.amazon.kindle"}""", AppGroup::class.java)
        val restored = gson.toJsonTree(gson.fromJson(gson.toJson(group), AppGroup::class.java)).asJsonObject
        assertEquals("com.amazon.kindle", restored.get("redirectPackage")?.asString)
    }
}
