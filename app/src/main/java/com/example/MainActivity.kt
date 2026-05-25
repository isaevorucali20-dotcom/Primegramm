package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PrimeViewModel
import com.example.ui.screens.PrimegramDashboard
import com.example.ui.theme.PrimegramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrimegramTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PrimeViewModel = viewModel()
                    PrimegramDashboard(viewModel = viewModel)
                }
            }
        }
    }
}
