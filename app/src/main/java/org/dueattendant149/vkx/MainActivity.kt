package org.dueattendant149.vkx

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dueattendant149.vkx.design.ThemeMode
import org.dueattendant149.vkx.design.VkxAccents
import org.dueattendant149.vkx.design.VkxTheme
import org.dueattendant149.vkx.ui.VkxRoot

class MainActivity : ComponentActivity() {

    private lateinit var app: VkxApp

    override fun onCreate(savedInstanceState: Bundle?) {
        app = application as VkxApp
        if (app.api.session() == null) {
            startActivity(
                Intent(this, AuthActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
            finish()
            return
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            var themeMode by remember { mutableStateOf(app.settings.themeMode) }
            var accentId by remember { mutableIntStateOf(app.settings.accentId) }
            var monet by remember { mutableStateOf(app.settings.monet) }

            VkxTheme(
                mode = themeMode,
                accent = VkxAccents.byId(accentId),
                monet = monet,
            ) {
                val view = LocalView.current
                val systemDark = isSystemInDarkTheme()
                SideEffect {
                    val window = (view.context as Activity).window
                    val lightBars = themeMode == ThemeMode.LIGHT ||
                        (themeMode == ThemeMode.MONET && !systemDark)
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                VkxRoot(
                    app = app,
                    themeMode = themeMode,
                    accentId = accentId,
                    monet = monet,
                    onThemeChanged = { mode, id, dynamic ->
                        themeMode = mode
                        accentId = id
                        monet = dynamic
                        app.settings.themeMode = mode
                        app.settings.accentId = id
                        app.settings.monet = dynamic
                    },
                    onLogout = ::performLogout,
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            runCatching { app.api.logout() }
            app.settings.loggedIn = false
            startActivity(
                Intent(this@MainActivity, AuthActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
            finish()
        }
    }
}
