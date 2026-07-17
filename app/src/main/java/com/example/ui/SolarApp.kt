package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.SolarRepository
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.wizard.WizardScreen
import com.example.ui.wizard.WizardViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.UserPreferences
import com.example.ui.login.LoginScreen

@Composable
fun SolarApp() {
    MyApplicationTheme(dynamicColor = false) {
        val context = LocalContext.current
        val database = remember { AppDatabase.getDatabase(context) }
        val repository = remember { SolarRepository(database) }
        val factory = remember { AppViewModelFactory(repository) }
        val userPreferences = remember { UserPreferences(context) }
        val userName by userPreferences.userName.collectAsState(initial = null)
        
        val navController = rememberNavController()
        
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("login") {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    userPreferences = userPreferences
                )
            }
            
            composable("dashboard") {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    userName = userName ?: "",
                    onNavigateToWizard = { inspectionId ->
                        navController.navigate("wizard/$inspectionId")
                    }
                )
            }
            
            composable("wizard/{inspectionId}") { backStackEntry ->
                val inspectionId = backStackEntry.arguments?.getString("inspectionId")?.toLongOrNull() ?: return@composable
                val wizardViewModel: WizardViewModel = viewModel(factory = factory)
                WizardScreen(
                    viewModel = wizardViewModel,
                    inspectionId = inspectionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
