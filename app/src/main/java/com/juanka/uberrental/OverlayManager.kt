package com.juanka.uberrental

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Dibuja la tarjeta verde/roja encima de Uber Driver usando WindowManager.
 * La tarjeta se puede arrastrar y desaparece sola despues de unos segundos.
 */
class OverlayManager(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null
    private val hideRunnable = Runnable { hide() }

    /** Muestra el resultado. profitable -> verde, si no -> rojo. */
    fun show(result: ProfitResult, learnText: String?) {
        handler.removeCallbacks(hideRunnable)
        ensureView()
        val v = view ?: return

        val card = v.findViewById<View>(R.id.overlayCard)
        val title = v.findViewById<TextView>(R.id.overlayTitle)
        val perHour = v.findViewById<TextView>(R.id.overlayPerHour)
        val perKm = v.findViewById<TextView>(R.id.overlayPerKm)
        val detail = v.findViewById<TextView>(R.id.overlayDetail)
        val learn = v.findViewById<TextView>(R.id.overlayLearn)

        val bg = if (result.isProfitable) R.drawable.bg_card_green else R.drawable.bg_card_red
        card.setBackgroundResource(bg)

        title.text = if (result.isProfitable)
            context.getString(R.string.overlay_take)
        else
            context.getString(R.string.overlay_skip)

        perHour.text = context.getString(
            R.string.overlay_per_hour, Formatting.money(result.perHourCop)
        )
        perKm.text = context.getString(
            R.string.overlay_per_km, Formatting.money(result.perKmCop)
        )
        detail.text = context.getString(
            R.string.overlay_detail,
            Formatting.money(result.fareCop),
            Formatting.minutes(result.totalMinutes),
            Formatting.km(result.totalKm)
        )

        if (!learnText.isNullOrBlank()) {
            learn.visibility = View.VISIBLE
            learn.text = learnText
        } else {
            learn.visibility = View.GONE
        }

        // Auto-ocultar despues de 15 s
        handler.postDelayed(hideRunnable, 15_000)
    }

    /** Muestra solo el texto crudo leido (modo aprendizaje sin oferta valida). */
    fun showRaw(rawText: String) {
        handler.removeCallbacks(hideRunnable)
        ensureView()
        val v = view ?: return
        v.findViewById<View>(R.id.overlayCard).setBackgroundResource(R.drawable.bg_card_neutral)
        v.findViewById<TextView>(R.id.overlayTitle).text =
            context.getString(R.string.overlay_learn_title)
        v.findViewById<TextView>(R.id.overlayPerHour).text = ""
        v.findViewById<TextView>(R.id.overlayPerKm).text = ""
        v.findViewById<TextView>(R.id.overlayDetail).text = ""
        val learn = v.findViewById<TextView>(R.id.overlayLearn)
        learn.visibility = View.VISIBLE
        learn.text = rawText.take(600)
        handler.postDelayed(hideRunnable, 12_000)
    }

    /** Tarjeta neutra cuando la licencia no está activa. */
    fun showLocked() {
        handler.removeCallbacks(hideRunnable)
        ensureView()
        val v = view ?: return
        v.findViewById<View>(R.id.overlayCard).setBackgroundResource(R.drawable.bg_card_neutral)
        v.findViewById<TextView>(R.id.overlayTitle).text =
            context.getString(R.string.overlay_locked_title)
        v.findViewById<TextView>(R.id.overlayPerHour).text = ""
        v.findViewById<TextView>(R.id.overlayPerKm).text = ""
        v.findViewById<TextView>(R.id.overlayDetail).text =
            context.getString(R.string.overlay_locked_sub)
        v.findViewById<TextView>(R.id.overlayLearn).visibility = View.GONE
        handler.postDelayed(hideRunnable, 8_000)
    }

    fun hide() {
        handler.removeCallbacks(hideRunnable)
        view?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        view = null
    }

    private fun ensureView() {
        if (view != null) return
        val inflated = LayoutInflater.from(context).inflate(R.layout.overlay_card, null)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = 120
        makeDraggable(inflated, lp)
        try {
            windowManager.addView(inflated, lp)
            view = inflated
            params = lp
        } catch (_: Exception) {
            view = null
        }
    }

    private fun makeDraggable(v: View, lp: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        v.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = initialX + (event.rawX - touchX).toInt()
                    lp.y = initialY + (event.rawY - touchY).toInt()
                    try {
                        windowManager.updateViewLayout(v, lp)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Toque corto = ocultar
                    val moved = Math.abs(event.rawX - touchX) + Math.abs(event.rawY - touchY)
                    if (moved < 10) hide()
                    true
                }
                else -> false
            }
        }
    }
}
