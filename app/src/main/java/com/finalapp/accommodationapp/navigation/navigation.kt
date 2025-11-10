package com.finalapp.accommodationapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finalapp.accommodationapp.data.UserSession
import com.finalapp.accommodationapp.screens.AddPropertyScreen
import com.finalapp.accommodationapp.screens.RegisterScreen
import com.finalapp.accommodationapp.screens.admin.AdminAddStudentScreen
import com.finalapp.accommodationapp.screens.admin.AdminDashboardScreen
import com.finalapp.accommodationapp.screens.admin.AdminLandlordListScreen
import com.finalapp.accommodationapp.screens.admin.AdminPropertyListScreen
import com.finalapp.accommodationapp.screens.admin.AdminStudentListScreen
import com.finalapp.accommodationapp.screens.admin.AdminUniversityListScreen
import com.finalapp.accommodationapp.screens.landlord.AddLandlordScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordAddPropertyScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordBookingManagementScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordEditProfileScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordEditPropertyScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordHomeScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordProfileCompletionScreen
import com.finalapp.accommodationapp.screens.landlord.LandlordProfileScreen
import com.finalapp.accommodationapp.screens.student.HomeScreen
import com.finalapp.accommodationapp.screens.student.LoginScreen
import com.finalapp.accommodationapp.screens.student.MapScreen
import com.finalapp.accommodationapp.screens.student.ProfileCompletionScreen
import com.finalapp.accommodationapp.screens.student.PropertyDetailScreen
import com.finalapp.accommodationapp.screens.student.StudentBookingsScreen
import com.finalapp.accommodationapp.screens.student.StudentEditProfileScreen
import com.finalapp.accommodationapp.screens.student.StudentFavoritesScreen
import com.finalapp.accommodationapp.screens.student.StudentProfileScreen
import com.finalapp.accommodationapp.screens.student.UniversitySelectionScreen

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
                onBookingsClick = {
                    navController.navigate(Screen.StudentBookings.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.StudentProfile.route)
                },
                onFavoritesClick = {
                    navController.navigate(Screen.StudentFavorites.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onMapClick = {
                    navController.navigate(Screen.StudentMap.route)
                }
            )
        }

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

                },
                onEditClick = if (UserSession.currentUser?.userType == "landlord") {
                    { id ->
                        navController.navigate("${Screen.LandlordEditProperty.route}/$id")
                    }
                } else null
            )
        }

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
                    navController.navigate(Screen.AdminAddLandlord.route)
                }
            )
        }

        composable(Screen.AdminProperties.route) {
            AdminPropertyListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddProperty = {
                    navController.navigate(Screen.AdminAddProperty.route)
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

        composable(Screen.LandlordProfileCompletion.route) {
            LandlordProfileCompletionScreen(
                onProfileComplete = {
                    navController.navigate(Screen.LandlordHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

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
                onBookingsClick = {
                    navController.navigate(Screen.LandlordBookings.route)
                },
                onProfileClick = {

                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

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

        composable(Screen.AdminAddStudent.route) {
            AdminAddStudentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStudentAdded = {
                    navController.popBackStack()
                }
            )
        }

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

        composable(Screen.AdminAddLandlord.route) {
            AddLandlordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLandlordAdded = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminAddProperty.route) {
            AddPropertyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyAdded = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.StudentProfile.route) {
            StudentProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditProfile = {
                    navController.navigate(Screen.StudentEditProfile.route)
                }
            )
        }

        composable(Screen.StudentBookings.route) {
            StudentBookingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.StudentBookings.route) { inclusive = true }
                    }
                },
                onFavoritesClick = {
                    navController.navigate(Screen.StudentFavorites.route) {
                        popUpTo(Screen.StudentBookings.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StudentFavorites.route) {
            StudentFavoritesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.StudentFavorites.route) { inclusive = true }
                    }
                },
                onBookingsClick = {
                    navController.navigate(Screen.StudentBookings.route) {
                        popUpTo(Screen.StudentFavorites.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StudentMap.route) {
            MapScreen(
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.StudentMap.route) { inclusive = true }
                    }
                },
                onBookingsClick = {
                    navController.navigate(Screen.StudentBookings.route) {
                        popUpTo(Screen.StudentMap.route) { inclusive = true }
                    }
                },
                onFavoritesClick = {
                    navController.navigate(Screen.StudentFavorites.route) {
                        popUpTo(Screen.StudentMap.route) { inclusive = true }
                    }
                },
                onProfileClick = {
                    navController.navigate(Screen.StudentProfile.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LandlordBookings.route) {
            LandlordBookingManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPropertyClick = { propertyId ->
                    navController.navigate("${Screen.PropertyDetail.route}/$propertyId")
                },
                onHomeClick = {
                    navController.navigate(Screen.LandlordHome.route) {
                        popUpTo(Screen.LandlordBookings.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LandlordProfile.route) {
            LandlordProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditProfile = {
                    navController.navigate(Screen.LandlordEditProfile.route)
                }
            )
        }

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
                onBookingsClick = {
                    navController.navigate(Screen.LandlordBookings.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.LandlordProfile.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StudentEditProfile.route) {
            StudentEditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileUpdated = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.LandlordEditProfile.route) {
            LandlordEditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileUpdated = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")

    //Student screens
    object UniversitySelection : Screen("university_selection")
    object ProfileCompletion : Screen("profile_completion")
    object Home : Screen("home")
    object StudentEditProfile : Screen("student_edit_profile")
    object StudentProfile : Screen("student_profile")
    object StudentBookings : Screen("student_bookings")
    object StudentFavorites : Screen("student_favorites")
    object StudentMap : Screen("student_map")

    //Landlord screens
    object LandlordProfileCompletion : Screen("landlord_profile_completion")
    object LandlordEditProperty : Screen("landlord_edit_property")
    object LandlordHome : Screen("landlord_home")
    object LandlordProfile : Screen("landlord_profile")
    object LandlordBookings : Screen("landlord_bookings")
    object LandlordAddProperty : Screen("landlord_add_property")
    object PropertyDetail : Screen("property_detail")
    object LandlordEditProfile : Screen("landlord_edit_profile")

    // Admin screens
    object AdminDashboard : Screen("admin_dashboard")
    object AdminStudents : Screen("admin_students")
    object AdminLandlords : Screen("admin_landlords")
    object AdminProperties : Screen("admin_properties")
    object AdminUniversities : Screen("admin_universities")
    object AdminAddStudent : Screen("admin_add_student")
    object AdminAddLandlord : Screen("add_landlord")
    object AdminAddProperty : Screen("add_property")
}