package neth.iecal.curbox.data.models

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class AutoRedirectPersistenceTest {
    @Test fun `explicit auto redirect opt in survives settings serialization`() {
        val gson = Gson()
        val group = gson.fromJson("""{"redirectPackage":"com.amazon.kindle","redirectAfter5Seconds":true}""", AppGroup::class.java)
        assertTrue(gson.toJson(group).contains("\"redirectAfter5Seconds\":true"))
    }
    @Test fun `old groups never silently opt in`() {
        val gson = Gson()
        val group = gson.fromJson("""{"redirectPackage":"com.amazon.kindle"}""", AppGroup::class.java)
        assertFalse(gson.toJson(group).contains("\"redirectAfter5Seconds\":true"))
    }
}
