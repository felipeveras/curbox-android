package neth.iecal.curbox.data.models

/** Persistent display session, independent from the daily analytics count. */
class BrainCounterState(var count: Int = 0, var lastSeen: Long = 0, var outsideSince: Long = 0) {
    fun observe(now: Long, day: String, active: Boolean) {
        val anchor = if (outsideSince > 0) outsideSince else lastSeen
        if (anchor > 0 && now >= anchor && now - anchor >= 300_000) count = 0
        // A clock correction must not produce negative durations or trap a session in the future.
        if (now < lastSeen || now < outsideSince) {
            lastSeen = now
            outsideSince = if (active) 0 else now
        }
        if (active) { lastSeen = now; outsideSince = 0 }
        else if (outsideSince == 0L) outsideSince = now
    }
    fun increment() { if (count < Int.MAX_VALUE) count++ }
}
