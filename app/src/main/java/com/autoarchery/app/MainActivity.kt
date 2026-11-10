package com.autoarchery.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.view.WindowManager
import com.autoarchery.app.ui.AutoArcheryApp
import com.autoarchery.app.ui.theme.AutoArcheryTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) {
        val code = base.getSharedPreferences("ui_locale", Context.MODE_PRIVATE).getString("language", "zh-TW")!!
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(code))
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lifecycleScope.launch {
            (application as AutoArcheryApplication).appContainer.preferenceRepository.observePreference().collect { preference ->
                if (preference != null) {
                    val prefs = getSharedPreferences("ui_locale", Context.MODE_PRIVATE)
                    val current = prefs.getString("language", "zh-TW")
                    if (current != preference.displayLanguage) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            prefs.edit().putString("language", preference.displayLanguage).commit()
                        }
                        val config = Configuration(application.resources.configuration)
                        config.setLocale(Locale.forLanguageTag(preference.displayLanguage))
                        @Suppress("DEPRECATION")
                        application.resources.updateConfiguration(config, application.resources.displayMetrics)
                        recreate()
                    }
                }
            }
        }
        setContent {
            AutoArcheryTheme {
                AutoArcheryApp()
            }
        }
    }
}
