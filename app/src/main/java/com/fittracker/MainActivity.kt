package com.fittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import com.fittracker.ui.screens.LoginScreen
import com.fittracker.ui.screens.RegisterScreen
import com.fittracker.ui.screens.TrackingScreen
import com.fittracker.ui.screens.ProfileScreen
import com.fittracker.ui.theme.FitTrackerTheme
import com.fittracker.viewmodel.LoginViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitTrackerNav()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitTrackerNav() {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    val isLoggedIn = auth.currentUser != null
    val loginViewModel: LoginViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "tracking" else "login"
    ) {
        composable("login") {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { navController.navigate("tracking") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = loginViewModel,
                onRegisterSuccess = { navController.navigate("tracking") { popUpTo("register") { inclusive = true } } },
                onNavigateToLogin = { navController.navigate("login") { popUpTo("register") { inclusive = true } } }
            )
        }
        composable("tracking") {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("FitTracker") },
                        actions = {
                            IconButton(onClick = { navController.navigate("profile") }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                            }
                        }
                    )
                }
            ) { padding ->
                TrackingScreen()
            }
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("tracking") { inclusive = true }
                        popUpTo("profile") { inclusive = true }
                    }
                }
            )
        }
    }
}