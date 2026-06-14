package com.example.moneykeep.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class ReportFilter { DAY, WEEK, MONTH, CUSTOM }

data class CategoryData(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)
data class BarData(
    val label: String,
    val value: Double
)

data class ReportUiState(
    val selectedFilter: ReportFilter = ReportFilter.WEEK,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val incomeTransactionCount: Int = 0,
    val expenseTransactionCount: Int = 0,
    val averagePerDay: Double = 0.0,
    val averageIncomePerDay: Double = 0.0,
    val expenseBarData: List<BarData> = emptyList(),
    val incomeBarData: List<BarData> = emptyList(),
    val expenseCategoryData: List<CategoryData> = emptyList(),
    val incomeCategoryData: List<CategoryData> = emptyList(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ReportViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ReportFilter.WEEK)
    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val categoryColors = mapOf(
        "Ăn Uống"    to Color(0xFF66BB6A),
        "Di Chuyển"  to Color(0xFF42A5F5),
        "Mua Sắm"    to Color(0xFFFFA726),
        "Hóa Đơn"    to Color(0xFFEF5350),
        "Giải Trí"   to Color(0xFFAB47BC),
        "Sức Khỏe"   to Color(0xFF26C6DA),
        "Giáo Dục"   to Color(0xFFFFCA28),
        "Lương"      to Color(0xFF26A69A),
        "Khác"       to Color(0xFF78909C)
    )

    private val dateFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(repository.allTransactions, _filter, _dateRange) { allTransactions, filter, range ->
                Triple(allTransactions, filter, range)
            }
            .collectLatest { (allTransactions, filter, range) ->
                _uiState.update { it.copy(isLoading = true) }
                val startTime = System.currentTimeMillis()
                try {
                    // Xử lý dữ liệu nặng ở background thread để tránh lag UI
                    val newState = withContext(Dispatchers.Default) {
                        processData(allTransactions, filter, range)
                    }

                    // Duy trì trạng thái Loading tối thiểu 600ms để tránh hiện tượng nháy
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val minLoadingDuration = 600L
                    if (elapsedTime < minLoadingDuration) {
                        delay(minLoadingDuration - elapsedTime)
                    }

                    _uiState.value = newState.copy(isLoading = false, errorMessage = null)
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Lỗi xử lý báo cáo: ${e.message}") }
                }
            }
        }
    }

    fun onFilterChange(filter: ReportFilter) {
        if (_filter.value == filter) return
        _uiState.update { it.copy(selectedFilter = filter, isLoading = true) }
        _filter.value = filter
    }

    fun onCustomDateSelected(start: Long, end: Long) {
        _uiState.update { it.copy(isLoading = true) }
        val calStart = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        _dateRange.value = calStart.timeInMillis to calEnd.timeInMillis
        _filter.value = ReportFilter.CUSTOM
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getCategoryColor(name: String): Color {
        val normalized = name.trim().lowercase()
        val defaultColor = categoryColors.entries.find { it.key.lowercase() == normalized }?.value
        if (defaultColor != null) return defaultColor

        val userPalette = listOf(
            Color(0xFF8D6E63), Color(0xFF78909C), Color(0xFF9E9E9E),
            Color(0xFFA1887F), Color(0xFF90A4AE), Color(0xFFB0BEC5),
            Color(0xFF80CBC4), Color(0xFFC5E1A5), Color(0xFFD7CCC8),
            Color(0xFFBCAAA4), Color(0xFFAED581), Color(0xFFFFB74D),
            Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFF9575CD)
        )
        val hash = name.hashCode()
        val index = (hash and 0x7FFFFFFF) % userPalette.size
        return userPalette[index]
    }

    private fun processData(allTransactions: List<Transaction>, filter: ReportFilter, range: Pair<Long?, Long?>): ReportUiState {
        val now = Calendar.getInstance()
        val parsedTransactions = allTransactions.mapNotNull { transaction ->
            try {
                dateFormat.parse(transaction.date)?.let { date ->
                    val cal = Calendar.getInstance().apply { time = date }
                    transaction to cal
                }
            } catch (e: Exception) { null }
        }

        val filtered = when (filter) {
            ReportFilter.DAY -> {
                parsedTransactions.filter {
                    it.second.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) &&
                            it.second.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
            }
            ReportFilter.WEEK -> {
                parsedTransactions.filter {
                    it.second.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                            it.second.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
            }
            ReportFilter.MONTH -> {
                parsedTransactions.filter {
                    it.second.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
            }
            ReportFilter.CUSTOM -> {
                val start = range.first ?: 0L
                val end = range.second ?: Long.MAX_VALUE
                parsedTransactions.filter { it.second.timeInMillis in start..end }
            }
        }

        val incomeList = filtered.filter { it.first.amount > 0 }
        val expenseList = filtered.filter { it.first.amount < 0 }

        val totalInc = incomeList.sumOf { it.first.amount }
        val totalExp = expenseList.sumOf { -it.first.amount }

        val incomeTxCount = incomeList.size
        val expenseTxCount = expenseList.size

        val daysInPeriod = when (filter) {
            ReportFilter.DAY -> 7.0
            ReportFilter.WEEK -> now.getActualMaximum(Calendar.DAY_OF_MONTH).toDouble()
            ReportFilter.MONTH -> now.getActualMaximum(Calendar.DAY_OF_YEAR).toDouble()
            ReportFilter.CUSTOM -> {
                val diff = (range.second ?: 0L) - (range.first ?: 0L)
                if (diff <= 0) 1.0 else (diff / (1000 * 60 * 60 * 24).toDouble()).roundToInt().coerceAtLeast(1).toDouble()
            }
        }

        return ReportUiState(
            selectedFilter = filter,
            totalIncome = totalInc,
            totalExpense = totalExp,
            incomeTransactionCount = incomeTxCount,
            expenseTransactionCount = expenseTxCount,
            averagePerDay = totalExp / daysInPeriod,
            averageIncomePerDay = totalInc / daysInPeriod,
            incomeBarData = buildBarData(incomeList, filter),
            expenseBarData = buildBarData(expenseList, filter),
            incomeCategoryData = buildCategoryData(incomeList, totalInc),
            expenseCategoryData = buildCategoryData(expenseList, totalExp),
            startDate = range.first,
            endDate = range.second
        )
    }

    private fun buildCategoryData(list: List<Pair<Transaction, Calendar>>, total: Double): List<CategoryData> {
        if (total <= 0) return emptyList()
        val groups = list.groupBy { it.first.category }
            .map { (name, items) ->
                val amt = items.sumOf { if (it.first.amount < 0) -it.first.amount else it.first.amount }
                name to amt
            }.sortedByDescending { it.second }

        val roundedPercentages = groups.map { ((it.second / total) * 100).roundToInt() }.toMutableList()
        val sum = roundedPercentages.sum()
        if (sum != 100 && roundedPercentages.isNotEmpty()) {
            roundedPercentages[0] += (100 - sum)
        }

        return groups.mapIndexed { index, (name, amt) ->
            CategoryData(name, amt, roundedPercentages[index].toFloat(), getCategoryColor(name))
        }
    }

    private fun buildBarData(list: List<Pair<Transaction, Calendar>>, filter: ReportFilter): List<BarData> {
        return when (filter) {
            ReportFilter.DAY -> {
                val dayMap = list.groupBy { it.second.get(Calendar.DAY_OF_WEEK) }
                    .mapValues { it.value.sumOf { tx -> if (tx.first.amount < 0) -tx.first.amount else tx.first.amount } }

                listOf(2, 3, 4, 5, 6, 7, 1).map { day ->
                    val label = if (day == 1) "CN" else "Thứ $day"
                    BarData(label, dayMap[day] ?: 0.0)
                }
            }
            ReportFilter.WEEK -> {
                val weekMap = list.groupBy { it.second.get(Calendar.WEEK_OF_MONTH) }
                    .mapValues { it.value.sumOf { tx -> if (tx.first.amount < 0) -tx.first.amount else tx.first.amount } }

                (1..5).map { week ->
                    BarData("Tuần $week", weekMap[week] ?: 0.0)
                }
            }
            ReportFilter.MONTH -> {
                val monthMap = list.groupBy { it.second.get(Calendar.MONTH) }
                    .mapValues { it.value.sumOf { tx -> if (tx.first.amount < 0) -tx.first.amount else tx.first.amount } }

                (0..11).map { month ->
                    BarData("T${month + 1}", monthMap[month] ?: 0.0)
                }
            }
            else -> {
                list.groupBy { it.first.date }
                    .map { (label, items) ->
                        BarData(label, items.sumOf { if (it.first.amount < 0) -it.first.amount else it.first.amount })
                    }.sortedBy { it.label }
            }
        }
    }

    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportViewModel(repository) as T
        }
    }
}
