package com.example.moneykeep.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.moneykeep.viewmodel.WalletViewModel

private fun formatMoneyLocal(value: String): String {
    val digits = value.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val normalized = digits.trimStart('0').ifEmpty { "0" }
    return normalized.reversed().chunked(3).joinToString(".").reversed()
}

@Composable
fun CreateWalletScreen(
    navController: NavHostController,
    walletViewModel: WalletViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by walletViewModel.uiState.collectAsState()

    var balanceValue by remember { mutableStateOf(TextFieldValue(uiState.initialBalance)) }
    var spendingLimitValue by remember { mutableStateOf(TextFieldValue(uiState.spendingLimit)) }

    LaunchedEffect(uiState.initialBalance) {
        if (balanceValue.text != uiState.initialBalance) {
            balanceValue = TextFieldValue(
                text = uiState.initialBalance,
                selection = TextRange(uiState.initialBalance.length)
            )
        }
    }

    LaunchedEffect(uiState.spendingLimit) {
        if (spendingLimitValue.text != uiState.spendingLimit) {
            spendingLimitValue = TextFieldValue(
                text = uiState.spendingLimit,
                selection = TextRange(uiState.spendingLimit.length)
            )
        }
    }

    LaunchedEffect(uiState.isSaveSuccess, uiState.isMergeSuccess) {
        if (uiState.isSaveSuccess || uiState.isMergeSuccess) {
            walletViewModel.resetSaveSuccess()
            navController.popBackStack()
        }
    }

    var showMergeDialog by remember { mutableStateOf(false) }

    if (showMergeDialog && uiState.editingWallet != null) {
        val otherWallets = uiState.wallets.filter { it.id != uiState.editingWallet?.id }
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Chọn ví để gộp vào", fontWeight = FontWeight.Bold) },
            text = {
                if (otherWallets.isEmpty()) {
                    Text("Không có ví nào khác để gộp.")
                } else {
                    Column {
                        Text("Số dư và tất cả giao dịch của ví hiện tại sẽ được chuyển sang ví được chọn.")
                        Spacer(modifier = Modifier.height(12.dp))
                        otherWallets.forEach { wallet ->
                            TextButton(
                                onClick = {
                                    walletViewModel.mergeWallet(uiState.editingWallet!!, wallet)
                                    showMergeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(wallet.name, color = colorScheme.onSurface)
                                    Text(
                                        "${formatMoneyLocal(wallet.balance.toLong().toString())}đ",
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            HorizontalDivider(color = colorScheme.outlineVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
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
                .padding(top = 48.dp, bottom = 24.dp)
        ) {
            Text(
                text = if (uiState.editingWallet == null) "Thêm Ví Mới" else "Chỉnh Sửa Ví",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            // ✅ FIX: dùng colorScheme.surface thay vì Color.White cứng
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = uiState.walletName,
                    onValueChange = walletViewModel::onWalletNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên ví") },
                    placeholder = { Text("Ví dụ: Tiền tiết kiệm") },
                    leadingIcon = { Icon(Icons.Default.Title, null, tint = colorScheme.primary) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = balanceValue,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }
                        if (digits.length <= 15) {
                            val formatted = formatMoneyLocal(digits)
                            balanceValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                            walletViewModel.onInitialBalanceChange(digits)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (uiState.editingWallet == null) "Số dư ban đầu" else "Số dư") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = colorScheme.primary)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = spendingLimitValue,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }
                        if (digits.length <= 15) {
                            val formatted = formatMoneyLocal(digits)
                            spendingLimitValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                            walletViewModel.onSpendingLimitChange(digits)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mức cảnh báo chi tiêu") },
                    placeholder = { Text("0") },
                    leadingIcon = {
                        Icon(Icons.Default.NotificationsActive, null, tint = colorScheme.primary)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                uiState.errorMessage?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            walletViewModel.clearError()
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Hủy")
                    }

                    if (uiState.editingWallet != null) {
                        OutlinedButton(
                            onClick = { showMergeDialog = true },
                            modifier = Modifier.weight(1.2f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.secondary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.CallMerge, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gộp Ví", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = { walletViewModel.saveWallet() },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1.6f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.editingWallet == null) "Lưu ví" else "Cập nhật",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}