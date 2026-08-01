package com.juanka.uberrental

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formateo en estilo colombiano: separador de miles con punto.
 */
object Formatting {

    private val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val intFormat = DecimalFormat("#,##0", symbols)
    private val oneDecimal = DecimalFormat("#,##0.0", symbols)

    fun money(value: Double?): String =
        if (value == null) "—" else "$" + intFormat.format(Math.round(value))

    fun money(value: Int?): String =
        if (value == null) "—" else "$" + intFormat.format(value)

    fun km(value: Double?): String =
        if (value == null) "—" else oneDecimal.format(value) + " km"

    fun minutes(value: Double?): String =
        if (value == null) "—" else intFormat.format(Math.round(value)) + " min"
}
