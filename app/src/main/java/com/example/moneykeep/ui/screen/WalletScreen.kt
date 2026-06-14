package com.example.moneykeep.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.moneykeep.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WalletScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    walletViewModel: WalletViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by walletViewModel.uiState.collectAsState()
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    }

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(colorScheme.primary, colorScheme.secondary)
                        )
                    )
                    .padding(top = 24.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Ví Tiền Của Tôi",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                items(uiState.wallets, key = { it.id }) { wallet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = currencyFormatter.format(wallet.balance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }

                            Row {
                                IconButton(onClick = {
                                    walletViewModel.prepareEditWallet(wallet)
                                    navController.navigate("create_wallet")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    walletViewModel.prepareCreateWallet()
                    navController.navigate("create_wallet")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thêm Ví Tiền",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay Loading
        if (uiState.isLoading || uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Đang xử lý dữ liệu...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error Dialog
        uiState.errorMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { walletViewModel.clearError() },
                icon = { Icon(Icons.Default.Warning, null, tint = colorScheme.error) },
                title = { Text("Lỗi") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { walletViewModel.clearError() }) { Text("Đóng") }
                }
            )
        }
    }
}
