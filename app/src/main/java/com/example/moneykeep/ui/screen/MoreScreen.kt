package com.example.moneykeep.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.moneykeep.R
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.viewmodel.HomeViewModel
import com.example.moneykeep.viewmodel.ThemeViewModel
import java.io.File
import java.io.FileOutputStream

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.moneykeep.viewmodel.TransactionViewModel
import com.example.moneykeep.viewmodel.WalletViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun MoreScreen(
    paddingValues: PaddingValues,
    navController: NavHostController,
    walletViewModel: WalletViewModel,
    homeViewModel: HomeViewModel,
    transactionViewModel: TransactionViewModel,
    themeViewModel: ThemeViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val homeUiState by homeViewModel.uiState.collectAsState()
    val walletUiState by walletViewModel.uiState.collectAsState()
    val transactions by transactionViewModel.transactions.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    var showInfoDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportData by remember { mutableStateOf<String?>(null) }

    // ── IMPORT ──────────────────────────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                importFromCsv(context, it) { txList, walletBalances ->
                    transactionViewModel.importTransactions(txList, walletBalances)
                    Toast.makeText(
                        context,
                        "Đã nhập thành công ${txList.size} giao dịch!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )

    // ── EXPORT ──────────────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            pendingExportData?.let { csv ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(csv.toByteArray())
                }
                Toast.makeText(context, "Xuất file thành công", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi xuất file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(colorScheme.background)
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(colorScheme.primary, colorScheme.secondary)
                    )
                )
                .padding(top = 40.dp, bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = homeUiState.userName.ifBlank { "Người dùng" },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Quản lý tài chính cá nhân",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // --- CONTENT ---
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .offset(y = (-30).dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    SettingsItem(
                        icon = when (isDarkMode) {
                            true -> Icons.Default.DarkMode
                            false -> Icons.Default.LightMode
                            else -> Icons.Default.SettingsBrightness
                        },
                        title = "Giao diện",
                        subtitle = when (isDarkMode) {
                            true -> "Tối"
                            false -> "Sáng"
                            else -> "Theo hệ thống"
                        },
                        iconColor = colorScheme.primary,
                        iconBg = colorScheme.primary.copy(alpha = 0.1f),
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colorScheme.outlineVariant)

                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Hướng dẫn sử dụng",
                        iconColor = colorScheme.primary,
                        iconBg = colorScheme.primary.copy(alpha = 0.1f),
                        onClick = { openPdfFromAssets(context, "hdsd.pdf") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colorScheme.outlineVariant)

                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        title = "Xuất dữ liệu",
                        subtitle = "Xuất lịch sử giao dịch ra CSV",
                        iconColor = Color(0xFF4CAF50),
                        iconBg = Color(0xFFE8F5E9),
                        onClick = { showExportDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colorScheme.outlineVariant)

                    SettingsItem(
                        icon = Icons.Default.FileUpload,
                        title = "Nhập dữ liệu",
                        subtitle = "Nhập giao dịch từ file CSV",
                        iconColor = Color(0xFF2196F3),
                        iconBg = Color(0xFFE3F2FD),
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "text/comma-separated-values",
                                    "text/csv",
                                    "application/octet-stream"
                                )
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colorScheme.outlineVariant)

                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Thông tin ứng dụng",
                        iconColor = Color(0xFF78909C),
                        iconBg = Color(0xFFECEFF1),
                        onClick = { showInfoDialog = true }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Made with ❤️ by MoneyKeep Team\nPhiên bản $appVersion",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }

    // ── DIALOGS ─────────────────────────────────────────────────────────────

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Chọn giao diện") },
            text = {
                Column {
                    ThemeOptionItem("Sáng", isDarkMode == false) {
                        themeViewModel.toggleDarkMode(false); showThemeDialog = false
                    }
                    ThemeOptionItem("Tối", isDarkMode == true) {
                        themeViewModel.toggleDarkMode(true); showThemeDialog = false
                    }
                    ThemeOptionItem("Theo hệ thống", isDarkMode == null) {
                        themeViewModel.toggleDarkMode(null); showThemeDialog = false
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Đóng") }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thông Tin Ứng Dụng", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "💰 MoneyKeep - Ứng dụng quản lý tài chính cá nhân hiệu quả, an toàn, lưu trữ dữ liệu hoàn toàn bảo mật ngay trên thiết bị của bạn.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Phiên bản hiện tại: $appVersion", fontWeight = FontWeight.Medium)
                    Text("• Nhà phát triển: MoneyKeep Team")
                    Text("• Chức năng chính: Ghi chép thu chi, quản lý các tài khoản ví, đặt hạn mức cảnh báo chi tiêu và thống kê báo cáo.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Đóng", fontWeight = FontWeight.Bold, color = colorScheme.primary)
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Chọn ví cần xuất dữ liệu", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    walletUiState.wallets.forEach { wallet ->
                        TextButton(
                            onClick = {
                                val csvContent = buildCsvContent(wallet.name, transactions, wallet.balance)
                                if (csvContent == null) {
                                    Toast.makeText(context, "Không có giao dịch nào để xuất!", Toast.LENGTH_SHORT).show()
                                } else {
                                    pendingExportData = csvContent
                                    exportLauncher.launch("MoneyKeep_${wallet.name}_${System.currentTimeMillis()}.csv")
                                }
                                showExportDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(wallet.name, color = colorScheme.onSurface)
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── HELPER FUNCTIONS ────────────────────────────────────────────────────────

/**
 * Xuất CSV với số dư tính lũy kế sau từng giao dịch.
 * Dòng cuối cùng sẽ có "Số dư còn lại" = số dư thực tế của ví hiện tại.
 */
fun buildCsvContent(
    walletName: String,
    allTransactions: List<Transaction>,
    walletBalance: Double
): String? {
    val transactions = allTransactions
        .filter { it.walletName == walletName }
        .sortedBy { it.date } // sắp xếp từ cũ → mới

    if (transactions.isEmpty()) return null

    val csvHeader = "Ngày giao dịch,Loại,Danh mục,Số tiền,Ghi chú,Ví,Số dư còn lại\n"
    val csvContent = StringBuilder(csvHeader)

    // Tính số dư trước giao dịch đầu tiên
    val totalTx = transactions.sumOf { it.amount }
    var runningBalance = walletBalance - totalTx

    transactions.forEach { tx ->
        runningBalance += tx.amount // cộng dần từng giao dịch
        val type = if (tx.amount >= 0) "Thu" else "Chi"
        val safeCategory = tx.category.replace(",", ";")
        val safeNote = tx.description.replace(",", ";").replace("\n", " ").replace("\r", " ")
        val safeWallet = tx.walletName.replace(",", ";")
        csvContent.append("${tx.date},$type,$safeCategory,${tx.amount},$safeNote,$safeWallet,$runningBalance\n")
    }

    return csvContent.toString()
}

/**
 * Đọc CSV và trả về:
 * - danh sách giao dịch
 * - map ví → số dư thực tế (lấy từ dòng cuối cùng của mỗi ví)
 */
fun importFromCsv(
    context: Context,
    uri: Uri,
    onResult: (List<Transaction>, Map<String, Double>) -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val transactions = mutableListOf<Transaction>()
        val walletBalances = mutableMapOf<String, Double>() // ví → số dư dòng cuối

        reader.readLine() // bỏ qua header

        var line: String? = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                val parts = line.split(",")
                if (parts.size >= 6) {
                    val date       = parts[0].trim()
                    val category   = parts[2].trim().replace(";", ",")
                    val amount     = parts[3].trim().toDoubleOrNull() ?: 0.0
                    val note       = parts[4].trim().replace(";", ",")
                    val walletName = parts[5].trim().replace(";", ",")
                    // Cột index 6 = "Số dư còn lại" sau giao dịch này
                    val balance    = parts.getOrNull(6)?.trim()?.toDoubleOrNull()

                    transactions.add(
                        Transaction(
                            date        = date,
                            amount      = amount,
                            category    = category,
                            description = note,
                            walletName  = walletName
                        )
                    )

                    // Ghi đè mỗi lần → cuối vòng lặp sẽ giữ lại số dư của dòng cuối cùng
                    if (balance != null) {
                        walletBalances[walletName] = balance
                    }
                }
            }
            line = reader.readLine()
        }
        reader.close()
        onResult(transactions, walletBalances)
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi khi nhập file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ThemeOptionItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(18.dp)
        )
    }
}

fun openPdfFromAssets(context: Context, fileName: String) {
    try {
        val cacheFile = File(context.cacheDir, fileName)
        context.assets.open(fileName).use { inputStream ->
            FileOutputStream(cacheFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val pdfUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        context.startActivity(Intent.createChooser(intent, "Mở file hướng dẫn bằng:"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Thiết bị không có ứng dụng đọc PDF!", Toast.LENGTH_LONG).show()
    }
}