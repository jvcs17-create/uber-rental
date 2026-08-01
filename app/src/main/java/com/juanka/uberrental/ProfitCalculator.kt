package com.juanka.uberrental

/**
 * Resultado del analisis de una oferta.
 */
data class ProfitResult(
    val fareCop: Int,
    val perHourCop: Double?,   // ganancia por hora
    val perKmCop: Double?,     // ganancia por km
    val totalMinutes: Double?,
    val totalKm: Double?,
    val isProfitable: Boolean  // true si perHour >= umbral
)

/**
 * Calcula la ganancia BRUTA por hora y por km.
 * Incluye el trayecto de ida a recoger (ya sumado dentro de OfferData.totalMinutes/totalKm).
 *
 * Rentable  = ganancia por hora >= umbral (por defecto $25.000/hora) -> VERDE
 * No rentable = por debajo del umbral -> ROJO
 */
object ProfitCalculator {

    fun analyze(offer: OfferData, thresholdPerHour: Int): ProfitResult? {
        val fare = offer.fareCop ?: return null
        val minutes = offer.totalMinutes
        val km = offer.totalKm

        val perHour = if (minutes != null && minutes > 0) fare / (minutes / 60.0) else null
        val perKm = if (km != null && km > 0) fare / km else null

        // Si no pudimos calcular por hora, caemos a "no rentable" para no dar falsos verdes.
        val profitable = perHour != null && perHour >= thresholdPerHour

        return ProfitResult(
            fareCop = fare,
            perHourCop = perHour,
            perKmCop = perKm,
            totalMinutes = minutes,
            totalKm = km,
            isProfitable = profitable
        )
    }
}
