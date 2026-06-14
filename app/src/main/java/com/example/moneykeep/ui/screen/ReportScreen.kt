package com.example.moneykeep.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneykeep.viewmodel.CategoryData
import com.example.moneykeep.viewmodel.ReportFilter
import com.example.moneykeep.viewmodel.ReportViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    paddingValues: PaddingValues,
    viewModel: ReportViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")) }

    var showDatePicker by remember { mutableStateOf(false) }
    var reportType by remember { mutableStateOf("Chi") }
    val isIncome = reportType == "Thu"

    val total = if (isIncome) uiState.totalIncome else uiState.totalExpense
    val bars = if (isIncome) uiState.incomeBarData else uiState.expenseBarData
    val categories = if (isIncome) uiState.incomeCategoryData else uiState.expenseCategoryData
    val dateRangeState = rememberDateRangePickerState()

    val incomeColor = Color(0xFF1565C0)
    val expenseColor = Color(0xFFC62828)
    val activeColor = if (isIncome) incomeColor else expenseColor

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .verticalScroll(rememberScrollState())
        ) {
            // ── HEADER ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Brush.verticalGradient(listOf(cs.primary, cs.secondary)))
                    .padding(top = 24.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Báo Cáo Tài Chính",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    // Tab Thu / Chi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Thu", "Chi").forEach { type ->
                            val sel = reportType == type
                            Button(
                                onClick = { reportType = type },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (sel) Color.White else Color.Transparent,
                                    contentColor = if (sel) (if (type == "Thu") incomeColor else expenseColor) else Color.White.copy(alpha = 0.8f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                            ) {
                                Icon(
                                    if (type == "Thu") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(type, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 4 METRIC CARDS ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = if (isIncome) "Tổng thu" else "Tổng chi",
                        value = fmt.format(abs(total)),
                        valueColor = activeColor
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Số giao dịch",
                        value = "${if (isIncome) uiState.incomeTransactionCount else uiState.expenseTransactionCount} GD",
                        valueColor = cs.onSurface
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "TB / ngày",
                        value = fmt.format(if (isIncome) uiState.averageIncomePerDay else uiState.averagePerDay),
                        valueColor = cs.onSurface
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Danh mục",
                        value = "${categories.size} loại",
                        valueColor = cs.onSurface
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── BIỂU ĐỒ CỘT ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Biểu đồ thống kê",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReportFilter.entries.forEach { filter ->
                                val lbl = when (filter) {
                                    ReportFilter.DAY -> "Ngày"
                                    ReportFilter.WEEK -> "Tuần"
                                    ReportFilter.MONTH -> "Tháng"
                                    ReportFilter.CUSTOM -> "Khác"
                                }
                                val sel = uiState.selectedFilter == filter
                                Surface(
                                    onClick = {
                                        if (filter == ReportFilter.CUSTOM) showDatePicker = true
                                        else viewModel.onFilterChange(filter)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (sel) activeColor else cs.surfaceVariant,
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            lbl,
                                            fontSize = 11.sp,
                                            color = if (sel) Color.White else cs.onSurfaceVariant,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (bars.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Không có dữ liệu", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val maxVal = bars.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
                            bars.forEach { data ->
                                val barH = ((data.value / maxVal) * 100).dp.coerceAtLeast(3.dp)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(barH)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (data.value > 0) activeColor else cs.surfaceVariant)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        data.label,
                                        fontSize = 9.sp,
                                        color = cs.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── PHÂN BỔ DONUT ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (isIncome) "Phân bổ thu nhập" else "Phân bổ chi tiêu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    if (categories.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Không có dữ liệu", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(110.dp)
                            ) {
                                var startAngle = -90f
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    categories.forEach { item ->
                                        val sweep = (item.percentage / 100f) * 360f
                                        drawArc(
                                            color = item.color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 28f, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${categories.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = cs.onSurface
                                    )
                                    Text("danh mục", fontSize = 9.sp, color = cs.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(item.color)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            item.name,
                                            fontSize = 12.sp,
                                            color = cs.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${item.percentage}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = item.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Overlay Loading
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = activeColor)
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
                onDismissRequest = { /* Bắt buộc nhấn nút đã hiểu */ },
                icon = { Icon(Icons.Default.Warning, null, tint = cs.error) },
                title = { Text("Thông báo lỗi") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) { 
                        Text("Đã hiểu", fontWeight = FontWeight.Bold) 
                    }
                }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val s = dateRangeState.selectedStartDateMillis
                        val e = dateRangeState.selectedEndDateMillis
                        if (s != null && e != null) viewModel.onCustomDateSelected(s, e)
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
            ) { DateRangePicker(state = dateRangeState) }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
