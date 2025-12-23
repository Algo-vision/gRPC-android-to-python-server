package com.instacart.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.instacart.sample.ui.GrpcSampleScreen
import com.instacart.sample.ui.theme.SampleGrpcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleGrpcTheme { GrpcSampleScreen() }
        }
    }
}
