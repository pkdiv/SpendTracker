package dev.pkdiv.spendtracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.ui.history.HistoryScreen
import dev.pkdiv.spendtracker.ui.home.HomeScreen
import dev.pkdiv.spendtracker.ui.reports.ReportsScreen
import dev.pkdiv.spendtracker.ui.settings.SettingsScreen
import dev.pkdiv.spendtracker.ui.unrecognized.UnrecognizedScreen

private data class Destination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val destinations = listOf(
    Destination("home", R.string.nav_home, Icons.Filled.Home),
    Destination("history", R.string.nav_history, Icons.Filled.List),
    Destination("reports", R.string.nav_reports, Icons.Filled.PieChart),
    Destination("unrecognized", R.string.nav_unrecognized, Icons.Filled.Warning),
    Destination("settings", R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun SpendTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") { HomeScreen() }
            composable("history") { HistoryScreen() }
            composable("reports") { ReportsScreen() }
            composable("unrecognized") { UnrecognizedScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
