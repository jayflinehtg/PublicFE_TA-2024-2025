package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

import com.example.myapplication.AddPlant
import com.example.myapplication.Home
import com.example.myapplication.MainViewModel
import com.example.myapplication.Profile
import com.example.myapplication.Screen

@Composable
fun BottomNavBarScreen(rootNavController: NavHostController, isGuest: Boolean, viewModel: MainViewModel) {
    val bottomNavController = rememberNavController()
    val items = listOf(
        BottomNavItem("home", Icons.Default.Home, "Home"),
        BottomNavItem("addplant", Icons.Default.Add, "Add"),
        BottomNavItem("profile", Icons.Default.Person, "Profile")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = bottomNavController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                if (isGuest && (item.route == "addplant" || item.route == "profile")) {
                                    rootNavController.navigate(Screen.WalletComponent.route) {
                                        popUpTo(rootNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    viewModel.resetGuestState() // Reset isGuest supaya WalletComponent muncul
                                } else {
                                    bottomNavController.navigate(item.route) {
                                        popUpTo(bottomNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                Home(navController = rootNavController)
            }
            composable("addplant") {
                AddPlant(navController = bottomNavController)
            }
            composable("profile") {
                Profile(navController = rootNavController)
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
