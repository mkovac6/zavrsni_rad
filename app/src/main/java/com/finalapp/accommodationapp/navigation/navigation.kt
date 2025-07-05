package com.finalapp.accommodationapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finalapp.accommodationapp.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route // Temporary for testing
    ) {
        composable(Screen.DatabaseTest.route) {
            DatabaseTestScreen()
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToUniversitySelection = {
                    navController.navigate(Screen.UniversitySelection.route)
                }
            )
        }

        composable(Screen.UniversitySelection.route) {
            UniversitySelectionScreen(
                onUniversitySelected = {
                    navController.navigate(Screen.ProfileCompletion.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ProfileCompletion.route) {
            ProfileCompletionScreen(
                onProfileComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onPropertyClick = {
                    navController.navigate(Screen.PropertyDetail.route)
                },
                onNavigateToSearch = {
                    // TODO: Navigate to search screen
                },
                onNavigateToProfile = {
                    // TODO: Navigate to profile screen
                },
                onNavigateToFavorites = {
                    // TODO: Navigate to favorites screen
                }
            )
        }

        composable(Screen.PropertyDetail.route) {
            PropertyDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBookingClick = {
                    // TODO: Navigate to booking screen
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object DatabaseTest : Screen("database_test") // Temporary
    object Login : Screen("login")
    object Register : Screen("register")
    object UniversitySelection : Screen("university_selection")
    object ProfileCompletion : Screen("profile_completion")
    object Home : Screen("home")
    object PropertyDetail : Screen("property_detail")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Favorites : Screen("favorites")
    object Booking : Screen("booking")
}