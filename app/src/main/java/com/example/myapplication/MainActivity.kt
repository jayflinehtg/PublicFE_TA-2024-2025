package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.UiEvent
import com.example.myapplication.ui.components.BottomNavBarScreen
import com.example.myapplication.ui.components.WalletComponent
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object WalletComponent : Screen("walletComponent")
    object Profile : Screen("profile")
    object Register : Screen("register")
    object Login : Screen("login")
    object Home : Screen("home")
    object EditPlant : Screen("editplant/{plantId}") {
        fun createRoute(plantId: String) = "editplant/$plantId"
    }
    object Detail : Screen("detail/{plantId}") {
        fun createRoute(plantId: String) = "detail/$plantId"
    }
    object TransactionHistory : Screen("transaction_history/{plantId}")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Untuk navigasi berdasarkan status login (jika user keluar aplikasi tanpa logout)
                LaunchedEffect(uiState.isLoggedIn, uiState.isGuest) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route

                    if (uiState.isLoggedIn && !uiState.isGuest) {
                        if (currentRoute == Screen.WalletComponent.route ||
                            currentRoute == Screen.Login.route ||
                            currentRoute == Screen.Register.route) {

                            Log.d("MainActivity", "User is logged in. Navigating to Home.")
                            navController.navigate(Screen.Home.route) {
                                // Mencegah user agar tidak bisa kembali ke tampilan register/login
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // Handle UI events dari ViewModel (navigasi & pesan)
                LaunchedEffect(navController, viewModel) { // Key bisa disederhanakan
                    viewModel.uiEvent.collect { event ->
                        Log.d("MainActivity", "Menerima UiEvent: $event")
                        when (event) {
                            is UiEvent.NavigateTo -> {
                                Log.d("MainActivity", "Navigasi ke: ${event.route}")
                                navController.navigate(event.route) {
                                    when (event.route) {
                                        Screen.Home.route -> {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        }
                                        Screen.Login.route -> {
                                            popUpTo(Screen.Register.route) { inclusive = true }
                                        }
                                        Screen.WalletComponent.route -> {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        }
                                    }
                                    launchSingleTop = true
                                }
                            }
                            is UiEvent.Message -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = event.message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.WalletComponent.route,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                    // Wallet Component Screen
                    composable(Screen.WalletComponent.route) {
                        WalletComponent(
                            isConnecting = uiState.isConnecting,
                            isGuest = uiState.isGuest,
                            eventSink = { event -> viewModel.eventSink(event) }
                        )
                    }

                    // Register Screen
                    composable(Screen.Register.route) {
                        RegisterScreen(
                            navController = navController,
                            viewModel = viewModel, // Teruskan MainViewModel
                            onNavigateToLogin = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            // Callback onRegisterSuccess dihapus
                        )
                    }

                    // Login Screen
                        composable(Screen.Login.route) {
                            LoginScreen(
                                navController = navController,
                                viewModel = viewModel, // Teruskan MainViewModel
                                onNavigateToRegister = {
                                    navController.navigate(Screen.Register.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                                // Callback onLoginSuccess dihapus
                            )
                        }

                    // Home Screen
                        composable(Screen.Home.route) {
                            BottomNavBarScreen(
                                rootNavController = navController,
                                isGuest = uiState.isGuest,
                                viewModel = viewModel // MainViewModel diteruskan ke BottomNavBarScreen
                            )
                        }

                    // Detail Screen
                        composable(
                            route = Screen.Detail.route,
                            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
                            val plantViewModel: PlantViewModel = hiltViewModel()

                            if (plantId.isNotEmpty()) {
                                DetailScreen(
                                    plantId = plantId,
                                    onBack = { navController.popBackStack() },
                                    onEdit = {
                                        navController.navigate(Screen.EditPlant.createRoute(plantId))
                                    },
                                    navController = navController,
                                    viewModel = plantViewModel
                                )
                            } else {
                                Text("Plant ID tidak valid.")
                            }
                        }

                        // Transaction History Screen
                        composable(
                            Screen.TransactionHistory.route,
                            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
                            if (plantId.isEmpty()) {
                                Text("Plant ID kosong")
                                return@composable
                            }

                            TransactionHistoryScreen(
                                plantId = plantId,
                                navController = navController
                            )
                        }

                    // Edit Plant Screen
                        composable(
                            route = Screen.EditPlant.route,
                            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
                            if (plantId.isEmpty()) {
                                Text("Plant ID kosong.") // Pesan yang lebih baik
                            } else {
                                val plantViewModel: PlantViewModel = hiltViewModel()
                                EditPlantScreen(
                                    navController = navController,
                                    plantId = plantId,
                                    viewModel = plantViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
