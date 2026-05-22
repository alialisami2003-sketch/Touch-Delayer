package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.CircleViewModel
import com.example.ui.MainControlPanel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  
  private val viewModel: CircleViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          MainControlPanel(viewModel = viewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Instantly check and update permission checklists if the user returns from system settings
    viewModel.checkRealtimeStatuses()
  }
}
