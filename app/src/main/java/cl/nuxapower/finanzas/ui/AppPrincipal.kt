package cl.nuxapower.finanzas.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.nuxapower.finanzas.ui.ajustes.AjustesScreen
import cl.nuxapower.finanzas.ui.categorias.CategoriasScreen
import cl.nuxapower.finanzas.ui.comun.Avisos
import cl.nuxapower.finanzas.ui.comun.LocalAvisos
import cl.nuxapower.finanzas.ui.ingreso.IngresoScreen
import cl.nuxapower.finanzas.ui.movimientos.MovimientosScreen
import cl.nuxapower.finanzas.ui.resumen.ResumenScreen

private enum class Pestana(val ruta: String, val titulo: String, val icono: ImageVector) {
    INGRESO("ingreso", "Ingresar", Icons.Filled.AddCircle),
    MOVIMIENTOS("movimientos", "Movimientos", Icons.AutoMirrored.Filled.ReceiptLong),
    RESUMEN("resumen", "Resumen", Icons.Filled.PieChart),
    AJUSTES("ajustes", "Ajustes", Icons.Filled.Settings),
}

/** Cambia de pestaña conservando el estado de cada una. */
private fun irA(nav: NavHostController, ruta: String) = nav.navigate(ruta) {
    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppPrincipal() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val avisos = remember { Avisos(snackbar, scope) }
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    CompositionLocalProvider(LocalAvisos provides avisos) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                // La barra solo se muestra en las pantallas principales.
                if (Pestana.entries.any { it.ruta == rutaActual }) {
                    NavigationBar {
                        Pestana.entries.forEach { p ->
                            NavigationBarItem(
                                selected = rutaActual == p.ruta,
                                onClick = { irA(nav, p.ruta) },
                                icon = { Icon(p.icono, null) },
                                label = { Text(p.titulo) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Pestana.INGRESO.ruta,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            ) {
                composable(Pestana.INGRESO.ruta) { IngresoScreen() }
                composable(Pestana.MOVIMIENTOS.ruta) {
                    MovimientosScreen(onEditar = { id -> nav.navigate("editar/$id") })
                }
                composable(Pestana.RESUMEN.ruta) {
                    ResumenScreen(onVerMovimientos = { irA(nav, Pestana.MOVIMIENTOS.ruta) })
                }
                composable(Pestana.AJUSTES.ruta) { AjustesScreen(onCategorias = { nav.navigate("categorias") }) }
                composable("categorias") { CategoriasScreen(onVolver = { nav.popBackStack() }) }
                composable(
                    "editar/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { IngresoScreen(onCerrar = { nav.popBackStack() }) }
            }
        }
    }
}
