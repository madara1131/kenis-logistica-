package com.kenisshop.logistica

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenisshop.logistica.ui.LoginScreen
import com.kenisshop.logistica.ui.MainScreen
import com.kenisshop.logistica.ui.MainViewModel
import com.kenisshop.logistica.ui.theme.LogisticaTheme

class MainActivity : ComponentActivity() {

    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LogisticaTheme {
                val vm: MainViewModel = viewModel()
                if (vm.sesionIniciada) {
                    MainScreen(vm)
                } else {
                    LoginScreen(onIniciar = { usuario, clave -> vm.iniciarSesion(usuario, clave) })
                }
            }
        }
    }
}
