package neth.iecal.curbox.trackers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.LruCache
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import neth.iecal.curbox.data.models.BrainCounterState
import neth.iecal.curbox.CrashLogger
import neth.iecal.curbox.data.db.AppDatabase
import neth.iecal.curbox.data.db.ReelStatsDao
import neth.iecal.curbox.data.db.ReelStatsEntity
import neth.iecal.curbox.data.models.ReelCounterOverlayConfig
import neth.iecal.curbox.hardcoded.ReelAppConfig.Companion.reelData
import neth.iecal.curbox.services.BaseBlockingService
import neth.iecal.curbox.ui.overlay.ReelsOverlayManager
import neth.iecal.curbox.utils.TimeTools



class ReelsCountTracker {

    companion object {
        const val INTENT_ACTION_REFRESH_REEL_COUNTER = "neth.iecal.curbox.refresh.reel_counter"
    }

    private lateinit var service: BaseBlockingService
    private lateinit var overlayManager: ReelsOverlayManager
    private lateinit var reelStatsDao: ReelStatsDao
    private lateinit var crashLogger: CrashLogger

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        if (error !is CancellationException && ::crashLogger.isInitialized) crashLogger.logNonFatalError(if (error is Exception) error else RuntimeException(error))
    })
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isOnDisplayCounter = true
    private var overlayConfig = ReelCounterOverlayConfig()
    private var todayCount = 0
    private var lastDateStr = TimeTools.getCurrentDate()
    private var reelCountedCallback: (String) -> Unit = {}


    private val lastDynamicText = mutableMapOf<String, String>()
    private val seenReelsCache = mutableMapOf<String, LruCache<String, Boolean>>()

    private var ignored = listOf<String>()
    private var initialized = false
    private lateinit var session: BrainCounterState
    private lateinit var sessionPrefs: android.content.SharedPreferences
    private var activeShortVideo = false
    private var lastActivity = 0L
    private val stateLock = Any()

    private fun displayCount() = synchronized(stateLock) { if (overlayConfig.dailyMode) todayCount else session.count }
    private fun saveSession() {
        sessionPrefs.edit().putInt("count", session.count).putLong("last", session.lastSeen)
            .putLong("outside", session.outsideSince).commit()
    }
    private fun updateDay() {
        val date = TimeTools.getCurrentDate()
        if (date != lastDateStr) {
            todayCount = runBlocking(Dispatchers.IO) { reelStatsDao.getCount(date) ?: 0 }
            lastDateStr = date
        }
    }
    private val heartbeat = object : Runnable {
        override fun run() {
            scope.launch {
                synchronized(stateLock) {
                    updateDay()
                    val now = System.currentTimeMillis()
                    if (now - lastActivity >= 300_000 || now < lastActivity) activeShortVideo = false
                    if (!activeShortVideo) session.observe(now, lastDateStr, false)
                    saveSession()
                    val count = displayCount()
                    postOverlayUpdate {
                        overlayManager.reelsScrolledThisSession = count
                        overlayManager.setCounterVisible(activeShortVideo && shouldShowCounter())
                    }
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }
    fun setup(
        service: BaseBlockingService,
        overlayManager: ReelsOverlayManager,
        onReelCounted: (String) -> Unit
    ) {
        if (initialized) return
        initialized = true
        this.service = service
        this.overlayManager = overlayManager
        crashLogger = CrashLogger(service)
        reelCountedCallback = onReelCounted

        ignored = listOf("com.android.systemui",
            service.packageName,
            "com.google.android.apps.wellbeing")
        val db = AppDatabase.getInstance(service)
        this.reelStatsDao = db.reelStatsDao()
        sessionPrefs = service.getSharedPreferences("brain_counter_session", Context.MODE_PRIVATE)
        session = BrainCounterState(sessionPrefs.getInt("count", 0), sessionPrefs.getLong("last", 0), sessionPrefs.getLong("outside", 0))
        if (session.outsideSince == 0L) session.outsideSince = session.lastSeen
        sessionPrefs.all.filterKeys { it.startsWith("comparator:") }.forEach { (key, value) ->
            if (value is String) lastDynamicText[key.removePrefix("comparator:")] = value
        }
        sessionPrefs.all.filterKeys { it.startsWith("seen:") }.forEach { (key, value) ->
            if (value is Set<*>) {
                val cache = LruCache<String, Boolean>(50)
                value.filterIsInstance<String>().takeLast(50).forEach { cache.put(it, true) }
                seenReelsCache[key.removePrefix("seen:")] = cache
            }
        }
        lastDateStr = TimeTools.getCurrentDate()
        todayCount = runBlocking(Dispatchers.IO) { reelStatsDao.getCount(lastDateStr) ?: 0 }
        overlayManager.onPositionChanged = { x, y ->
            scope.launch { service.dataStoreManager.updateReelCounterOverlayConfig(overlayConfig.copy(positionX = x, positionY = y)) }
        }
        mainHandler.post(heartbeat)

        scope.launch {
            service.dataStoreManager.settings.collectLatest { settings ->
                synchronized(stateLock) {
                    isOnDisplayCounter = settings.isReelCounterOn
                    overlayConfig = settings.reelCounterOverlayConfig
                }
                postOverlayUpdate {
                    overlayManager.reelsScrolledThisSession = displayCount()
                    overlayManager.setCounterVisible(activeShortVideo && shouldShowCounter())
                }
            }
        }

    }

    fun onEvent(event: AccessibilityEvent?, dynamicComparator: String?) = synchronized(stateLock) {
        try {
            val pkg = event?.packageName?.toString() ?: return
            if (ignored.contains(pkg)) return
            updateDay()
            val interactive = (service.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isInteractive
            activeShortVideo = reelData[pkg] != null && dynamicComparator != null && interactive
            val now = System.currentTimeMillis()
            if (activeShortVideo) lastActivity = now
            session.observe(now, lastDateStr, activeShortVideo)
            saveSession()
            if (!interactive) { hideReelCounter(); return }

            val data = reelData[pkg]

            if (data != null) {
                if (dynamicComparator == null) {
                    lastDynamicText.remove(pkg)
                    sessionPrefs.edit().remove("comparator:$pkg").commit()
                    postOverlayUpdate { overlayManager.removeOverlay() }
                    return
                }
                if ((event.eventType and data.eventType) == 0) return
                if (Settings.canDrawOverlays(service) && !overlayManager.isOverlayVisible) {
                    postOverlayUpdate {
                        // The event may have been superseded while this render waited on main.
                        if (activeShortVideo && !overlayManager.isOverlayVisible) {
                            overlayManager.reelsScrolledThisSession = displayCount()
                            overlayManager.startDisplaying(overlayConfig, shouldShowCounter())
                        }
                    }
                }

                checkForReelProgression(
                    pkg,
                    dynamicComparator,
                    data.deduplicateComparators,
                    data.initialComparator
                )
            } else if (overlayManager.isOverlayVisible) {
                postOverlayUpdate { overlayManager.removeOverlay() }
                return
            }


        } catch (error: Exception) {
            crashLogger.logNonFatalError(error)
        }
    }

    private fun checkForReelProgression(
        pkg: String,
        currentText: String,
        deduplicateComparators: Boolean,
        initialComparator: String?
    ) {
        if (currentText.trim().isBlank()) {
            if (!initialComparator.isNullOrBlank() && lastDynamicText[pkg].isNullOrEmpty()) {
                lastDynamicText[pkg] = initialComparator
            }
            return
        }

        val previousText = lastDynamicText[pkg] ?: ""
        if (currentText != previousText) {
            val isSubstantialChange = isSubstantialTextChange(currentText, previousText)

            if (previousText.isNotEmpty() && isSubstantialChange) {
                if (!deduplicateComparators) {
                    onReelCounted(pkg)
                } else {
                    val appCache = seenReelsCache.getOrPut(pkg) { LruCache(50) }
                    if (appCache.get(currentText) == null) {
                        onReelCounted(pkg)
                        appCache.put(currentText, true)
                        sessionPrefs.edit().putStringSet("seen:$pkg", appCache.snapshot().keys.toSet()).commit()
                    }
                }
            }
            
            if (isSubstantialChange || currentText.length > previousText.length) {
                lastDynamicText[pkg] = currentText
                sessionPrefs.edit().putString("comparator:$pkg", currentText).commit()
            }
        }
    }

    fun getTodayCount(): Int = todayCount

    private fun onReelCounted(packageName: String) {
        updateDay()
        val date = lastDateStr
        todayCount++
        session.increment()
        saveSession()
        reelCountedCallback(packageName)

        postOverlayUpdate {
            val shouldShow = shouldShowCounter()
            if (shouldShow) {
                overlayManager.reelsScrolledThisSession = displayCount()
            }
            overlayManager.setCounterVisible(shouldShow)
        }

        // Serialize the existing daily analytics write; never let an older async snapshot win.
        runBlocking(Dispatchers.IO) { reelStatsDao.upsert(ReelStatsEntity(date = date, count = todayCount)) }

        service.lastBackPressTimeStamp = SystemClock.uptimeMillis()
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> synchronized(stateLock) {
                    activeShortVideo = false
                    session.observe(System.currentTimeMillis(), lastDateStr, false)
                    saveSession()
                    hideReelCounter()
                }
                INTENT_ACTION_REFRESH_REEL_COUNTER -> { /* Settings are already collected once. */ }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun setupReceivers() {
        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_REEL_COUNTER)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            service.registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            service.registerReceiver(refreshReceiver, filter)
        }
    }

    fun onDestroy() {
        mainHandler.removeCallbacks(heartbeat)
        scope.cancel()
        postOverlayUpdate {
            overlayManager.removeOverlay()
            overlayManager.binding = null
        }
        try { service.unregisterReceiver(refreshReceiver) } catch (_: Exception) {}
    }

    private fun hideReelCounter() {
        postOverlayUpdate { overlayManager.setCounterVisible(false) }
    }

    private fun shouldShowCounter(): Boolean =
        isOnDisplayCounter && overlayConfig.shouldShowAtCount(displayCount())

    private fun postOverlayUpdate(block: () -> Unit) {
        mainHandler.post {
            try {
                block()
            } catch (error: Exception) {
                crashLogger.logNonFatalError(error)
            }
        }
    }

    private fun isSubstantialTextChange(currentText: String, previousText: String): Boolean {
        if (currentText.isEmpty() || previousText.isEmpty()) return true

        fun countWords(text: String, wordCounts: HashMap<String, Int>) {
            val len = text.length
            var start = -1
            for (i in 0 until len) {
                if (text[i].isWhitespace()) {
                    if (start != -1) {
                        val word = text.substring(start, i)
                        wordCounts[word] = wordCounts.getOrDefault(word, 0) + 1
                        start = -1
                    }
                } else {
                    if (start == -1) start = i
                }
            }
            if (start != -1) {
                val word = text.substring(start, len)
                wordCounts[word] = wordCounts.getOrDefault(word, 0) + 1
            }
        }

        val currentWords = HashMap<String, Int>()
        val previousWords = HashMap<String, Int>()
        
        countWords(currentText, currentWords)
        countWords(previousText, previousWords)

        if (currentWords.isEmpty() || previousWords.isEmpty()) return true

        var intersectionSize = 0
        var totalSmaller = 0
        
        val smallerMap = if (currentWords.size < previousWords.size) currentWords else previousWords
        val largerMap = if (currentWords.size < previousWords.size) previousWords else currentWords

        for ((word, count) in smallerMap) {
            totalSmaller += count
            val largerCount = largerMap[word] ?: 0
            intersectionSize += minOf(count, largerCount)
        }

        if (totalSmaller == 0) return true

        val overlapRatio = intersectionSize.toFloat() / totalSmaller
        return overlapRatio < 0.90f
    }
}
