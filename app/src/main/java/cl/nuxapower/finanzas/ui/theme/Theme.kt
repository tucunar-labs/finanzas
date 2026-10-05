package cl.nuxapower.finanzas.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Verde = Color(0xFF2E7D5B)

private val Claro = lightColorScheme(primary = Verde, secondary = Color(0xFF4F6354))
private val Oscuro = darkColorScheme(primary = Color(0xFF8BD6B0), secondary = Color(0xFFB5CCBA))

/** Verde para ingresos y rojo para gastos, legibles en claro y oscuro. */
object ColoresMonto {
    @Composable fun ingreso() = if (isSystemInDarkTheme()) Color(0xFF81C995) else Color(0xFF1E8E3E)
    @Composable fun gasto() = if (isSystemInDarkTheme()) Color(0xFFF28B82) else Color(0xFFC5221F)
}

@Composable
fun FinanzasTheme(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    val esquema = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (oscuro) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        oscuro -> Oscuro
        else -> Claro
    }
    MaterialTheme(colorScheme = esquema, content = content)
}
