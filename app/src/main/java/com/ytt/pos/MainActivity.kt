package com.ytt.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var hardwareManager: HardwareManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            hardwareManager.reconnectAll()
        }
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
                    }, onNavigateToTransactions = {
                        navController.navigate("transactions")
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
                composable("transactions") {
                    TransactionsListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenTransaction = { transactionId ->
                            navController.navigate("transactions/$transactionId")
                        },
                    )
                }
                composable(
                    route = "transactions/{transactionId}",
                    arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
                ) {
                    TransactionDetailScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
