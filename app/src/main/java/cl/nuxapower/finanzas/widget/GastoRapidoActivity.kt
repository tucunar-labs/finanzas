package cl.nuxapower.finanzas.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.MainActivity
import cl.nuxapower.finanzas.data.repo.Sesion
import cl.nuxapower.finanzas.data.repo.SesionRepository
import cl.nuxapower.finanzas.ui.comun.colorTipo
import cl.nuxapower.finanzas.ui.ingreso.EventoIngreso
import cl.nuxapower.finanzas.ui.ingreso.IngresoViewModel
import cl.nuxapower.finanzas.ui.ingreso.Teclado
import cl.nuxapower.finanzas.ui.theme.FinanzasTheme
import cl.nuxapower.finanzas.util.formatoClp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hoja de ingreso que abre el widget: solo monto y categoría. Persona = quien usa el teléfono,
 * fecha = hoy. Al guardar se cierra; sin conexión queda en cola y se sincroniza después.
 */
@AndroidEntryPoint
class GastoRapidoActivity : ComponentActivity() {

    @Inject lateinit var sesionRepo: SesionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanzasTheme {
                val sesion by sesionRepo.sesion.collectAsStateWithLifecycle()
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    when (sesion) {
                        is Sesion.ConHogar -> Hoja(onListo = { mensaje ->
                            Toast.makeText(this@GastoRapidoActivity, mensaje, Toast.LENGTH_SHORT).show()
                            finish()
                        })
                        Sesion.Cargando -> CircularProgressIndicator(Modifier.padding(64.dp))
                        // Sin sesión o sin hogar: hay que pasar por la app.
                        else -> LaunchedEffect(Unit) {
                            startActivity(Intent(this@GastoRapidoActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hoja(onListo: (String) -> Unit, vm: IngresoViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val f = e.form

    LaunchedEffect(Unit) {
        vm.eventos.collect { ev -> if (ev is EventoIngreso.Guardado) onListo(ev.mensaje) }
    }

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // Evita que los toques en la hoja cierren la actividad.
        modifier = Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = 16.dp)) {
            Text(
                "Gasto rápido",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                formatoClp(f.monto),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
                color = if (f.monto == 0L) MaterialTheme.colorScheme.outline else colorTipo(f.tipo),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                e.categorias.forEach { c ->
                    FilterChip(
                        selected = c.id == f.categoriaId,
                        onClick = { vm.categoria(c.id) },
                        label = { Text("${c.icono} ${c.nombre}") },
                    )
                }
            }
            f.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp))
            }
            Teclado(onTecla = vm::tecla, onSumar = vm::sumar, onBorrar = vm::borrar, onLimpiar = vm::limpiarMonto)
            Button(
                onClick = vm::guardar,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
            ) { Text("Guardar", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
