package com.autoarchery.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.view.WindowManager
import com.autoarchery.app.ui.AutoArcheryApp
import com.autoarchery.app.ui.theme.AutoArcheryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AutoArcheryTheme {
                AutoArcheryApp()
            }
        }
    }
}
