package com.autoarchery.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.autoarchery.app.ui.live.LiveSessionRoute
import com.autoarchery.app.ui.metrics.MetricsRoute
import com.autoarchery.app.ui.settings.SettingsRoute

private const val ActiveSessionId = "active"

@Composable
fun AutoArcheryNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.LiveSession.route(ActiveSessionId),
    ) {
        composable(AppDestination.LiveSession.routePattern) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ActiveSessionId
            LiveSessionRoute(
                sessionId = sessionId,
                onOpenMetrics = { selectedSessionId ->
                    navController.navigate(AppDestination.SessionMetrics.route(selectedSessionId))
                },
                onOpenSettings = { navController.navigate(AppDestination.Settings.routePattern) },
            )
        }

        composable(AppDestination.SessionMetrics.routePattern) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ActiveSessionId
            MetricsRoute(
                sessionId = sessionId,
                onBackToLive = { navController.popBackStack() },
            )
        }

        composable(AppDestination.Settings.routePattern) {
            SettingsRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
