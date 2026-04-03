package com.cannatrace.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cannatrace.presentation.auth.LoginScreen
import com.cannatrace.presentation.batch.BatchDetailScreen
import com.cannatrace.presentation.batch.BatchListScreen
import com.cannatrace.presentation.batch.CreateBatchScreen
import com.cannatrace.presentation.culture.CultureJournalScreen
import com.cannatrace.presentation.dashboard.DashboardScreen
import com.cannatrace.presentation.reports.ReportScreen
import com.cannatrace.presentation.scanner.QrScannerScreen
import com.cannatrace.presentation.stock.StockScreen

@Composable
fun CannaTraceNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Authentification
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Tableau de bord
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToBatches = { navController.navigate(Screen.BatchList.route) },
                    onNavigateToScanner = { navController.navigate(Screen.QrScanner.route) }
                )
            }

            // Liste des lots
            composable(Screen.BatchList.route) {
                BatchListScreen(
                    onNavigateToDetail = { batchId ->
                        navController.navigate(Screen.BatchDetail.createRoute(batchId))
                    },
                    onNavigateToCreate = { navController.navigate(Screen.CreateBatch.route) }
                )
            }

            // Création d'un lot
            composable(Screen.CreateBatch.route) {
                CreateBatchScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Détail d'un lot
            composable(
                route = Screen.BatchDetail.route,
                arguments = listOf(navArgument("batchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val batchId = backStackEntry.arguments?.getString("batchId") ?: return@composable
                BatchDetailScreen(
                    batchId = batchId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCulture = { id ->
                        navController.navigate(Screen.CultureJournal.createRoute(id))
                    }
                )
            }

            // Journal de culture
            composable(
                route = Screen.CultureJournal.route,
                arguments = listOf(navArgument("batchId") { type = NavType.StringType })
            ) { backStackEntry ->
                val batchId = backStackEntry.arguments?.getString("batchId") ?: return@composable
                CultureJournalScreen(
                    batchId = batchId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Stock
            composable(Screen.Stock.route) {
                StockScreen()
            }

            // Scanner QR
            composable(Screen.QrScanner.route) {
                QrScannerScreen(
                    onScanResult = { scannedValue ->
                        navController.popBackStack()
                        // Naviguer vers le détail si c'est un lot connu
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Rapports
            composable(Screen.Reports.route) {
                ReportScreen()
            }
        }
    }
}
