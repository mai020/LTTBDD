package com.example.moneykeep.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.moneykeep.viewmodel.TransactionType
import com.example.moneykeep.viewmodel.TransactionViewModel
import com.example.moneykeep.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    walletViewModel: WalletViewModel,
    transactionViewModel: TransactionViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val walletUiState by walletViewModel.uiState.collectAsState()
    val transactionUiState by transactionViewModel.uiState.collectAsState()

    val vnLocale = remember { Locale("vi", "VN") }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(vnLocale) }

    var walletMenuWidth by remember { mutableStateOf(0.dp) }
    var categoryMenuWidth by remember { mutableStateOf(0.dp) }

    // ✅ FIX xoay màn hình: dùng rememberSaveable cho tất cả state nhập liệu
    // amount lưu chuỗi số thuần túy (có thể Saveable), amountValue rebuild từ nó
    var amount by rememberSaveable { mutableStateOf("") }
    // TextFieldValue không Serializable nên dùng remember bình thường,
    // nhưng sync lại từ `amount` mỗi khi recompose sau rotation
    var amountValue by remember(amount) {
        val formatted = if (amount.isEmpty()) "" else try {
            NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount.toLong())
        } catch (e: Exception) { amount }
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    var note by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("Chi") }
    var selectedWallet by rememberSaveable { mutableStateOf("") }
    var isWalletExpanded by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Ăn Uống") }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var newCategoryName by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val today = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
    var selectedDate by rememberSaveable { mutableStateOf(today) }

    val isEditMode = transactionUiState.editingTransactionId != null

    val categoryIcons = remember {
        mapOf(
            "Ăn Uống" to Icons.Default.Restaurant,
            "Di Chuyển" to Icons.Default.DirectionsCar,
            "Mua Sắm" to Icons.Default.ShoppingBag,
            "Hóa Đơn" to Icons.AutoMirrored.Filled.ReceiptLong,
            "Giải Trí" to Icons.Default.SportsEsports,
            "Sức Khỏe" to Icons.Default.MedicalServices,
            "Giáo Dục" to Icons.Default.School,
            "Lương" to Icons.Default.Payments,
            "Khác" to Icons.Default.MoreHoriz
        )
    }

    LaunchedEffect(walletUiState.wallets) {
        if (selectedWallet.isBlank() || walletUiState.wallets.none { it.name == selectedWallet }) {
            selectedWallet = walletUiState.wallets.firstOrNull()?.name.orEmpty()
        }
    }

    LaunchedEffect(transactionUiState.editingTransactionId) {
        if (isEditMode) {
            val rawAmt = transactionUiState.amount
            // ✅ Chỉ set amount (String), remember(amount) tự rebuild amountValue
            amount = rawAmt
            note = transactionUiState.note
            selectedType = if (transactionUiState.selectedType == TransactionType.INCOME) "Thu" else "Chi"
            selectedWallet = transactionUiState.selectedWallet
            selectedCategory = transactionUiState.selectedCategory
            selectedDate = transactionUiState.selectedDate
        }
    }

    LaunchedEffect(transactionUiState.isSaveSuccess, transactionUiState.isDeleteSuccess) {
        if (transactionUiState.isSaveSuccess || transactionUiState.isDeleteSuccess) {
            transactionViewModel.resetSaveSuccess()
            navController.popBackStack()
        }
    }

    val selectedWalletBalance = walletUiState.wallets.firstOrNull { it.name == selectedWallet }?.balance
    val selectedWalletText = selectedWalletBalance?.let {
        "$selectedWallet | ${currencyFormatter.format(it)}"
    } ?: selectedWallet

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        selectedDate = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            },
            // ✅ FIX: dùng colorScheme.surface thay vì Color.White cứng
            colors = DatePickerDefaults.colors(containerColor = colorScheme.surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    // ✅ FIX: dùng colorScheme.surface thay vì Color.White cứng
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.primary,
                    headlineContentColor = colorScheme.primary,
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

    fun saveNewCategory() {
        val category = newCategoryName.trim()
        if (category.isNotBlank()) {
            transactionViewModel.addNewCategory(category)
            selectedCategory = category
            newCategoryName = ""
            isCategoryExpanded = false
        }
    }

    fun saveCurrentTransaction() {
        transactionViewModel.onAmountChange(amount)
        transactionViewModel.onNoteChange(note)
        transactionViewModel.onTypeChange(if (selectedType == "Thu") TransactionType.INCOME else TransactionType.EXPENSE)
        transactionViewModel.onWalletChange(selectedWallet)
        transactionViewModel.onCategoryChange(selectedCategory)
        transactionViewModel.onDateChange(selectedDate)
        transactionViewModel.saveTransaction()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Brush.verticalGradient(colors = listOf(colorScheme.primary, colorScheme.secondary)))
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            Text(
                text = if (isEditMode) "Sửa Giao Dịch" else "Thêm Giao Dịch",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            // ✅ FIX: dùng colorScheme.surface thay vì Color.White cứng
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Tab Thu/Chi
                Row(
                    modifier = Modifier
                        .fillMaxWidth(.7f)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = .5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Thu", "Chi").forEach { type ->
                        val isSelected = selectedType == type
                        Button(
                            onClick = {
                                selectedType = type
                                transactionViewModel.clearError()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) (if (type == "Thu") Color(0xFF1976D2) else Color(0xFFD32F2F)) else Color.Transparent,
                                contentColor = if (isSelected) Color.White else colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(text = type, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nhập số tiền
                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }
                        if (digits.length <= 15) {
                            // ✅ Chỉ cần cập nhật `amount` (String), remember(amount) sẽ tự rebuild amountValue
                            amount = digits
                            transactionViewModel.clearError()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số tiền") },
                    placeholder = { Text("0") },
                    leadingIcon = { Icon(Icons.Default.Payments, null, tint = colorScheme.primary) },
                    trailingIcon = { Text("đ", color = colorScheme.primary, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ví
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> walletMenuWidth = with(density) { size.width.toDp() } }) {
                    OutlinedTextField(
                        value = selectedWalletText, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Ví") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = colorScheme.primary) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = colorScheme.primary) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { isWalletExpanded = true })
                    DropdownMenu(
                        expanded = isWalletExpanded,
                        onDismissRequest = { isWalletExpanded = false },
                        // ✅ FIX: dùng colorScheme.surface thay vì Color.White cứng
                        modifier = Modifier.width(walletMenuWidth).background(colorScheme.surface)
                    ) {
                        walletUiState.wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text("${wallet.name} (${currencyFormatter.format(wallet.balance)})") },
                                onClick = {
                                    selectedWallet = wallet.name
                                    isWalletExpanded = false
                                    transactionViewModel.clearError()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Danh mục
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> categoryMenuWidth = with(density) { size.width.toDp() } }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Danh mục") },
                        leadingIcon = {
                            val icon = categoryIcons[selectedCategory] ?: Icons.Default.Category
                            Icon(icon, null, tint = colorScheme.primary)
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = colorScheme.primary) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { isCategoryExpanded = true })
                    DropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false },
                        // ✅ FIX: dùng colorScheme.surface thay vì Color.White cứng
                        modifier = Modifier.width(categoryMenuWidth).background(colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newCategoryName, onValueChange = { newCategoryName = it },
                                modifier = Modifier.weight(1f), placeholder = { Text("Mới...") },
                                singleLine = true, shape = RoundedCornerShape(8.dp)
                            )
                            IconButton(onClick = { saveNewCategory() }, enabled = newCategoryName.isNotBlank()) {
                                Icon(Icons.Default.AddCircle, null, tint = if (newCategoryName.isNotBlank()) colorScheme.primary else colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                        transactionUiState.categoryOptions.forEach { category ->
                            val isDefault = categoryIcons.containsKey(category)
                            DropdownMenuItem(
                                leadingIcon = {
                                    val icon = categoryIcons[category] ?: Icons.Default.Category
                                    Icon(icon, null, tint = if (category == selectedCategory) colorScheme.primary else colorScheme.onSurfaceVariant)
                                },
                                text = {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(category, modifier = Modifier.weight(1f))
                                        if (!isDefault) {
                                            IconButton(onClick = { transactionViewModel.requestDeleteCategory(category) }, Modifier.size(24.dp)) {
                                                Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                },
                                onClick = { selectedCategory = category; isCategoryExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ngày giao dịch
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ngày giao dịch") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = colorScheme.primary) },
                        trailingIcon = { Icon(Icons.Default.EditCalendar, null, tint = colorScheme.primary) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ghi chú
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Ghi chú") },
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = colorScheme.primary) },
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )

                if (selectedType == "Chi" && transactionUiState.errorMessage != null) {
                    Text(
                        text = transactionUiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { saveCurrentTransaction() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !transactionUiState.isSaving
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditMode) "Cập nhật giao dịch" else "Lưu giao dịch", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                transactionViewModel.clearError()
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hủy")
                        }

                        if (isEditMode) {
                            OutlinedButton(
                                onClick = { transactionViewModel.requestDeleteTransaction() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Xóa")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (transactionUiState.showDeleteTransactionDialog) {
        AlertDialog(
            onDismissRequest = { transactionViewModel.cancelDeleteTransaction() },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa giao dịch này không? Số dư ví sẽ được cập nhật tương ứng.") },
            confirmButton = {
                TextButton(onClick = { transactionViewModel.deleteTransaction() }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { transactionViewModel.cancelDeleteTransaction() }) { Text("Hủy") }
            }
        )
    }

    if (transactionUiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { transactionViewModel.cancelDeleteCategory() },
            title = { Text("Xóa danh mục") },
            text = { Text("Xóa \"${transactionUiState.categoryToDelete}\" khỏi danh sách?") },
            confirmButton = {
                TextButton(onClick = { transactionViewModel.confirmDeleteCategory() }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { transactionViewModel.cancelDeleteCategory() }) { Text("Hủy") }
            }
        )
    }
}