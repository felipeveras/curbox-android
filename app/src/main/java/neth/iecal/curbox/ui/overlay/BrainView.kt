package neth.iecal.curbox.ui.overlay

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/** Original five still drawings. No timer, bitmap allocation or continuous animation. */
class BrainView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var stage: Int = 0
        set(value) { val next = value.coerceIn(0, 4); if (field != next) { field = next; invalidate() } }
    private val ink = Color.rgb(75, 30, 55)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width / 100f, height / 90f)
        val shrinkX = when (stage) { 0 -> 1f; 1 -> .95f; 2 -> .86f; 3 -> .72f; else -> .62f }
        val shrinkY = when (stage) { 0 -> 1f; 1 -> .82f; 2 -> .62f; 3 -> .40f; else -> .18f }
        canvas.save()
        canvas.scale(shrinkX, shrinkY, 50f, 50f)
        shape.reset()
        when (stage) {
            0 -> { shape.moveTo(17f,64f); shape.cubicTo(3f,57f,6f,38f,17f,34f); shape.cubicTo(12f,18f,29f,10f,39f,15f); shape.cubicTo(47f,3f,60f,7f,65f,16f); shape.cubicTo(80f,10f,91f,23f,85f,35f); shape.cubicTo(99f,44f,93f,61f,82f,65f); shape.cubicTo(66f,77f,31f,76f,17f,64f) }
            1 -> { shape.moveTo(15f,68f); shape.cubicTo(1f,58f,8f,41f,20f,36f); shape.cubicTo(13f,20f,33f,16f,43f,23f); shape.cubicTo(54f,10f,71f,19f,73f,29f); shape.cubicTo(92f,23f,96f,44f,84f,51f); shape.cubicTo(98f,78f,66f,82f,57f,73f); shape.cubicTo(41f,83f,24f,77f,15f,68f) }
            2 -> { shape.moveTo(12f,72f); shape.cubicTo(0f,59f,14f,40f,27f,43f); shape.cubicTo(25f,23f,46f,24f,53f,37f); shape.cubicTo(69f,23f,88f,38f,83f,54f); shape.cubicTo(99f,55f,96f,72f,83f,74f); shape.cubicTo(85f,91f,72f,89f,71f,74f); shape.cubicTo(60f,83f,34f,76f,29f,73f); shape.cubicTo(26f,91f,14f,87f,18f,74f) }
            3 -> { shape.moveTo(10f,76f); shape.cubicTo(0f,65f,19f,54f,28f,56f); shape.cubicTo(26f,38f,46f,37f,53f,48f); shape.cubicTo(73f,36f,87f,51f,82f,65f); shape.cubicTo(104f,65f,99f,83f,79f,81f); shape.cubicTo(66f,91f,52f,77f,43f,82f); shape.cubicTo(28f,91f,17f,80f,10f,76f) }
            else -> { shape.moveTo(8f,76f); shape.cubicTo(-3f,63f,25f,60f,35f,65f); shape.cubicTo(45f,57f,64f,60f,68f,65f); shape.cubicTo(89f,59f,106f,73f,91f,81f); shape.cubicTo(79f,89f,64f,79f,54f,83f); shape.cubicTo(38f,91f,22f,81f,8f,76f) }
        }
        shape.close()
        paint.color = Color.rgb(248, 145, 181); paint.style = Paint.Style.FILL
        canvas.drawPath(shape, paint)
        paint.color = ink; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(shape, paint)
        if (stage < 4) {
            val dy = stage * 9f
            shape.reset(); shape.moveTo(37f, 26f + dy); shape.cubicTo(24f,27f+dy,24f,40f+dy,34f,41f+dy)
            shape.moveTo(53f,23f+dy); shape.cubicTo(44f,30f+dy,61f,33f+dy,55f,40f+dy)
            shape.moveTo(72f,35f+dy); shape.cubicTo(81f,42f+dy,67f,46f+dy,72f,51f+dy)
            canvas.drawPath(shape,paint)
        }
        canvas.restore()
        val eyeY = when(stage) { 0 -> 51f; 1 -> 55f; 2 -> 59f; 3 -> 61f; else -> 51f }
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawOval(33f,eyeY-8,45f,eyeY+6,paint); canvas.drawOval(54f,eyeY-8,66f,eyeY+6,paint)
        paint.color = ink
        canvas.drawCircle(40f,eyeY,3f,paint); canvas.drawCircle(60f,eyeY+stage*.5f,3f,paint)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=2f
        if(stage==0) canvas.drawArc(43f,56f,55f,64f,0f,180f,false,paint)
        else canvas.drawLine(46f,eyeY+8,53f,eyeY+9,paint)
        canvas.restore()
    }
}
