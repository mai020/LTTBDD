package com.example.moneykeep.data.repository

import com.example.moneykeep.data.local.Category
import com.example.moneykeep.data.local.CategoryDao
import com.example.moneykeep.data.local.Transaction
import com.example.moneykeep.data.local.TransactionDao
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    // Lấy tất cả giao dịch
    val allTransactions: Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    // Lấy tất cả category
    val allUsedCategories: Flow<List<Category>> =
        categoryDao.getAllCategories()

    // Lọc theo category
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category)
    }

    // Lọc theo ví
    fun getTransactionsByWallet(walletName: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByWallet(walletName)
    }

    // --- TRANSACTION OPERATIONS ---

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction)
    }


    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun getTotalExpenseByCategory(categoryName: String): Double? {
        return transactionDao.getTotalExpenseByCategory(categoryName)
    }

    // --- WALLET OPERATIONS ---

    suspend fun updateTransactionsWalletName(oldWalletName: String, newWalletName: String) {
        transactionDao.updateWalletName(oldWalletName, newWalletName)
    }

    // --- CATEGORY OPERATIONS ---

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun deleteCategoryByName(name: String) {
        categoryDao.deleteByName(name)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun insertTransactionsNoDuplicate(transactions: List<Transaction>) {
        transactions.forEach { tx ->
            val count = transactionDao.countDuplicate(tx.date, tx.amount, tx.walletName, tx.category)
            if (count == 0) {
                transactionDao.insert(tx)
            }
        }
    }
}
