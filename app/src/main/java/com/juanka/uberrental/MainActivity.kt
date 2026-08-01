package com.juanka.uberrental

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.juanka.uberrental.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = SettingsStore(this)

        binding.inputThreshold.setText(settings.thresholdPerHour.toString())
        binding.switchLearn.isChecked = settings.learnMode

        binding.btnSaveThreshold.setOnClickListener { saveThreshold() }

        binding.switchLearn.setOnCheckedChangeListener { _, checked ->
            settings.learnMode = checked
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        binding.btnTest.setOnClickListener { showTestOverlay() }

        // --- Licencia ---
        binding.tvDeviceId.text = LicenseManager.deviceId(this)
        binding.btnCopyId.setOnClickListener { copyDeviceId() }
        binding.btnPasteCode.setOnClickListener { pasteCode() }
        binding.btnActivate.setOnClickListener { activateCode() }

        // --- Mantener activo ---
        KeepAliveService.start(this)
        binding.btnBattery.setOnClickListener { requestIgnoreBattery() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshLicense()
        refreshBattery()
    }

    private fun requestIgnoreBattery() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, R.string.status_battery_ok, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }

    private fun refreshBattery() {
        val ok = try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } catch (e: Exception) {
            false
        }
        binding.statusBattery.text =
            getString(if (ok) R.string.status_battery_ok else R.string.status_battery_bad)
        binding.statusBattery.setTextColor(
            getColor(if (ok) R.color.status_ok else R.color.status_bad)
        )
    }

    private fun copyDeviceId() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("device_id", LicenseManager.deviceId(this)))
        Toast.makeText(this, R.string.toast_id_copied, Toast.LENGTH_SHORT).show()
    }

    private fun pasteCode() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        val text = if (clip != null && clip.itemCount > 0)
            clip.getItemAt(0).coerceToText(this).toString() else ""
        if (text.isBlank()) {
            Toast.makeText(this, R.string.toast_paste_empty, Toast.LENGTH_SHORT).show()
            return
        }
        binding.inputCode.setText(text.trim())
    }

    private fun activateCode() {
        val code = binding.inputCode.text.toString().trim()
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.toast_paste_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val st = LicenseManager.activate(this, code)
        if (st.active) {
            Toast.makeText(
                this, getString(R.string.lic_activated_ok, st.expiryDisplay), Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, licenseMessage(st), Toast.LENGTH_LONG).show()
        }
        refreshLicense()
    }

    private fun refreshLicense() {
        val st = LicenseManager.status(this)
        binding.tvLicenseStatus.text = licenseMessage(st)
        binding.tvLicenseStatus.setTextColor(
            getColor(if (st.active) R.color.status_ok else R.color.status_bad)
        )
    }

    private fun licenseMessage(st: LicenseStatus): String = when {
        st.reason == "TRIAL" -> getString(R.string.lic_trial, st.expiryDisplay, st.daysLeft)
        st.active -> getString(R.string.lic_active, st.expiryDisplay, st.daysLeft)
        st.reason == "TRIAL_VENCIDA" -> getString(R.string.lic_trial_ended, st.expiryDisplay)
        st.reason == "VENCIDA" -> getString(R.string.lic_expired, st.expiryDisplay)
        st.reason == "OTRO_EQUIPO" -> getString(R.string.lic_other_device)
        st.reason == "RELOJ_ALTERADO" -> getString(R.string.lic_clock)
        st.reason == "SIN_LICENCIA" -> getString(R.string.lic_none)
        else -> getString(R.string.lic_invalid)
    }

    private fun saveThreshold() {
        val raw = binding.inputThreshold.text.toString().replace(".", "").replace(",", "").trim()
        val value = raw.toIntOrNull()
        if (value == null || value <= 0) {
            Toast.makeText(this, R.string.toast_invalid_threshold, Toast.LENGTH_SHORT).show()
            return
        }
        settings.thresholdPerHour = value
        binding.inputThreshold.setText(value.toString())
        Toast.makeText(
            this,
            getString(R.string.toast_threshold_saved, Formatting.money(value)),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun refreshStatus() {
        val accOn = isAccessibilityEnabled()
        val overlayOn = Settings.canDrawOverlays(this)

        binding.statusAccessibility.text = getString(
            if (accOn) R.string.status_on else R.string.status_off
        )
        binding.statusAccessibility.setTextColor(
            getColor(if (accOn) R.color.status_ok else R.color.status_bad)
        )

        binding.statusOverlay.text = getString(
            if (overlayOn) R.string.status_on else R.string.status_off
        )
        binding.statusOverlay.setTextColor(
            getColor(if (overlayOn) R.color.status_ok else R.color.status_bad)
        )

        binding.btnTest.isEnabled = overlayOn
    }

    private fun showTestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.toast_need_overlay, Toast.LENGTH_SHORT).show()
            return
        }
        // Oferta de ejemplo: tarifa $12.000, 3 min/1,5 km ida + 15 min/8 km viaje
        val demo = OfferData(
            fareCop = 12000,
            pickupMinutes = 3.0, pickupKm = 1.5,
            tripMinutes = 15.0, tripKm = 8.0,
            rawText = "Ejemplo de prueba"
        )
        val result = ProfitCalculator.analyze(demo, settings.thresholdPerHour)
        if (result != null) {
            OverlayManager(this).show(result, if (settings.learnMode) demo.rawText else null)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expectedId = "$packageName/${UberAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedId, ignoreCase = true)) return true
        }
        return false
    }
}
