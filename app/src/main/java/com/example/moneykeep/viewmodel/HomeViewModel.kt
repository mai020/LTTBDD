package com.example.moneykeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneykeep.data.local.AppDatabase
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.data.local.UserSetting
import com.example.moneykeep.data.repository.TransactionRepository
import com.example.moneykeep.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class HomeFilter(
    val dateFrom: String = "",
    val dateTo: String = "",
    val selectedWallets: Set<String> = emptySet(),
    val selectedType: String = "Tất cả",
    val selectedCategories: Set<String> = emptySet()
)

data class HomeUiState(
    val userName: String = "",
    val currentBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filter: HomeFilter = HomeFilter()
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class HomeViewModel(
    private val repository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val KEY_USER_NAME = "user_name"
    }

    init {
        observeData()
        observeUserName()
    }

    private fun observeUserName() {
        viewModelScope.launch {
            database.userSettingDao().getSettingFlow(KEY_USER_NAME).collect { savedName ->
                _uiState.update {
                    it.copy(userName = savedName ?: "")
                }
            }
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            database.userSettingDao().saveSetting(UserSetting(KEY_USER_NAME, name))
        }
    }

    fun updateFilter(filter: HomeFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    private fun parseDate(str: String): Calendar? = try {
        val p = str.split("/")
        Calendar.getInstance().apply {
            set(Calendar.YEAR, p[2].toInt())
            set(Calendar.MONTH, p[1].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, p[0].toInt())
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    } catch (e: Exception) { null }

    private fun observeData() {
        // Theo dõi cả giao dịch và ví để cập nhật số dư và lọc dữ liệu
        viewModelScope.launch {
            combine(
                repository.allTransactions,
                _uiState.map { it.filter }.distinctUntilChanged()
            ) { transactions, filter ->
                val filtered = transactions.filter { tx ->
                    val txCal = parseDate(tx.date)
                    val fromOk = filter.dateFrom.isEmpty() || (txCal != null && parseDate(filter.dateFrom)?.let { !txCal.before(it) } ?: true)
                    val toOk = filter.dateTo.isEmpty() || (txCal != null && parseDate(filter.dateTo)?.let { !txCal.after(it) } ?: true)
                    val walletOk = filter.selectedWallets.isEmpty() || tx.walletName in filter.selectedWallets
                    val typeOk = when (filter.selectedType) {
                        "Thu" -> tx.amount >= 0
                        "Chi" -> tx.amount < 0
                        else -> true
                    }
                    val categoryOk = filter.selectedCategories.isEmpty() || tx.category in filter.selectedCategories
                    fromOk && toOk && walletOk && typeOk && categoryOk
                }

                val totalIncome = transactions
                    .filter { it.amount > 0 }
                    .sumOf { it.amount }

                val totalExpense = transactions
                    .filter { it.amount < 0 }
                    .sumOf { it.amount }

                Triple(filtered, totalIncome, totalExpense)
            }.collect { (filtered, income, expense) ->
                _uiState.update { state ->
                    state.copy(
                        totalIncome = income,
                        totalExpense = expense,
                        recentTransactions = filtered,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            walletRepository.allWallets.collect { wallets ->
                _uiState.update { state ->
                    state.copy(
                        currentBalance = wallets.sumOf { it.balance }
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    // ---------------------------------------------------------------------------
    // Factory
    // ---------------------------------------------------------------------------
    class Factory(
        private val repository: TransactionRepository,
        private val walletRepository: WalletRepository,
        private val database: AppDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository, walletRepository, database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
