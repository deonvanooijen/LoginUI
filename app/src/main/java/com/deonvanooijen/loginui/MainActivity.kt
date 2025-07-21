package com.deonvanooijen.loginui

import RegistrationScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deonvanooijen.loginui.ui.screens.HomeScreen
import com.deonvanooijen.loginui.ui.screens.LoginScreen
import com.deonvanooijen.loginui.ui.theme.LoginUITheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()

        setContent {
            LoginUITheme {
                AppNavigation(auth = auth)
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth) {
    val navController = rememberNavController()
    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(auth = auth, navController = navController)
        }
        composable("registration") {
            RegistrationScreen(auth = auth, navController = navController)
        }
        composable("home") {
            HomeScreen(auth = auth, navController = navController)
        }
    }
}