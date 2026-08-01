package com.juanka.uberrental

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Lee el contenido de la pantalla de Uber Driver. Cuando detecta una oferta
 * (tiene tarifa + tiempo + distancia) la analiza y muestra la tarjeta verde/roja.
 *
 * IMPORTANTE: este servicio SOLO LEE la pantalla. No toca botones ni acepta
 * viajes por ti. Aun asi, automatizar sobre Uber va contra sus terminos: usalo
 * bajo tu propio criterio.
 */
class UberAccessibilityService : AccessibilityService() {

    private lateinit var overlay: OverlayManager
    private lateinit var settings: SettingsStore

    private var lastSignature: String = ""
    private var lastShownAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayManager(this)
        settings = SettingsStore(this)
        KeepAliveService.start(this)
        Log.i(TAG, "Servicio de accesibilidad conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != UBER_DRIVER_PKG) return

        val sb = StringBuilder()
        collectAllUberText(sb)
        val text = sb.toString().trim()
        if (text.isEmpty()) return

        // Throttle: no reprocesar el mismo contenido, y minimo 800 ms entre analisis
        val now = System.currentTimeMillis()
        val signature = text.hashCode().toString()
        if (signature == lastSignature && now - lastShownAt < 3000) return

        val looksLikeOffer = hasFare(text) && hasKm(text) && hasMin(text)

        if (!looksLikeOffer) {
            if (settings.learnMode && hasFare(text)) {
                overlay.showRaw(text)
                lastSignature = signature
                lastShownAt = now
            }
            return
        }

        // Licencia: si no está activa (sin código, vencida, otro equipo...), no calculamos.
        if (!LicenseManager.status(this).active) {
            overlay.showLocked()
            lastSignature = signature
            lastShownAt = now
            return
        }

        val offer = OfferParser.parse(text)
        val result = ProfitCalculator.analyze(offer, settings.thresholdPerHour)

        if (result != null && offer.isUsable) {
            val learnText = if (settings.learnMode) offer.rawText.take(500) else null
            overlay.show(result, learnText)
            lastSignature = signature
            lastShownAt = now
            Log.i(TAG, "Oferta: $/h=${result.perHourCop} $/km=${result.perKmCop} rentable=${result.isProfitable}")
        } else if (settings.learnMode) {
            overlay.showRaw(text)
            lastSignature = signature
            lastShownAt = now
        }
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (::overlay.isInitialized) overlay.hide()
        KeepAliveService.stop(this)
        return super.onUnbind(intent)
    }

    /**
     * Lee el texto de TODAS las ventanas de Uber Driver, no solo la activa.
     * La oferta de viaje aparece en una ventana/overlay aparte, por eso antes
     * no se detectaba (solo leíamos rootInActiveWindow = el menú de inicio).
     */
    private fun collectAllUberText(sb: StringBuilder) {
        try {
            val wins = windows
            if (wins != null) {
                for (w in wins) {
                    val r = try { w.root } catch (e: Exception) { null } ?: continue
                    if (r.packageName?.toString() == UBER_DRIVER_PKG) collectText(r, sb)
                }
            }
        } catch (e: Exception) {
            // en algunos equipos windows puede fallar; caemos al método clásico
        }
        if (sb.isEmpty()) {
            rootInActiveWindow?.let { collectText(it, sb) }
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        node.text?.let { if (it.isNotBlank()) sb.append(it).append('\n') }
        node.contentDescription?.let { if (it.isNotBlank()) sb.append(it).append('\n') }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb)
        }
    }

    private fun hasFare(t: String) =
        Regex("""(?:\$|COP)\s*\d""", RegexOption.IGNORE_CASE).containsMatchIn(t)

    private fun hasKm(t: String) =
        Regex("""\d\s*km""", RegexOption.IGNORE_CASE).containsMatchIn(t)

    private fun hasMin(t: String) =
        Regex("""\d\s*min""", RegexOption.IGNORE_CASE).containsMatchIn(t)

    companion object {
        private const val TAG = "UberRental"
        private const val UBER_DRIVER_PKG = "com.ubercab.driver"
    }
}
