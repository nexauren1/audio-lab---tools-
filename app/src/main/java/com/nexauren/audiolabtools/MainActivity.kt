package com.nexauren.audiolabtools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nexauren.audiolabtools.ui.AudioLabApp
import com.nexauren.audiolabtools.ui.theme.AudioLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(7, 11, 22)
        window.navigationBarColor = android.graphics.Color.rgb(7, 11, 22)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            AudioLabTheme {
                AudioLabApp()
            }
        }
    }
}
