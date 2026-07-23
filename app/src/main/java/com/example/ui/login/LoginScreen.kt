package com.example.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserPreferences
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class LoginViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    var name by mutableStateOf("")
        private set
        
    var password by mutableStateOf("")
        private set
        
    var hasError by mutableStateOf(false)
        private set

    fun onNameChange(newName: String) {
        name = newName
        hasError = false
    }
    
    fun onPasswordChange(newPassword: String) {
        password = newPassword
        hasError = false
    }

    fun login(onSuccess: () -> Unit) {
        val validUser = name.trim().equals("BR SOLAR", ignoreCase = true)
        val validPass = password == "BRSOLAR123"
        
        val validAdmin = name.trim().equals("admin", ignoreCase = true)
        val validAdminPass = password == "Miguel123"
        
        if ((validUser && validPass) || (validAdmin && validAdminPass)) {
            viewModelScope.launch {
                val userType = if (validAdmin) "Administrador" else "BR SOLAR"
                userPreferences.saveUserName(userType)
                onSuccess()
            }
        } else {
            hasError = true
        }
    }
}

class LoginViewModelFactory(private val userPreferences: UserPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    userPreferences: UserPreferences
) {
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(userPreferences))
    val userName by userPreferences.userName.collectAsState(initial = null)

    LaunchedEffect(userName) {
        if (!userName.isNullOrBlank()) {
            onNavigateToDashboard()
        }
    }

    if (userName.isNullOrBlank()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.br_solar),
                            contentDescription = "Logo BR Solar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "BR Solar",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sistema de Vistorias",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Usuário") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = viewModel.hasError,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Senha") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = viewModel.hasError,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (viewModel.hasError) {
                        Text(
                            text = "Usuário ou senha incorretos",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.Start)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.login(onNavigateToDashboard) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = viewModel.name.isNotBlank() && viewModel.password.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Entrar", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text(
                        text = "Create by Midnight Studios.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
