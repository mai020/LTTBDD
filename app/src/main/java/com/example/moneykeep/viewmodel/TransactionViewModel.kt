package com.example.moneykeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class TransactionUiState(
    val amount: String = "",
    val note: String = "",
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedWallet: String = "",
    val selectedCategory: String = "",
    val selectedDate: String = "",
    val categoryOptions: List<String> = emptyList(),
    val selectedFilterCategory: String = "All",
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val categoryToDelete: String? = null,
    val editingTransactionId: Int? = null,
    val originalAmount: Double = 0.0,
    val originalWallet: String = "",
    val showDeleteTransactionDialog: Boolean = false,
    val overBudgetCategoryName: String? = null
)

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val walletViewModel: WalletViewModel
) : ViewModel() {

    private val defaultCategories = listOf(
        "Ăn Uống", "Di Chuyển", "Mua Sắm", "Hóa Đơn", "Giải Trí",
        "Sức Khỏe", "Giáo Dục", "Lương", "Khác"
    )

    private val _uiState = MutableStateFlow(
        TransactionUiState(
            categoryOptions = defaultCategories,
            selectedCategory = defaultCategories.first()
        )
    )

    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions = uiState
        .flatMapLatest { state ->
            if (state.selectedFilterCategory == "All") {
                repository.allTransactions
            } else {
                repository.getTransactionsByCategory(state.selectedFilterCategory)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.allUsedCategories.collect { dbCategories ->
                _uiState.update { state ->
                    val usedCategoryNames = dbCategories.map { it.name }
                    val allCategories = (defaultCategories + usedCategoryNames).distinct()
                    state.copy(categoryOptions = allCategories)
                }
            }
        }
    }

    fun selectFilterCategory(category: String) {
        _uiState.update { it.copy(selectedFilterCategory = category) }
    }

    fun addNewCategory(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        if (defaultCategories.contains(trimmedName)) {
            onCategoryChange(trimmedName)
            return
        }
        viewModelScope.launch {
            try {
                repository.insertCategory(
                    com.example.moneykeep.data.local.Category(name = trimmedName)
                )
                onCategoryChange(trimmedName)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi thêm danh mục: ${e.message}") }
            }
        }
    }

    fun requestDeleteCategory(categoryName: String) {
        if (defaultCategories.contains(categoryName)) {
            _uiState.update { it.copy(errorMessage = "Không thể xóa danh mục mặc định") }
            return
        }
        _uiState.update { it.copy(showDeleteConfirmDialog = true, categoryToDelete = categoryName) }
    }

    fun confirmDeleteCategory() {
        val categoryName = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            try {
                repository.deleteCategoryByName(categoryName)
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        categoryToDelete = null,
                        selectedCategory = if (it.selectedCategory == categoryName) "Ăn Uống" else it.selectedCategory
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        categoryToDelete = null,
                        errorMessage = "Lỗi khi xóa danh mục: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelDeleteCategory() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false, categoryToDelete = null) }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onWalletChange(wallet: String) {
        _uiState.update { it.copy(selectedWallet = wallet) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onDateChange(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun prepareEditTransaction(transaction: Transaction) {
        _uiState.update {
            it.copy(
                editingTransactionId = transaction.id,
                amount = kotlin.math.abs(transaction.amount).toInt().toString(),
                note = transaction.description,
                selectedType = if (transaction.amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                selectedWallet = transaction.walletName,
                selectedCategory = transaction.category,
                selectedDate = transaction.date,
                originalAmount = transaction.amount,
                originalWallet = transaction.walletName,
                errorMessage = null
            )
        }
    }

    fun prepareAddTransaction() {
        resetForm()
        _uiState.update {
            it.copy(
                editingTransactionId = null,
                errorMessage = null
            )
        }
    }

    fun saveTransaction() {
        val state = _uiState.value

        if (state.selectedWallet.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn ví") }
            return
        }

        val amountDouble = state.amount.filter { it.isDigit() }.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập số tiền hợp lệ") }
            return
        }

        if (state.selectedDate.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn ngày giao dịch") }
            return
        }

        val signedAmount = if (state.selectedType == TransactionType.EXPENSE) -amountDouble else amountDouble

        val transaction = Transaction(
            id = state.editingTransactionId ?: 0,
            category = state.selectedCategory,
            amount = signedAmount,
            description = state.note,
            date = state.selectedDate,
            walletName = state.selectedWallet
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val wallets = walletViewModel.uiState.value.wallets
                if (state.editingTransactionId == null) {
                    // ── THÊM MỚI ──
                    if (signedAmount < 0) {
                        val wallet = wallets.find { it.name == state.selectedWallet }
                        val currentBalance = wallet?.balance ?: 0.0
                        if (currentBalance + signedAmount < 0) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = "Ví \"${state.selectedWallet}\" không đủ số dư. Vui lòng nạp thêm tiền hoặc giảm số tiền chi."
                                )
                            }
                            return@launch
                        }
                    }
                    repository.insertTransaction(transaction)
                    walletViewModel.adjustWalletBalance(state.selectedWallet, signedAmount)

                } else {
                    // ── SỬA GIAO DỊCH ──
                    if (state.selectedWallet == state.originalWallet) {
                        val wallet = wallets.find { it.name == state.selectedWallet }
                        val currentBalance = wallet?.balance ?: 0.0
                        // Hoàn tác cũ + Áp dụng mới
                        if (currentBalance - state.originalAmount + signedAmount < 0) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = "Thay đổi này làm ví \"${state.selectedWallet}\" bị âm số dư. Vui lòng kiểm tra lại."
                                )
                            }
                            return@launch
                        }
                    } else {
                        // Trường hợp đổi ví
                        val oldWallet = wallets.find { it.name == state.originalWallet }
                        val newWallet = wallets.find { it.name == state.selectedWallet }
                        
                        // Kiểm tra ví cũ sau khi hoàn tác (đặc biệt nếu giao dịch cũ là Thu nhập)
                        if ((oldWallet?.balance ?: 0.0) - state.originalAmount < 0) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = "Không thể chuyển vì ví cũ \"${state.originalWallet}\" sẽ bị âm số dư."
                                )
                            }
                            return@launch
                        }
                        
                        // Kiểm tra ví mới sau khi áp dụng giao dịch mới (đặc biệt nếu là Chi tiêu)
                        if ((newWallet?.balance ?: 0.0) + signedAmount < 0) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    errorMessage = "Ví mới \"${state.selectedWallet}\" không đủ số dư."
                                )
                            }
                            return@launch
                        }
                    }
                    
                    repository.updateTransaction(transaction)
                    walletViewModel.adjustWalletBalance(state.originalWallet, -state.originalAmount)
                    walletViewModel.adjustWalletBalance(state.selectedWallet, signedAmount)
                }

                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Lỗi khi lưu giao dịch: ${e.message}") }
            }
        }
    }

    fun deleteTransaction() {
        val state = _uiState.value
        val transactionId = state.editingTransactionId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Kiểm tra nếu xóa giao dịch (hoàn tác) làm ví bị âm (ví dụ xóa khoản thu lớn)
                val wallet = walletViewModel.uiState.value.wallets.find { it.name == state.originalWallet }
                if ((wallet?.balance ?: 0.0) - state.originalAmount < 0) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Không thể xóa vì ví \"${state.originalWallet}\" sẽ bị âm số dư."
                        )
                    }
                    return@launch
                }

                val transactionToDelete = Transaction(
                    id = transactionId,
                    amount = state.originalAmount,
                    walletName = state.originalWallet,
                    category = state.selectedCategory,
                    description = state.note,
                    date = state.selectedDate
                )
                repository.deleteTransaction(transactionToDelete)
                walletViewModel.adjustWalletBalance(state.originalWallet, -state.originalAmount)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isDeleteSuccess = true,
                        showDeleteTransactionDialog = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Lỗi khi xóa: ${e.message}") }
            }
        }
    }

    fun deleteTransactionFromHistory(transaction: Transaction) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val wallet = walletViewModel.uiState.value.wallets.find { it.name == transaction.walletName }
                if ((wallet?.balance ?: 0.0) - transaction.amount < 0) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Không thể xóa vì ví \"${transaction.walletName}\" sẽ bị âm số dư."
                        )
                    }
                    return@launch
                }

                repository.deleteTransaction(transaction)
                // Nếu xóa khoản chi thì hoàn tiền vào ví; nếu xóa khoản thu thì trừ lại khoản thu khỏi ví.
                walletViewModel.adjustWalletBalance(transaction.walletName, -transaction.amount)
                _uiState.update { it.copy(isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Lỗi khi xóa giao dịch: ${e.message}") }
            }
        }
    }

    fun requestDeleteTransaction() {
        _uiState.update { it.copy(showDeleteTransactionDialog = true) }
    }

    fun cancelDeleteTransaction() {
        _uiState.update { it.copy(showDeleteTransactionDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearOverBudgetAlert() {
        _uiState.update { it.copy(overBudgetCategoryName = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update {
            it.copy(
                isSaveSuccess = false,
                isDeleteSuccess = false,
                editingTransactionId = null,
                originalAmount = 0.0,
                originalWallet = "",
                amount = "",
                note = "",
                selectedType = TransactionType.EXPENSE,
                selectedCategory = defaultCategories.first(),
                selectedDate = "",
                errorMessage = null
            )
        }
    }

    private fun resetForm() {
        _uiState.update {
            it.copy(
                amount = "",
                note = "",
                selectedType = TransactionType.EXPENSE,
                selectedCategory = defaultCategories.first(),
                selectedDate = ""
            )
        }
    }

    fun importTransactions(
        transactions: List<Transaction>,
        walletBalances: Map<String, Double> = emptyMap()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.insertTransactionsNoDuplicate(transactions)

                transactions.groupBy { it.walletName }.keys.forEach { walletName ->
                    val wallet = walletViewModel.uiState.value.wallets.find { it.name == walletName }
                    // Lấy số dư thực tế từ CSV (dòng cuối của ví đó)
                    val correctBalance = walletBalances[walletName] ?: 0.0

                    if (wallet != null) {
                        walletViewModel.setWalletBalance(walletName, correctBalance)
                    } else {
                        walletViewModel.createWalletFromImport(walletName, correctBalance)
                    }
                }
                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Lỗi khi nhập dữ liệu: ${e.message}") }
            }
        }
    }

    class Factory(
        private val repository: TransactionRepository,
        private val walletViewModel: WalletViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                return TransactionViewModel(repository, walletViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
