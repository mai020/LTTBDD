package com.example.moneykeep.ui.screen

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.moneykeep.viewmodel.HomeViewModel
import com.example.moneykeep.viewmodel.ReportViewModel
import com.example.moneykeep.viewmodel.TransactionViewModel
import com.example.moneykeep.viewmodel.WalletViewModel
import com.example.moneykeep.viewmodel.ThemeViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MainScreen(
    navController: NavHostController,
    currentRoute: String,
    walletViewModel: WalletViewModel,
    homeViewModel: HomeViewModel,
    reportViewModel: ReportViewModel,
    transactionViewModel: TransactionViewModel,
    themeViewModel: ThemeViewModel
) {
    val walletUiState by walletViewModel.uiState.collectAsState()
    val currencyFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    if (walletUiState.lowBalanceWalletName != null) {
        AlertDialog(
            onDismissRequest = { walletViewModel.clearLowBalanceAlert() },
            title = { Text("Cảnh báo ví sắp hết tiền") },
            text = {
                Text(
                    "Ví \"${walletUiState.lowBalanceWalletName}\" chỉ còn " +
                        "${currencyFormatter.format(walletUiState.lowBalanceAmount.toLong())} đ. " +
                        "Số dư đã chạm mức cảnh báo 50.000 đ, bạn nên nạp thêm tiền hoặc kiểm soát chi tiêu."
                )
            },
            confirmButton = {
                Button(onClick = { walletViewModel.clearLowBalanceAlert() }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    val navigateToTab = { route: String ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo("home") {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = {
                        transactionViewModel.prepareAddTransaction()
                        navController.navigate("add_transaction")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    ),
                    modifier = Modifier.offset(y = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navigateToTab("home") },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = currentRoute == "wallet",
                    onClick = { navigateToTab("wallet") },
                    icon = { Icon(Icons.Default.Wallet, null) },
                    label = { Text("Ví") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = currentRoute == "report",
                    onClick = { navigateToTab("report") },
                    icon = { Icon(Icons.Default.PieChart, null) },
                    label = { Text("Thống Kê") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = currentRoute == "more",
                    onClick = { navigateToTab("more") },
                    icon = { Icon(Icons.Default.Menu, null) },
                    label = { Text("Khác") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        when (currentRoute) {
            "wallet" -> {
                WalletScreen(navController, paddingValues, walletViewModel)
            }
            "report" -> {
                ReportScreen(paddingValues, reportViewModel)
            }
            "more" -> {
                MoreScreen(paddingValues, navController, walletViewModel, homeViewModel, transactionViewModel, themeViewModel)
            }
            else -> {
                HomeScreen(navController, paddingValues, walletViewModel, homeViewModel, transactionViewModel)
            }
        }
    }
}
