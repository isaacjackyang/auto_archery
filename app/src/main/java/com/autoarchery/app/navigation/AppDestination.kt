package com.autoarchery.app.navigation

sealed interface AppDestination {
    val routePattern: String

    data object LiveSession : AppDestination {
        override val routePattern: String = "live_session/{sessionId}"
        fun route(sessionId: String): String = "live_session/$sessionId"
    }

    data object SessionMetrics : AppDestination {
        override val routePattern: String = "session_metrics/{sessionId}"
        fun route(sessionId: String): String = "session_metrics/$sessionId"
    }

    data object Settings : AppDestination {
        override val routePattern: String = "settings"
    }
}

