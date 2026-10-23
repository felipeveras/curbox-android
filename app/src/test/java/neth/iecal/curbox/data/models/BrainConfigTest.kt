package neth.iecal.curbox.data.models

import android.app.Application
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import neth.iecal.curbox.R
import neth.iecal.curbox.ui.fragments.main.reducers.anti_stimulants.reel_counter.ReelCounterFragment
import neth.iecal.curbox.utils.DataStoreManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class BrainConfigTest {
    @Test fun oldJsonDefaultsToSessionAndExplicitModeRoundTrips() {
        assertFalse(Gson().fromJson("{}",ReelCounterOverlayConfig::class.java).dailyMode)
        assertTrue(Gson().fromJson(Gson().toJson(ReelCounterOverlayConfig(dailyMode=true)),ReelCounterOverlayConfig::class.java).dailyMode)
    }
    @Test @Config(qualifiers="pt-rBR-night") fun portugueseModePersistsAcrossRecreation() = exercise("Contar por dia")
    @Test @Config(qualifiers="en-rUS-notnight") fun englishModePersistsAcrossRecreation() = exercise("Count per day")
    private fun exercise(label: String) {
        org.robolectric.Shadows.shadowOf(org.robolectric.RuntimeEnvironment.getApplication()).declareComponentUnbindable(
            android.content.ComponentName(org.robolectric.RuntimeEnvironment.getApplication(), androidx.room.MultiInstanceInvalidationService::class.java))
        val c=Robolectric.buildActivity(AppCompatActivity::class.java)
        c.get().setTheme(R.style.Theme_Curbox)
        val a=c.setup().get()
        try {
        val manager=DataStoreManager(a)
        runBlocking { manager.updateReelCounterOverlayConfig(ReelCounterOverlayConfig()) }
        val f=ReelCounterFragment()
        a.supportFragmentManager.beginTransaction().add(android.R.id.content,f).commitNow()
        val toggle=f.requireView().findViewById<MaterialSwitch>(R.id.switchBrainDaily)
        assertEquals(label,toggle.text.toString())
        assertFalse(toggle.isChecked)
        toggle.performClick()
        var saved=false
        repeat(100) {
            shadowOf(Looper.getMainLooper()).idle()
            saved=runBlocking { manager.settings.first().reelCounterOverlayConfig.dailyMode }
            if(!saved) Thread.sleep(10)
        }
        assertTrue("Mode must reach real multiprocess DataStore",saved)
        a.supportFragmentManager.beginTransaction().remove(f).commitNow()
        val recreated=ReelCounterFragment()
        a.supportFragmentManager.beginTransaction().add(android.R.id.content,recreated).commitNow()
        repeat(30) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertTrue(recreated.requireView().findViewById<MaterialSwitch>(R.id.switchBrainDaily).isChecked)
        } finally { c.pause().stop().destroy() }
    }
}
