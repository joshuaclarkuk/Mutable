package com.example.stemselector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.stemselector.navigation.AppNavigation
import com.example.stemselector.ui.theme.StemSelectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StemSelectorTheme {
                val navHostController: NavHostController = rememberNavController()
                AppNavigation(navHostController)
            }
        }
    }
}