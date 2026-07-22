package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.SolarRepository
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.theme.BRSolarTheme
import com.example.ui.wizard.WizardScreen
import com.example.ui.wizard.WizardViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.UserPreferences
import com.example.ui.login.LoginScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun LoadingSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.br_solar),
                    contentDescription = "Logo BR Solar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

class MainViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    private val _userNameState = MutableStateFlow<String?>("LOADING")
    val userNameState: StateFlow<String?> = _userNameState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.userName.collect { name ->
                _userNameState.value = name
            }
        }
    }
}

class MainViewModelFactory(private val userPreferences: UserPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun SolarApp() {
    BRSolarTheme(dynamicColor = false) {
        val context = LocalContext.current
        val database = remember { AppDatabase.getDatabase(context) }
        val repository = remember { SolarRepository(database) }
        val factory = remember { AppViewModelFactory(repository) }
        val userPreferences = remember { UserPreferences(context) }
        
        val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(userPreferences))
        val userName by mainViewModel.userNameState.collectAsState()
        val companyName by userPreferences.companyName.collectAsState(initial = "BR Solar")
        val scope = rememberCoroutineScope()
        
        val navController = rememberNavController()
        
        if (userName == "LOADING") {
            LoadingSplashScreen()
        } else {
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
                        companyName = companyName ?: "BR Solar",
                        userPreferences = userPreferences,
                        onSaveCompanyName = { newName ->
                            scope.launch {
                                userPreferences.saveCompanyName(newName)
                            }
                        },
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
                        companyName = companyName ?: "BR Solar",
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
