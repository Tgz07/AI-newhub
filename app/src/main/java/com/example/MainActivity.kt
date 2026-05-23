package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import android.graphics.Color
import androidx.activity.SystemBarStyle
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
  private val themeViewModel: ThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    setContent {
      val isDarkMode by themeViewModel.isDarkMode.collectAsState()
      
      LaunchedEffect(isDarkMode) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDarkMode },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDarkMode }
        )
      }
      
      MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation(themeViewModel)
        }
      }
    }
  }
}

