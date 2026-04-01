package com.autoarchery.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.autoarchery.app.navigation.AutoArcheryNavHost

@Composable
fun AutoArcheryApp() {
    val navController = rememberNavController()
    AutoArcheryNavHost(navController = navController)
}

