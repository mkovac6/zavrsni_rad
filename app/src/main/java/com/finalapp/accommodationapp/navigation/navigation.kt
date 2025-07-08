package com.finalapp.accommodationapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { userType ->
                    when (userType) {
                        "admin" -> navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }

                        "student" -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }

                        "landlord" -> navController.navigate(Screen.LandlordHome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }

                        else -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
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
                },
                onNavigateToLandlordProfile = {
                    navController.navigate(Screen.LandlordProfileCompletion.route)
                }
            )
        }

        composable(Screen.UniversitySelection.route) {
            UniversitySelectionScreen(
                onUniversitySelected = { universityId ->
                    navController.navigate("${Screen.ProfileCompletion.route}/$universityId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "${Screen.ProfileCompletion.route}/{universityId}",
            arguments = listOf(navArgument("universityId") { type = NavType.IntType })
        ) { backStackEntry ->
            val universityId = backStackEntry.arguments?.getInt("universityId") ?: 0
            ProfileCompletionScreen(
                universityId = universityId,
                onProfileComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                },
                onSearchClick = {
                    // TODO: Navigate to search screen
                },
                onProfileClick = {
                    // TODO: Navigate to profile screen
                },
                onFavoritesClick = {
                    // TODO: Navigate to favorites screen
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Single PropertyDetail route with edit functionality
        composable(
            "${Screen.PropertyDetail.route}/{propertyId}",
            arguments = listOf(navArgument("propertyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getInt("propertyId") ?: 0
            PropertyDetailScreen(
                propertyId = propertyId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBookingClick = {
                    // TODO: Navigate to booking screen
                },
                onEditClick = if (UserSession.currentUser?.userType == "landlord") {
                    { id ->
                        navController.navigate("${Screen.LandlordEditProperty.route}/$id")
                    }
                } else null
            )
        }

        // Admin Routes
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onStudentsClick = {
                    navController.navigate(Screen.AdminStudents.route)
                },
                onLandlordsClick = {
                    navController.navigate(Screen.AdminLandlords.route)
                },
                onPropertiesClick = {
                    navController.navigate(Screen.AdminProperties.route)
                },
                onUniversitiesClick = {
                    navController.navigate(Screen.AdminUniversities.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminStudents.route) {
            AdminStudentListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddStudent = {
                    navController.navigate(Screen.AdminAddStudent.route)
                }
            )
        }

        composable(Screen.AdminLandlords.route) {
            AdminLandlordListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddLandlord = {
                    navController.navigate(Screen.AddLandlord.route)
                }
            )
        }

        composable(Screen.AdminProperties.route) {
            AdminPropertyListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddProperty = {
                    navController.navigate(Screen.AddProperty.route)
                },
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                }
            )
        }

        composable(Screen.AdminUniversities.route) {
            AdminUniversityListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Landlord Profile Completion
        composable(Screen.LandlordProfileCompletion.route) {
            LandlordProfileCompletionScreen(
                onProfileComplete = {
                    navController.navigate(Screen.LandlordHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Landlord Home
        composable(Screen.LandlordHome.route) {
            LandlordHomeScreen(
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                },
                onAddProperty = {
                    navController.navigate(Screen.LandlordAddProperty.route)
                },
                onEditProperty = { propertyId ->
                    navController.navigate("${Screen.LandlordEditProperty.route}/$propertyId")
                },
                onProfileClick = {
                    // TODO: Navigate to landlord profile
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Landlord Add Property
        composable(Screen.LandlordAddProperty.route) {
            LandlordAddPropertyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyAdded = {
                    navController.popBackStack()
                }
            )
        }

        // MISSING ROUTE - Landlord Edit Property
        composable(
            "${Screen.LandlordEditProperty.route}/{propertyId}",
            arguments = listOf(navArgument("propertyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getInt("propertyId") ?: 0
            LandlordEditPropertyScreen(
                propertyId = propertyId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyUpdated = {
                    navController.popBackStack()
                },
                onPropertyDeleted = {
                    navController.navigate(Screen.LandlordHome.route) {
                        popUpTo(Screen.LandlordHome.route) { inclusive = true }
                    }
                }
            )
        }

        // Admin Add Landlord
        composable(Screen.AddLandlord.route) {
            AddLandlordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLandlordAdded = {
                    navController.popBackStack()
                }
            )
        }

        // Admin Add Property
        composable(Screen.AddProperty.route) {
            AddPropertyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyAdded = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object UniversitySelection : Screen("university_selection")
    object ProfileCompletion : Screen("profile_completion")
    object LandlordProfileCompletion : Screen("landlord_profile_completion")
    object LandlordEditProperty : Screen("landlord_edit_property")
    object Home : Screen("home")
    object LandlordHome : Screen("landlord_home")
    object LandlordAddProperty : Screen("landlord_add_property")
    object PropertyDetail : Screen("property_detail")
    object Search : Screen("search")
    object Profile : Screen("profile")
    object Favorites : Screen("favorites")
    object Booking : Screen("booking")

    // Admin screens
    object AdminDashboard : Screen("admin_dashboard")
    object AdminStudents : Screen("admin_students")
    object AdminLandlords : Screen("admin_landlords")
    object AdminProperties : Screen("admin_properties")
    object AdminUniversities : Screen("admin_universities")

    // Admin Add screens
    object AdminAddStudent : Screen("admin_add_student")
    object AddLandlord : Screen("add_landlord")
    object AddProperty : Screen("add_property")
}