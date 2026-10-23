package neth.iecal.curbox.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.view.WindowManager.LayoutParams
import neth.iecal.curbox.R
import neth.iecal.curbox.data.models.ReelCounterOverlayConfig
import neth.iecal.curbox.databinding.OverlayBrainCounterBinding

class ReelsOverlayManager(private val context: Context) {
    private var overlayView: View? = null
    var binding: OverlayBrainCounterBinding? = null
    var isOverlayVisible = false
    private var windowManager: WindowManager? = null
    var onPositionChanged: (Float, Float) -> Unit = { _, _ -> }
    var reelsScrolledThisSession = 0
        set(value) { field = value.coerceAtLeast(0); renderCount() }

    fun renderCount() {
        val count = reelsScrolledThisSession
        val stage = when { count < 3 -> 0; count < 6 -> 1; count < 10 -> 2; count < 15 -> 3; else -> 4 }
        binding?.brain?.stage = stage
        binding?.reelCounter?.text = count.toString()
        val names = context.resources.getStringArray(R.array.brain_stages)
        binding?.root?.contentDescription = context.getString(R.string.brain_counter_description, count, names[stage])
    }

    @SuppressLint("InlinedApi", "ClickableViewAccessibility")
    fun startDisplaying(config: ReelCounterOverlayConfig = ReelCounterOverlayConfig(), isVisible: Boolean = true) {
        if (overlayView != null || isOverlayVisible) return
        binding = OverlayBrainCounterBinding.inflate(LayoutInflater.from(context))
        overlayView = binding?.root
        isOverlayVisible = true
        renderCount()
        setCounterVisible(isVisible)
        val dm = context.resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth * config.positionX).toInt()
            y = (screenHeight * config.positionY).toInt()
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(overlayView, params)
        var downX = 0f; var downY = 0f; var initialX = 0; var initialY = 0
        binding?.dragHandle?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    initialX = params.x; initialY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (initialX + event.rawX - downX).toInt().coerceIn(0, (screenWidth - (overlayView?.width ?: 0)).coerceAtLeast(0))
                    params.y = (initialY + event.rawY - downY).toInt().coerceIn(0, (screenHeight - (overlayView?.height ?: 0)).coerceAtLeast(0))
                    windowManager?.updateViewLayout(overlayView, params)
                }
                MotionEvent.ACTION_UP -> onPositionChanged(
                    (params.x + (overlayView?.width ?: 0) / 2f) / screenWidth,
                    (params.y + (overlayView?.height ?: 0) / 2f) / screenHeight)
            }
            true // Consume the entire gesture, never click video controls underneath.
        }
        overlayView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                overlayView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                val vw = overlayView?.width ?: 0; val vh = overlayView?.height ?: 0
                params.x = (screenWidth * config.positionX - vw / 2f).toInt().coerceIn(0, (screenWidth - vw).coerceAtLeast(0))
                params.y = (screenHeight * config.positionY - vh / 2f).toInt().coerceIn(0, (screenHeight - vh).coerceAtLeast(0))
                try { windowManager?.updateViewLayout(overlayView, params) } catch (_: Exception) { }
            }
        })
    }
    fun setCounterVisible(isVisible: Boolean) {
        binding?.root?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
    fun removeOverlay() {
        if (overlayView != null && windowManager != null) windowManager?.removeView(overlayView)
        overlayView = null; binding = null; isOverlayVisible = false
    }
}
