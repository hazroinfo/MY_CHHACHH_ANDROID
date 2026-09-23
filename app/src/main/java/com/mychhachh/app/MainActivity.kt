package com.mychhachh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.mychhachh.app.ui.MyChhachhApp
import com.mychhachh.app.ui.theme.MyChhachhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent { MyChhachhTheme { MyChhachhApp() } }
    }
}
