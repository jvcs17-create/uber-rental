package com.juanka.uberrental

/**
 * Datos crudos extraidos de la tarjeta de oferta de Uber Driver.
 *
 * Uber (en Colombia) muestra normalmente:
 *   - Una tarifa grande, ej: "$14.300"
 *   - El trayecto de ida a recoger: ej "4 min (1,8 km) de distancia"
 *   - El viaje del pasajero: ej "18 min (10,5 km) de viaje"
 *
 * Todos los campos son opcionales porque no siempre aparecen todos.
 */
data class OfferData(
    val fareCop: Int? = null,          // tarifa en pesos, ej 14300
    val pickupMinutes: Double? = null, // minutos hasta el pasajero
    val pickupKm: Double? = null,      // km hasta el pasajero
    val tripMinutes: Double? = null,   // minutos del viaje pagado
    val tripKm: Double? = null,        // km del viaje pagado
    // Si Uber ya muestra un total combinado en vez de dos tramos:
    val totalMinutesRaw: Double? = null,
    val totalKmRaw: Double? = null,
    val rawText: String = ""           // texto crudo leido (para modo aprendizaje)
) {
    /** Minutos totales: ida a recoger + viaje. Cae al total crudo si no hay tramos. */
    val totalMinutes: Double?
        get() {
            val pk = pickupMinutes
            val tr = tripMinutes
            return when {
                pk != null && tr != null -> pk + tr
                tr != null -> tr
                totalMinutesRaw != null -> totalMinutesRaw
                else -> null
            }
        }

    /** Km totales: ida a recoger + viaje. Cae al total crudo si no hay tramos. */
    val totalKm: Double?
        get() {
            val pk = pickupKm
            val tr = tripKm
            return when {
                pk != null && tr != null -> pk + tr
                tr != null -> tr
                totalKmRaw != null -> totalKmRaw
                else -> null
            }
        }

    /** True si tenemos lo minimo para calcular rentabilidad. */
    val isUsable: Boolean
        get() = fareCop != null && (totalMinutes != null || totalKm != null)
}
