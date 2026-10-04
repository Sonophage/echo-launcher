package com.echo.core.navigation

sealed class NavigationCommand {
    data class Direction(val direction: NavigationDirection) : NavigationCommand()
    data object Confirm : NavigationCommand()
    data object Back : NavigationCommand()
}
