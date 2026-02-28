package com.ytt.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PosApp()
        }
    }
}

@Composable
fun PosApp() {
    val navController = rememberNavController()

    MaterialTheme {
        Surface(modifier = Modifier) {
            NavHost(navController = navController, startDestination = "register") {
                composable("register") {
                    RegisterScreen(onNavigateToHardware = {
                        navController.navigate("hardware")
                    }, onNavigateToCheckout = {
                        navController.navigate("checkout")
                    })
                }
                composable("hardware") {
                    HardwareScreen(onNavigateBack = {
                        navController.popBackStack()
                    })
                }
                composable("checkout") {
                    CheckoutScreen(onNavigateBack = {
                        navController.popBackStack()
                    })
                }
            }
        }
    }
}
