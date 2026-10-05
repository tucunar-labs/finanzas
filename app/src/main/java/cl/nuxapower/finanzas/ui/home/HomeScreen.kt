package cl.nuxapower.finanzas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.data.model.Tipo

@Composable
fun HomeScreen(modifier: Modifier = Modifier, vm: HomeViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.size(16.dp))
            Text("Hogar", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Card(Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Código de invitación",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                )
                Text(
                    e.hogar?.codigoInvitacion ?: "…",
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    "Miembros: ${e.hogar?.miembros?.size ?: 0} de 2",
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
                )
            }
        }
        item { Text("Personas", style = MaterialTheme.typography.titleMedium) }
        items(e.personas, key = { it.id }) { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(16.dp).background(Color(p.color), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(p.nombre + if (p.id == e.uid) " (tú)" else "")
            }
        }
        Tipo.entries.forEach { tipo ->
            item {
                Text(
                    if (tipo == Tipo.GASTO) "Categorías de gasto" else "Categorías de ingreso",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(e.categorias.filter { it.tipo == tipo }, key = { it.id }) { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.icono)
                    Spacer(Modifier.width(12.dp))
                    Text(c.nombre, color = Color(c.color))
                }
            }
        }
        item {
            TextButton(onClick = vm::salir, modifier = Modifier.padding(vertical = 24.dp)) { Text("Cerrar sesión") }
        }
    }
}
