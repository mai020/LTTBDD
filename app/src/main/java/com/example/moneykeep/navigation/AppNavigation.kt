package com.example.moneykeep.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moneykeep.data.local.AppDatabase
import com.example.moneykeep.data.repository.TransactionRepository
import com.example.moneykeep.data.repository.WalletRepository
import com.example.moneykeep.ui.screen.AddTransactionScreen
import com.example.moneykeep.ui.screen.CreateWalletScreen
import com.example.moneykeep.ui.screen.MainScreen
import com.example.moneykeep.ui.screen.WelcomeScreen
import com.example.moneykeep.viewmodel.HomeViewModel
import com.example.moneykeep.viewmodel.ReportViewModel
import com.example.moneykeep.viewmodel.TransactionViewModel
import com.example.moneykeep.viewmodel.WalletViewModel
import com.example.moneykeep.viewmodel.ThemeViewModel

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {

    val context = LocalContext.current
    val navController = rememberNavController()

    val db = remember { AppDatabase.getDatabase(context) }
    val walletRepository = remember { WalletRepository(db.walletDao()) }
    val transactionRepository = remember { TransactionRepository(db.transactionDao(), db.categoryDao()) }


    val walletViewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.Factory(walletRepository, transactionRepository)
    )
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(transactionRepository, walletRepository, db))
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModel.Factory(transactionRepository))


    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModel.Factory(transactionRepository, walletViewModel)
    )

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(navController)
        }
        composable("home") {
            MainScreen(navController, "home", walletViewModel, homeViewModel, reportViewModel, transactionViewModel, themeViewModel)
        }
        composable("wallet") {
            MainScreen(navController, "wallet", walletViewModel, homeViewModel, reportViewModel, transactionViewModel, themeViewModel)
        }
        composable("report") {
            MainScreen(navController, "report", walletViewModel, homeViewModel, reportViewModel, transactionViewModel, themeViewModel)
        }
        composable("more") {
            MainScreen(navController, "more", walletViewModel, homeViewModel, reportViewModel, transactionViewModel, themeViewModel)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController, walletViewModel, transactionViewModel)
        }
        composable("create_wallet") {
            CreateWalletScreen(navController, walletViewModel)
        }
    }
}
