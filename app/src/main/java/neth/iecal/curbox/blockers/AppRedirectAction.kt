package neth.iecal.curbox.blockers

/** A user initiated alternative, never an unlock or an accessibility event redirect. */
class AppRedirectAction(
    private val target: String?,
    private val hostPackage: String,
    private val blockedPackages: Set<String>
) {
    private var attempted = false

    fun open(launch: (String) -> Boolean): Boolean {
        if (attempted || target.isNullOrBlank() || target == hostPackage || target in blockedPackages) return false
        attempted = true
        return try {
            launch(target)
        } catch (_: RuntimeException) {
            false
        }
    }
}
