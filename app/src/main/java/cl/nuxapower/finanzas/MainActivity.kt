// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Tucunar SpA

package cl.nuxapower.finanzas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.nuxapower.finanzas.data.model.Tipo
import cl.nuxapower.finanzas.data.repo.Sesion
import cl.nuxapower.finanzas.data.repo.Solicitudes
import cl.nuxapower.finanzas.data.repo.SesionRepository
import cl.nuxapower.finanzas.ui.AppPrincipal
import cl.nuxapower.finanzas.ui.hogar.HogarSetupScreen
import cl.nuxapower.finanzas.ui.login.LoginScreen
import cl.nuxapower.finanzas.ui.theme.FinanzasTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RaizViewModel @Inject constructor(sesion: SesionRepository) : ViewModel() {
    val sesion = sesion.sesion
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var solicitudes: Solicitudes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) leerAtajo(intent)
        enableEdgeToEdge()
        setContent { FinanzasTheme { Raiz() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerAtajo(intent)
    }

    /** Atajos "Nuevo gasto"/"Nuevo ingreso" del launcher: preseleccionan el tipo en el ingreso rápido. */
    private fun leerAtajo(intent: Intent?) {
        val tipo = intent?.getStringExtra("tipo") ?: return
        runCatching { Tipo.valueOf(tipo) }.getOrNull()?.let { solicitudes.tipoIngreso.value = it }
    }
}

@Composable
private fun Raiz(vm: RaizViewModel = hiltViewModel()) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    // Con hogar, la app principal maneja su propio Scaffold (barra inferior y snackbars).
    if (sesion is Sesion.ConHogar) {
        AppPrincipal()
        return
    }
    Scaffold { padding ->
        val m = Modifier.padding(padding)
        when (val s = sesion) {
            Sesion.Cargando -> Centro(m) { CircularProgressIndicator() }
            Sesion.SinLogin -> Box(m) { LoginScreen() }
            is Sesion.SinHogar -> Box(m) { HogarSetupScreen() }
            is Sesion.ConHogar -> Unit
            is Sesion.Error -> Centro(m) {
                Text(s.mensaje, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(32.dp))
            }
        }
    }
}

@Composable
private fun Centro(modifier: Modifier, content: @Composable () -> Unit) =
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
