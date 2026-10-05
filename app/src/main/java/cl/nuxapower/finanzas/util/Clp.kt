package cl.nuxapower.finanzas.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private val formato = DecimalFormat("#,##0", DecimalFormatSymbols().apply { groupingSeparator = '.' })

/** 12500 -> "$12.500"; -12500 -> "-$12.500". */
fun formatoClp(monto: Long): String =
    if (monto < 0) "-$" + formato.format(-monto) else "$" + formato.format(monto)
