package com.kuronoa.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
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
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.R
import com.kuronoa.expensetracker.ui.dashboard.DashboardScreen
import com.kuronoa.expensetracker.ui.expenses.ExpenseListScreen
import com.kuronoa.expensetracker.ui.settings.SettingsScreen

sealed class Destination(val route: String, val labelRes: Int, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    object Dashboard : Destination("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Expenses : Destination("expenses", R.string.nav_expenses, Icons.Filled.Receipt, Icons.Outlined.Receipt)
    object Settings : Destination("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val bottomDestinations = listOf(Destination.Dashboard, Destination.Expenses, Destination.Settings)

@Composable
fun KuronoaNavHost(app: KuronoaApp) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                bottomDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) dest.filledIcon else dest.outlinedIcon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Destination.Dashboard.route) { DashboardScreen(app) }
            composable(Destination.Expenses.route) { ExpenseListScreen(app) }
            composable(Destination.Settings.route) { SettingsScreen(app) }
        }
    }
}
