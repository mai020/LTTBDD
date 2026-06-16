package com.example.moneykeep.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.viewmodel.HomeFilter
import com.example.moneykeep.viewmodel.HomeViewModel
import com.example.moneykeep.viewmodel.TransactionViewModel
import com.example.moneykeep.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class TransactionItem(
    val title: String,
    val amount: String,
    val date: String,
    val isExpense: Boolean,
    val walletName: String,
    val originalTransaction: Transaction
)

fun millisToDateString(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
}

fun dateStringToMillis(str: String): Long? = try {
    val p = str.split("/")
    Calendar.getInstance().apply {
        set(p[2].toInt(), p[1].toInt() - 1, p[0].toInt())
    }.timeInMillis
} catch (e: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteDatePickerDialog(
    title: String,
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(millisToDateString(it)) }
                onDismiss()
            }) { Text("Chọn", color = colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = colorScheme.onSurfaceVariant)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = colorScheme.surface)
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    title,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            colors = DatePickerDefaults.colors(
                containerColor = colorScheme.surface,
                titleContentColor = colorScheme.primary,
                headlineContentColor = colorScheme.onSurface,
                weekdayContentColor = colorScheme.onSurfaceVariant,
                dayContentColor = colorScheme.onSurface,
                selectedDayContainerColor = colorScheme.primary,
                selectedDayContentColor = colorScheme.onPrimary,
                todayContentColor = colorScheme.primary,
                todayDateBorderColor = colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    paddingValues: PaddingValues,
    walletViewModel: WalletViewModel,
    homeViewModel: HomeViewModel,
    transactionViewModel: TransactionViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val walletUiState by walletViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val transactionUiState by transactionViewModel.uiState.collectAsState()
    val allTransactions by transactionViewModel.transactions.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    LaunchedEffect(homeUiState.userName) {
        showNameDialog = homeUiState.userName.isBlank()
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var tempFilter by remember { mutableStateOf(homeUiState.filter) }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    var showNotificationDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    val activeFilterCount = listOf(
        homeUiState.filter.dateFrom.isNotEmpty() || homeUiState.filter.dateTo.isNotEmpty(),
        homeUiState.filter.selectedWallets.isNotEmpty(),
        homeUiState.filter.selectedType != "Tất cả",
        homeUiState.filter.selectedCategories.isNotEmpty()
    ).count { it }

    val transactionList = homeUiState.recentTransactions.map { tx ->
        val sign = if (tx.amount < 0) "-" else "+"
        TransactionItem(
            title = tx.category,
            amount = "$sign${currencyFormatter.format(kotlin.math.abs(tx.amount))}",
            date = tx.date,
            isExpense = tx.amount < 0,
            walletName = tx.walletName,
            originalTransaction = tx
        )
    }

    val latestNotifications = allTransactions.take(10)

    // Dialogs
    if (showFromPicker) {
        WhiteDatePickerDialog(
            title = "Từ ngày",
            initialMillis = dateStringToMillis(tempFilter.dateFrom),
            onDismiss = { showFromPicker = false },
            onConfirm = { tempFilter = tempFilter.copy(dateFrom = it) }
        )
    }
    if (showToPicker) {
        WhiteDatePickerDialog(
            title = "Đến ngày",
            initialMillis = dateStringToMillis(tempFilter.dateTo),
            onDismiss = { showToPicker = false },
            onConfirm = { tempFilter = tempFilter.copy(dateTo = it) }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Chào mừng bạn") },
            text = {
                Column {
                    Text("Hãy nhập tên của bạn")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputName, onValueChange = { inputName = it },
                        singleLine = true, placeholder = { Text("Ví dụ: Nguyen Van A") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputName.isNotBlank()) {
                        homeViewModel.setUserName(inputName.trim())
                        showNameDialog = false
                    }
                }) { Text("Tiếp tục") }
            }
        )
    }

    walletUiState.overBudgetWalletName?.let { walletName ->
        AlertDialog(
            onDismissRequest = { walletViewModel.clearOverBudgetAlert() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cảnh Báo Ngân Sách", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column {
                    Text("Bạn đã đạt đến mức cảnh báo chi tiêu của ví: $walletName.", fontSize = 15.sp)
                    Text("Vui lòng kiểm tra lại các khoản chi của mình.", fontSize = 15.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { walletViewModel.clearOverBudgetAlert() }) {
                    Text("Tôi đã hiểu", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = colorScheme.primary, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Thông Báo Giao Dịch", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (latestNotifications.isEmpty()) {
                    Text("Chưa có giao dịch thu/chi nào.", color = colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(latestNotifications, key = { it.id }) { tx ->
                            val isExpense = tx.amount < 0
                            val actionText = if (isExpense) "Đã chi" else "Đã thu"
                            val amountText = currencyFormatter.format(kotlin.math.abs(tx.amount))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                                        .background(if (isExpense) Color(0xFFD32F2F).copy(alpha = 0.1f) else Color(0xFF2E7D32).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        null,
                                        tint = if (isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "$actionText $amountText",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                    )
                                    Text("${tx.category} • ${tx.walletName} • ${tx.date}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(color = colorScheme.outlineVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Xóa giao dịch") },
            text = {
                val amountText = currencyFormatter.format(kotlin.math.abs(tx.amount))
                Text("Bạn có chắc muốn xóa khoản ${if (tx.amount < 0) "chi" else "thu"} $amountText này?")
            },
            confirmButton = {
                TextButton(onClick = {
                    transactionViewModel.deleteTransactionFromHistory(tx)
                    transactionToDelete = null
                }) { Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Hủy") }
            }
        )
    }

    // Main UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(colorScheme.background)
    ) {
        // HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Brush.verticalGradient(colors = listOf(colorScheme.primary, colorScheme.secondary)))
                .padding(top = 24.dp, bottom = 70.dp, start = 20.dp, end = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Xin Chào!", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge)
                        Text(homeUiState.userName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    BadgedBox(
                        badge = {
                            if (latestNotifications.isNotEmpty()) {
                                Badge { Text(latestNotifications.size.coerceAtMost(9).toString()) }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.NotificationsNone, null, tint = Color.White)
                        }
                    }
                }
            }
        }

        // CONTENT BODY
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .offset(y = (-45).dp)
        ) {
            // TỔNG SỐ DƯ CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Số Dư Hiện Tại", color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        currencyFormatter.format(walletUiState.totalBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryItem(Modifier.weight(1f), "Thu Nhập", "+${currencyFormatter.format(homeUiState.totalIncome)}", Color(0xFF1976D2), Icons.Default.ArrowUpward)
                        SummaryItem(Modifier.weight(1f), "Chi Tiêu", "-${currencyFormatter.format(kotlin.math.abs(homeUiState.totalExpense))}", Color(0xFFD32F2F), Icons.Default.ArrowDownward)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TITLE LỊCH SỬ + NÚT LỌC
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lịch Sử Giao Dịch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeFilterCount > 0) {
                        TextButton(onClick = { homeViewModel.updateFilter(HomeFilter()) }) {
                            Text("Xóa lọc", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                    BadgedBox(badge = { if (activeFilterCount > 0) Badge { Text("$activeFilterCount") } }) {
                        FilledTonalButton(
                            onClick = { tempFilter = homeUiState.filter; showFilterSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lọc", fontSize = 13.sp)
                        }
                    }
                }
            }

            if (activeFilterCount > 0) {
                Text("${transactionList.size} giao dịch phù hợp", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            }

            // TRANSACTION LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (transactionList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(8.dp))
                                Text("Không có giao dịch phù hợp", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(transactionList) { item ->
                        TransactionRow(
                            item = item,
                            onEdit = {
                                transactionViewModel.prepareEditTransaction(item.originalTransaction)
                                navController.navigate("add_transaction")
                            },
                            onDelete = {
                                transactionToDelete = item.originalTransaction
                            }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet Bộ Lọc
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bộ lọc", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    TextButton(onClick = { tempFilter = HomeFilter() }) {
                        Text("Xóa tất cả", color = Color.Red)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // 1. Thời gian
                Text("📅  Thời gian", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showFromPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (tempFilter.dateFrom.isEmpty()) "Từ ngày" else tempFilter.dateFrom, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showToPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (tempFilter.dateTo.isEmpty()) "Đến ngày" else tempFilter.dateTo, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // 2. Loại giao dịch
                Text("💰  Loại giao dịch", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Tất cả", "Thu", "Chi").forEach { type ->
                        val selected = tempFilter.selectedType == type
                        FilterChip(
                            selected = selected,
                            onClick = { tempFilter = tempFilter.copy(selectedType = type) },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (type) {
                                    "Thu" -> Color(0xFF2E7D32)
                                    "Chi" -> Color(0xFFC62828)
                                    else -> colorScheme.primary
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // 3. Ví
                Text("👛  Ví", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                if (walletUiState.wallets.isEmpty()) {
                    Text("Không có ví nào", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(walletUiState.wallets) { wallet ->
                            val selected = wallet.name in tempFilter.selectedWallets
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val newSet = tempFilter.selectedWallets.toMutableSet()
                                    if (selected) newSet.remove(wallet.name) else newSet.add(wallet.name)
                                    tempFilter = tempFilter.copy(selectedWallets = newSet)
                                },
                                label = { Text(wallet.name) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // 4. Danh mục
                Text("🏷️  Danh mục", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactionUiState.categoryOptions) { category ->
                        val selected = category in tempFilter.selectedCategories
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val newSet = tempFilter.selectedCategories.toMutableSet()
                                if (selected) newSet.remove(category) else newSet.add(category)
                                tempFilter = tempFilter.copy(selectedCategories = newSet)
                            },
                            label = { Text(category) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { 
                        homeViewModel.updateFilter(tempFilter)
                        showFilterSheet = false 
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Áp dụng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, amount: String, color: Color, icon: ImageVector) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
            Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

@Composable
fun TransactionRow(item: TransactionItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (item.isExpense) Color(0xFFD32F2F).copy(alpha = 0.1f) else Color(0xFF2E7D32).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, null, tint = if (item.isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text(item.date, color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(item.walletName, color = colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.amount, fontWeight = FontWeight.ExtraBold, color = if (item.isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
