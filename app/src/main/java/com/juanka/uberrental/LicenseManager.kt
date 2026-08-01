package com.juanka.uberrental

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Sistema de licencias OFFLINE con código firmado.
 *
 * - El vendedor genera un código con su LLAVE PRIVADA (fuera de la app).
 * - La app solo trae la LLAVE PÚBLICA: puede VERIFICAR, nunca fabricar códigos.
 *   Así, aunque alguien decompile el APK, no puede crear códigos falsos.
 * - El código lleva firmado: versión | ID del equipo | fecha de vencimiento.
 * - Se valida: firma correcta + mismo teléfono + no vencido + reloj no alterado.
 *
 * Formato del código:  base64url(payload) + "." + base64url(firma)
 *   payload = "UR1|<androidId>|<yyyymmdd>"
 */
data class LicenseStatus(
    val active: Boolean,
    val reason: String,               // OK, SIN_LICENCIA, VENCIDA, OTRO_EQUIPO, INVALIDA, RELOJ_ALTERADO
    val expiryDisplay: String = "—",  // dd/mm/yyyy
    val daysLeft: Long = 0
)

object LicenseManager {

    // Llave pública (SPKI, base64). Solo sirve para verificar firmas.
    private const val PUB_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2bN2m5WH7HVR6aUIWazR" +
        "RtVfCCwbfulY4bHVNU/lB1px5l169d/K3NoDtCSYnHm142H4uOQjhCXzEglZAwV" +
        "AyRNY63etS8epnnCZHHTT7GSqQdlFKGT4kGUFSISf3/T7gCFKQc5E4CtVek6j2i" +
        "Rcsp+AjLGuNlcHFspb2pI3SmKHyxqjorMF52rAMYeF3YbxlCvBpTOoTkLcdVQYe" +
        "FJlibfuK2MVM7oOPb0tpG7z6CXFGJ3GpS19tUHrLPuBt/eXKm3A80tAJ8R759Hz" +
        "vGSpQXNL9cOj/cuL5JaMW97aZQTByT6N9VfjRIrtGA3GDCYIZ81mLYz9Q9U6HQ/" +
        "w8c3pQQIDAQAB"

    private const val PREFS = "uber_rental_license"
    private const val KEY_CODE = "code"
    private const val KEY_LAST_SEEN = "last_seen"
    private const val KEY_TRIAL_START = "trial_start"

    /** Días de prueba GRATIS automática al instalar (sin código). */
    const val TRIAL_DAYS = 15L
    private val YMD = DateTimeFormatter.BASIC_ISO_DATE  // yyyyMMdd
    private val SHOW = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    @SuppressLint("HardwareIds")
    fun deviceId(context: Context): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return (id ?: "desconocido").lowercase()
    }

    /** Intenta activar un código; si es válido lo guarda. Devuelve el estado. */
    fun activate(context: Context, rawCode: String): LicenseStatus {
        val code = rawCode.trim()
        val st = validate(context, code)
        if (st.active) {
            prefs(context).edit()
                .putString(KEY_CODE, code)
                .putString(KEY_LAST_SEEN, LocalDate.now().format(YMD))
                .apply()
        }
        return st
    }

    /**
     * Estado actual. Prioridad:
     *  1) Si hay un código pagado guardado → manda ese (activo/vencido/otro equipo).
     *  2) Si no hay código → prueba GRATIS de 15 días (arranca sola al instalar).
     */
    fun status(context: Context): LicenseStatus {
        val code = prefs(context).getString(KEY_CODE, null)
        if (code != null) return validate(context, code)
        return trialStatus(context)
    }

    /** Prueba gratis automática de [TRIAL_DAYS] días desde el primer uso. */
    private fun trialStatus(context: Context): LicenseStatus {
        val p = prefs(context)
        val today = LocalDate.now()
        var startStr = p.getString(KEY_TRIAL_START, null)
        if (startStr == null) {
            // Primera vez: arranca la prueba ahora.
            startStr = today.format(YMD)
            p.edit()
                .putString(KEY_TRIAL_START, startStr)
                .putString(KEY_LAST_SEEN, startStr)
                .apply()
        }
        val start = runCatching { LocalDate.parse(startStr, YMD) }.getOrNull()
            ?: return LicenseStatus(false, "SIN_LICENCIA")

        // Anti-reloj: si atrasaron la fecha respecto a la última vez vista
        val lastSeen = p.getString(KEY_LAST_SEEN, null)
            ?.let { runCatching { LocalDate.parse(it, YMD) }.getOrNull() }
        if (lastSeen != null && today.isBefore(lastSeen.minusDays(3)))
            return LicenseStatus(false, "RELOJ_ALTERADO")
        if (lastSeen == null || today.isAfter(lastSeen))
            p.edit().putString(KEY_LAST_SEEN, today.format(YMD)).apply()

        val end = start.plusDays(TRIAL_DAYS)
        return if (!today.isAfter(end))
            LicenseStatus(true, "TRIAL", end.format(SHOW), ChronoUnit.DAYS.between(today, end))
        else
            LicenseStatus(false, "TRIAL_VENCIDA", end.format(SHOW))
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_CODE).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun validate(context: Context, code: String): LicenseStatus {
        try {
            val parts = code.split(".")
            if (parts.size != 2) return LicenseStatus(false, "INVALIDA")
            val payload = decode(parts[0])
            val sig = decode(parts[1])

            // 1) Verificar firma con la llave pública
            val spki = Base64.decode(PUB_B64, Base64.DEFAULT)
            val pub = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(spki))
            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(pub)
            verifier.update(payload)
            if (!verifier.verify(sig)) return LicenseStatus(false, "INVALIDA")

            // 2) Leer el contenido firmado
            val fields = String(payload, StandardCharsets.UTF_8).split("|")
            if (fields.size != 3 || fields[0] != "UR1") return LicenseStatus(false, "INVALIDA")
            if (!fields[1].equals(deviceId(context), ignoreCase = true))
                return LicenseStatus(false, "OTRO_EQUIPO")

            val expiry = LocalDate.parse(fields[2], YMD)
            val today = LocalDate.now()

            // 3) Anti-reloj: detectar si atrasaron la fecha del teléfono
            val lastSeen = prefs(context).getString(KEY_LAST_SEEN, null)
                ?.let { runCatching { LocalDate.parse(it, YMD) }.getOrNull() }
            if (lastSeen != null && today.isBefore(lastSeen.minusDays(3)))
                return LicenseStatus(false, "RELOJ_ALTERADO", expiry.format(SHOW))
            if (lastSeen == null || today.isAfter(lastSeen))
                prefs(context).edit().putString(KEY_LAST_SEEN, today.format(YMD)).apply()

            // 4) Vencimiento
            if (today.isAfter(expiry))
                return LicenseStatus(false, "VENCIDA", expiry.format(SHOW))

            val daysLeft = ChronoUnit.DAYS.between(today, expiry)
            return LicenseStatus(true, "OK", expiry.format(SHOW), daysLeft)
        } catch (e: Exception) {
            return LicenseStatus(false, "INVALIDA")
        }
    }

    private fun decode(s: String): ByteArray =
        Base64.decode(s, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
