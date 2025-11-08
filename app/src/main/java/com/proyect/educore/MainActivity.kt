package com.proyect.educore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.proyect.educore.ui.navigation.EduCoreApp
import com.proyect.educore.ui.theme.EduCoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduCoreTheme {
                EduCoreApp(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
