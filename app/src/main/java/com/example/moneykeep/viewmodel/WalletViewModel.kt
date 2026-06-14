package com.example.moneykeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.data.local.Wallet
import com.example.moneykeep.data.repository.TransactionRepository
import com.example.moneykeep.data.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_SPENDING_LIMIT = 0.0

private fun formatMoneyInput(value: String): String {
    val digits = value.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val normalized = digits.trimStart('0').ifEmpty { "0" }
    return normalized.reversed().chunked(3).joinToString(".").reversed()
}

private fun parseMoneyInput(value: String): Double {
    return value.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
}

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class WalletUiState(
    val wallets: List<Wallet> = emptyList(),
    val totalBalance: Double = 0.0,

    // Form tạo ví mới
    val walletName: String = "",
    val initialBalance: String = "",
    val spendingLimit: String = "",

    // Trạng thái
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val isMergeSuccess: Boolean = false,
    val errorMessage: String? = null,

    // Chế độ sửa/thêm
    val editingWallet: Wallet? = null,

    // Cảnh báo ví sắp hết tiền
    val lowBalanceWalletName: String? = null,
    val lowBalanceAmount: Double = 0.0,

    // Cảnh báo vượt ngân sách
    val overBudgetWalletName: String? = null
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class WalletViewModel(
    private val repository: WalletRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        observeWallets()
    }

    private fun observeWallets() {
        viewModelScope.launch {
            repository.allWallets.collect { wallets ->
                _uiState.update { state ->
                    val calculatedTotal = wallets.sumOf { it.balance } // bỏ maxOf
                    state.copy(
                        wallets = wallets,
                        totalBalance = calculatedTotal,
                        isLoading = false
                    )
                }
            }
        }
    }

    // --- Form field handlers ---

    fun onWalletNameChange(value: String) {
        _uiState.update { it.copy(walletName = value) }
    }

    fun onInitialBalanceChange(value: String) {
        _uiState.update { it.copy(initialBalance = formatMoneyInput(value)) }
    }

    fun onSpendingLimitChange(value: String) {
        _uiState.update { it.copy(spendingLimit = formatMoneyInput(value)) }
    }

    // --- Lưu ví (Thêm mới hoặc Cập nhật) ---

    fun prepareCreateWallet() {
        _uiState.update { 
            it.copy(
                editingWallet = null,
                walletName = "",
                initialBalance = "",
                spendingLimit = "",
                errorMessage = null
            ) 
        }
    }

    fun prepareEditWallet(wallet: Wallet) {
        _uiState.update {
            it.copy(
                editingWallet = wallet,
                walletName = wallet.name,
                initialBalance = formatMoneyInput(wallet.balance.toLong().toString()),
                spendingLimit = formatMoneyInput(wallet.spendingLimit.toLong().toString()),
                errorMessage = null
            )
        }
    }

    fun saveWallet() {
        val state = _uiState.value
        val name = state.walletName.trim()

        // 1. Không được để trống
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên ví") }
            return
        }

        // 2. Không bắt đầu bằng số hoặc ký tự đặc biệt
        if (!name.first().isLetter()) {
            _uiState.update { it.copy(errorMessage = "Tên ví phải bắt đầu bằng chữ cái") }
            return
        }

        // 3. Chỉ cho phép chữ, số, khoảng trắng (không cho ký tự đặc biệt)
        val validNameRegex = Regex("^[a-zA-ZÀ-ỹ0-9 ]+$")
        if (!validNameRegex.matches(name)) {
            _uiState.update { it.copy(errorMessage = "Tên ví không được chứa ký tự đặc biệt") }
            return
        }

        // 4. Độ dài tối đa 30 ký tự
        if (name.length > 30) {
            _uiState.update { it.copy(errorMessage = "Tên ví không được quá 30 ký tự") }
            return
        }

        val balance = parseMoneyInput(state.initialBalance)
        val spendingLimit = parseMoneyInput(state.spendingLimit)

        // 5. Mức cảnh báo không vượt số dư
        if (spendingLimit > 0 && spendingLimit > balance) {
            _uiState.update { it.copy(errorMessage = "Mức cảnh báo không được vượt quá số dư") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // 6. Không trùng tên (trừ khi đang sửa chính ví đó)
                val existing = repository.getWalletByName(name)
                if (existing != null && existing.id != state.editingWallet?.id) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Tên ví đã tồn tại, vui lòng chọn tên khác") }
                    return@launch
                }

                val editingWallet = state.editingWallet
                if (editingWallet == null) {
                    repository.insertWallet(Wallet(name = name, balance = maxOf(0.0, balance), spendingLimit = maxOf(0.0, spendingLimit)))
                } else {
                    val updatedWallet = editingWallet.copy(name = name, balance = maxOf(0.0, balance), spendingLimit = maxOf(0.0, spendingLimit))
                    repository.updateWallet(updatedWallet)
                    if (editingWallet.name != name) {
                        transactionRepository.updateTransactionsWalletName(editingWallet.name, name)
                    }
                }

                _uiState.update {
                    it.copy(walletName = "", initialBalance = "", spendingLimit = "", isSaving = false, isSaveSuccess = true, editingWallet = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Lỗi khi lưu ví: ${e.message}") }
            }
        }
    }

    fun createWalletFromImport(walletName: String, balance: Double) {
        viewModelScope.launch {
            try {
                val existing = repository.getWalletByName(walletName)
                if (existing != null) {
                    repository.setWalletBalance(walletName, balance)
                } else {
                    val newWallet = Wallet(
                        name = walletName,
                        balance = balance,
                        spendingLimit = 0.0
                    )
                    repository.insertWallet(newWallet)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi tạo ví từ import: ${e.message}") }
            }
        }
    }


    fun adjustWalletBalance(walletName: String, amount: Double) {
        viewModelScope.launch {
            try {
                val wallet = repository.getWalletByName(walletName)
                if (wallet != null) {
                    repository.adjustWalletBalance(walletName, amount)

                    // Cập nhật lại state cục bộ nếu cần thiết cho các thông báo
                    val newBalance = wallet.balance + amount
                    if (amount < 0 && wallet.spendingLimit > 0 && newBalance < wallet.spendingLimit) {
                        _uiState.update {
                            it.copy(
                                overBudgetWalletName = walletName
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi cập nhật số dư: ${e.message}") }
            }
        }
    }

    fun setWalletBalance(walletName: String, balance: Double) {
        viewModelScope.launch {
            try {
                repository.setWalletBalance(walletName, balance)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi đặt số dư ví: ${e.message}") }
            }
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            try {
                repository.deleteWallet(wallet)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi xóa ví: ${e.message}") }
            }
        }
    }

    fun clearOverBudgetAlert() {
        _uiState.update { it.copy(overBudgetWalletName = null) }
    }

    fun clearLowBalanceAlert() {
        _uiState.update { it.copy(lowBalanceWalletName = null, lowBalanceAmount = 0.0) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(isSaveSuccess = false, isMergeSuccess = false) }
    }

    fun mergeWallet(sourceWallet: Wallet, targetWallet: Wallet) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // 1. Chuyển tất cả giao dịch từ ví cũ sang ví mới
                transactionRepository.updateTransactionsWalletName(sourceWallet.name, targetWallet.name)

                // 2. Cập nhật số dư ví mới (cộng thêm số dư ví cũ)
                val updatedTargetWallet = targetWallet.copy(
                    balance = targetWallet.balance + sourceWallet.balance
                )
                repository.updateWallet(updatedTargetWallet)

                // 3. Xóa ví cũ
                repository.deleteWallet(sourceWallet)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isMergeSuccess = true,
                        editingWallet = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Lỗi khi gộp ví: ${e.message}")
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Factory
    // ---------------------------------------------------------------------------

    class Factory(
        private val repository: WalletRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
                return WalletViewModel(repository, transactionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
