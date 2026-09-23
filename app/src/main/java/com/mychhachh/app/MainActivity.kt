package com.mychhachh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.view.WindowCompat
import com.mychhachh.app.ui.MyChhachhApp
import com.mychhachh.app.ui.components.AppLanguage
import com.mychhachh.app.ui.theme.MyChhachhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val savedLanguage = getSharedPreferences("my_chhachh_native", MODE_PRIVATE)
            .getString("language", "en")
            ?.takeIf { it == "ur" || it == "en" }
            ?: "en"
        AppLanguage.set(savedLanguage)

        setContent {
            val direction = if (AppLanguage.isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MyChhachhTheme { MyChhachhApp() }
            }
        }
    }
}
