package com.waylo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waylo.app.ui.WayloApp
import com.waylo.app.ui.theme.WayloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        (application as? WayloApplication)?.currentActivity = this
        setContent {
            WayloTheme {
                WayloApp()
            }
        }
    }

    override fun onDestroy() {
        val wayloApplication = application as? WayloApplication
        if (wayloApplication !== null && wayloApplication.currentActivity === this) {
            wayloApplication.currentActivity = null
        }
        super.onDestroy()
    }
}
