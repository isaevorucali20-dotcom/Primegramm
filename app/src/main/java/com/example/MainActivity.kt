package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.PrimeViewModel
import com.example.ui.screens.PrimegramDashboard
import com.example.ui.theme.PrimegramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        setContent {
            val viewModel: PrimeViewModel = viewModel()
            val settingsState by viewModel.settings.collectAsState()
            val themeName = settingsState?.selectedTheme ?: "Dark Cosmic Slate"
            
            PrimegramTheme(themeName = themeName) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrimegramDashboard(viewModel = viewModel)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Primegram P2P Сообщения"
            val descriptionText = "Уведомления о полученных P2P сообщениях и статусе ядра"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("primegram_p2p_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
