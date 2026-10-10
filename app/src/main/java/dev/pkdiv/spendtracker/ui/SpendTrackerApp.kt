package dev.pkdiv.spendtracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.pkdiv.spendtracker.MainViewModel
import dev.pkdiv.spendtracker.R
import dev.pkdiv.spendtracker.ui.detail.TransactionDetailScreen
import dev.pkdiv.spendtracker.ui.history.HistoryScreen
import dev.pkdiv.spendtracker.ui.home.HomeScreen
import dev.pkdiv.spendtracker.ui.reports.ReportsScreen
import dev.pkdiv.spendtracker.ui.settings.SettingsScreen
import dev.pkdiv.spendtracker.ui.unrecognized.UnrecognizedScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_REPORTS = "reports"
private const val ROUTE_ALERTS = "unrecognized"
private const val ROUTE_SETTINGS = "settings"

private data class Destination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val destinations = listOf(
    Destination(ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    Destination(ROUTE_HISTORY, R.string.nav_history, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    Destination(ROUTE_REPORTS, R.string.nav_reports, Icons.Filled.PieChart, Icons.Outlined.PieChart),
    Destination(ROUTE_ALERTS, R.string.nav_unrecognized, Icons.Filled.Notifications, Icons.Outlined.Notifications),
    Destination(ROUTE_SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun SpendTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val mainViewModel: MainViewModel = hiltViewModel()
    val unrecognizedCount by mainViewModel.unrecognizedCount.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTab(destination.route) },
                        alwaysShowLabel = true,
                        label = { Text(stringResource(destination.labelRes)) },
                        icon = {
                            val icon = if (selected) destination.selectedIcon else destination.unselectedIcon
                            if (destination.route == ROUTE_ALERTS && unrecognizedCount > 0) {
                                BadgedBox(
                                    badge = { Badge { Text(unrecognizedCount.toString()) } },
                                ) {
                                    Icon(icon, contentDescription = null)
                                }
                            } else {
                                Icon(icon, contentDescription = null)
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(220))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(160)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(160))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(220))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(160)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(160))
            },
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    onNavigateToInsights = { navController.navigateToTab(ROUTE_REPORTS) },
                    onTransactionClick = { id -> navController.navigate("transaction/$id") },
                )
            }
            composable(ROUTE_HISTORY) {
                HistoryScreen(
                    onTransactionClick = { id -> navController.navigate("transaction/$id") },
                )
            }
            composable(ROUTE_REPORTS) { ReportsScreen() }
            composable(ROUTE_ALERTS) { UnrecognizedScreen() }
            composable(ROUTE_SETTINGS) { SettingsScreen() }
            composable(
                route = "transaction/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                TransactionDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
