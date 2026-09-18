package com.example.motionpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.motionpulse.ui.navigation.MainAppContent
import com.example.motionpulse.ui.theme.MotionPulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MotionPulseTheme {
                MainAppContent()
            }
        }
    }
}
