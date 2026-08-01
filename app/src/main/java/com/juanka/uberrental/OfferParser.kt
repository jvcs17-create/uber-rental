package com.juanka.uberrental

/**
 * Convierte el texto crudo leido de la pantalla de Uber Driver en un [OfferData].
 *
 * Formato colombiano:
 *   - Tarifa: "$14.300"  ( "." = separador de miles )
 *   - Distancia: "1,8 km" ( "," = decimal )
 *   - Tiempo: "12 min"
 *
 * El parser es tolerante: intenta varias formas y usa palabras clave
 * ("recoger", "distancia", "viaje") para distinguir el tramo de ida
 * a recoger del viaje pagado. Si no encuentra palabras clave, asume que
 * el primer par (min, km) es la ida a recoger y el segundo es el viaje.
 */
object OfferParser {

    // Captura el numero completo tras "$" o "COP", en cualquier formato:
    //   "$14.300", "COP 14.300", "COP9,036", "COP1,434.00"
    // (soporta miles con "." o con ",", y decimales con "," o con ".")
    private val fareRegex = Regex("""(?:\$|COP)\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)

    // "12 min (5,4 km)"  o  "12 min · 5,4 km"  -> captura minutos y km juntos
    private val pairRegex = Regex(
        """(\d+(?:[.,]\d+)?)\s*min[^\d(]*[\(·|,]?\s*(\d+(?:[.,]\d+)?)\s*km""",
        RegexOption.IGNORE_CASE
    )

    private val pickupKeywords = listOf("recoger", "distancia", "away", "de camino", "pickup", "hasta ti", "para llegar")
    private val tripKeywords = listOf("viaje", "trip", "recorrido", "de trayecto")

    fun parse(text: String): OfferData {
        val fare = extractFare(text)
        val pairs = pairRegex.findAll(text).toList()

        var pickupMin: Double? = null
        var pickupKm: Double? = null
        var tripMin: Double? = null
        var tripKm: Double? = null

        if (pairs.isNotEmpty()) {
            // Intentar clasificar por palabras clave alrededor de cada par
            val classified = pairs.map { m ->
                val minutes = parseNumber(m.groupValues[1])
                val km = parseNumber(m.groupValues[2])
                val ctx = contextAround(text, m.range.first, m.range.last)
                val role = when {
                    pickupKeywords.any { ctx.contains(it, ignoreCase = true) } -> Role.PICKUP
                    tripKeywords.any { ctx.contains(it, ignoreCase = true) } -> Role.TRIP
                    else -> Role.UNKNOWN
                }
                Triple(minutes, km, role)
            }

            val pickup = classified.firstOrNull { it.third == Role.PICKUP }
            val trip = classified.firstOrNull { it.third == Role.TRIP }

            if (pickup != null || trip != null) {
                pickup?.let { pickupMin = it.first; pickupKm = it.second }
                trip?.let { tripMin = it.first; tripKm = it.second }
                // Si solo detectamos uno por keyword, usar el otro par restante
                if (pickup == null && classified.size >= 2) {
                    val other = classified.firstOrNull { it !== trip }
                    other?.let { pickupMin = it.first; pickupKm = it.second }
                }
                if (trip == null && classified.size >= 2) {
                    val other = classified.firstOrNull { it !== pickup }
                    other?.let { tripMin = it.first; tripKm = it.second }
                }
            } else {
                // Sin palabras clave: por orden. 1ro = ida a recoger, 2do = viaje.
                when (classified.size) {
                    1 -> {
                        tripMin = classified[0].first
                        tripKm = classified[0].second
                    }
                    else -> {
                        pickupMin = classified[0].first
                        pickupKm = classified[0].second
                        tripMin = classified[1].first
                        tripKm = classified[1].second
                    }
                }
            }
        }

        // Fallback: si no hubo pares combinados, buscar min y km sueltos
        if (pickups(pickupMin, tripMin) == null && pairs.isEmpty()) {
            val mins = Regex("""(\d+(?:[.,]\d+)?)\s*min""", RegexOption.IGNORE_CASE)
                .findAll(text).map { parseNumber(it.groupValues[1]) }.toList()
            val kms = Regex("""(\d+(?:[.,]\d+)?)\s*km""", RegexOption.IGNORE_CASE)
                .findAll(text).map { parseNumber(it.groupValues[1]) }.toList()
            return OfferData(
                fareCop = fare,
                totalMinutesRaw = mins.filterNotNull().takeIf { it.isNotEmpty() }?.sum(),
                totalKmRaw = kms.filterNotNull().takeIf { it.isNotEmpty() }?.sum(),
                rawText = text
            )
        }

        return OfferData(
            fareCop = fare,
            pickupMinutes = pickupMin,
            pickupKm = pickupKm,
            tripMinutes = tripMin,
            tripKm = tripKm,
            rawText = text
        )
    }

    private fun pickups(a: Double?, b: Double?): Double? = a ?: b

    private enum class Role { PICKUP, TRIP, UNKNOWN }

    private fun contextAround(text: String, start: Int, end: Int): String {
        val from = (start - 30).coerceAtLeast(0)
        val to = (end + 30).coerceAtMost(text.length)
        return text.substring(from, to)
    }

    /**
     * Toma la tarifa mas grande encontrada, EXCLUYENDO los valores "por km"
     * (ej "COP1.434/km") y quedandose con la tarifa total del servicio.
     */
    private fun extractFare(text: String): Int? {
        val matches = fareRegex.findAll(text).toList()
        val candidates = mutableListOf<Int>()
        for (m in matches) {
            // Mirar lo que viene justo despues del numero: si es "/km", es tarifa por km, no la total
            val after = text.substring((m.range.last + 1).coerceAtMost(text.length)).take(4)
            if (after.trimStart().startsWith("/km", ignoreCase = true)) continue
            parseCop(m.groupValues[1])?.let { candidates.add(it) }
        }
        val pool = if (candidates.isNotEmpty()) candidates
        else matches.mapNotNull { parseCop(it.groupValues[1]) }
        return pool.maxOrNull()
    }

    /**
     * Convierte un texto de dinero a pesos enteros, detectando el formato:
     *   "9,036" -> 9036   (coma = miles)
     *   "1,434.00" -> 1434 (coma = miles, punto = decimal)
     *   "14.300" -> 14300  (punto = miles, formato colombiano clasico)
     *   "14.300,50" -> 14301 (punto = miles, coma = decimal)
     * Regla: si hay ambos separadores, el ULTIMO es el decimal. Si hay uno solo
     * y le siguen exactamente 3 digitos, es separador de miles.
     */
    fun parseCop(token: String): Int? {
        val t = token.trim()
        if (t.isEmpty()) return null
        val hasComma = t.contains(",")
        val hasDot = t.contains(".")
        val normalized = when {
            hasComma && hasDot -> {
                if (t.lastIndexOf('.') > t.lastIndexOf(',')) t.replace(",", "")
                else t.replace(".", "").replace(",", ".")
            }
            hasComma -> {
                val digitsAfter = t.length - t.lastIndexOf(',') - 1
                if (digitsAfter == 3) t.replace(",", "") else t.replace(",", ".")
            }
            hasDot -> {
                val digitsAfter = t.length - t.lastIndexOf('.') - 1
                if (digitsAfter == 3) t.replace(".", "") else t
            }
            else -> t
        }
        return normalized.toDoubleOrNull()?.let { Math.round(it).toInt() }
    }

    /** "1,8" -> 1.8 ; "10.5" -> 10.5 ; "12" -> 12.0 */
    fun parseNumber(token: String): Double? {
        val cleaned = if (token.contains(",")) token.replace(".", "").replace(",", ".") else token
        return cleaned.toDoubleOrNull()
    }
}
