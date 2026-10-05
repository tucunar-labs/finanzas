package cl.nuxapower.finanzas

import android.app.Application
import cl.nuxapower.finanzas.widget.WidgetSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FinanzasApp : Application() {

    @Inject lateinit var widgetSync: WidgetSync

    override fun onCreate() {
        super.onCreate()
        widgetSync.iniciar()
    }
}
