package com.example.pomodorotimer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

class TimerRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Float = 1f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }

    var isBreakMode: Boolean = false
        set(v) { field = v; updateColors(); invalidate() }

    var glowIntensity: Float = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }

    var isTimerRunning: Boolean = false
        set(v) { field = v; if (v) startAnims() else stopAnims() }

    private var accentColor = Color.parseColor("#F97316")
    private var accentWarm = Color.parseColor("#FFB690")
    private var accentGlow = Color.parseColor("#66F97316")

    private var breatheVal = 0f
    private var morphVal = 0f
    private var spinAngle = 0f

    private val breatheAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener { breatheVal = it.animatedValue as Float; invalidate() }
    }
    private val morphAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 8000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener { morphVal = it.animatedValue as Float }
    }
    private val spinAnim = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 20000; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { spinAngle = it.animatedValue as Float }
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init { setLayerType(LAYER_TYPE_SOFTWARE, null); updateColors() }

    private fun updateColors() {
        if (isBreakMode) {
            accentColor = Color.parseColor("#22C55E")
            accentWarm = Color.parseColor("#4ADE80")
            accentGlow = Color.parseColor("#6622C55E")
        } else {
            accentColor = Color.parseColor("#F97316")
            accentWarm = Color.parseColor("#FFB690")
            accentGlow = Color.parseColor("#66F97316")
        }
    }

    private fun startAnims() {
        if (!breatheAnim.isRunning) breatheAnim.start()
        if (!morphAnim.isRunning) morphAnim.start()
        if (!spinAnim.isRunning) spinAnim.start()
    }

    private fun stopAnims() {
        breatheAnim.cancel(); morphAnim.cancel(); spinAnim.cancel()
        breatheVal = 0f; morphVal = 0f; spinAngle = 0f; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = min(cx, cy)

        // 1 — Outer soft aura (always visible, subtle)
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.FILL
        val auraAlpha = (0.03f + breatheVal * 0.03f + glowIntensity * 0.05f)
        p.shader = RadialGradient(cx, cy, r * 1.1f,
            intArrayOf(Color.argb((auraAlpha * 255).toInt(), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), Color.TRANSPARENT),
            floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 1.1f, p)
        p.shader = null

        // 2 — Asymmetric orange wisp (rotating)
        if (isTimerRunning || glowIntensity > 0) {
            canvas.save()
            canvas.rotate(spinAngle, cx, cy)
            p.style = Paint.Style.FILL
            val wispAlpha = ((0.06f + breatheVal * 0.04f) * 255).toInt().coerceIn(0, 60)
            p.color = Color.argb(wispAlpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            p.maskFilter = BlurMaskFilter(r * 0.3f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawOval(cx - r * 0.45f, cy - r * 0.4f, cx + r * 0.35f, cy + r * 0.3f, p)
            canvas.restore()

            // White wisp (morphing, counter-rotate)
            canvas.save()
            canvas.rotate(-spinAngle * 0.7f, cx, cy)
            p.color = Color.argb((wispAlpha * 0.5f).toInt(), 255, 255, 255)
            p.maskFilter = BlurMaskFilter(r * 0.35f, BlurMaskFilter.Blur.NORMAL)
            val morphOff = morphVal * r * 0.05f
            canvas.drawOval(cx - r * 0.35f - morphOff, cy - r * 0.3f + morphOff,
                cx + r * 0.4f + morphOff, cy + r * 0.35f - morphOff, p)
            canvas.restore()
        }
        p.maskFilter = null

        // 3 — Inner circle (dark depth)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r * 0.7f,
            intArrayOf(Color.parseColor("#161616"), Color.parseColor("#080808"), Color.parseColor("#000000")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 0.7f, p)
        p.shader = null

        // 4 — Faint ring borders (ethereal)
        if (isTimerRunning || glowIntensity > 0) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 1f
            p.color = Color.argb(((0.06f + breatheVal * 0.04f) * 255).toInt().coerceIn(0, 30),
                Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            p.maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(cx, cy, r * 0.75f, p)
            p.color = Color.argb(12, 255, 255, 255)
            p.maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(cx, cy, r * 0.8f, p)
            p.maskFilter = null
        }



        // 6 — Floating particles
        if (isTimerRunning) {
            p.style = Paint.Style.FILL; p.maskFilter = null
            for (i in 0..4) {
                val a = Math.toRadians(((spinAngle * 0.8f + i * 72f) % 360f).toDouble())
                val dist = r * (0.78f + (i % 3) * 0.04f)
                val px = cx + dist * cos(a).toFloat()
                val py = cy + dist * sin(a).toFloat()
                val pa = ((0.15f + breatheVal * 0.2f) * 255).toInt().coerceIn(0, 60)
                p.color = Color.argb(pa, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                p.maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawCircle(px, py, 2f + (i % 2), p)
            }
            p.maskFilter = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breatheAnim.cancel(); morphAnim.cancel(); spinAnim.cancel()
    }
}
