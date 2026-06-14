package com.example.moneykeep.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction)

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)
    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date AND amount = :amount AND walletName = :walletName AND category = :category")
    suspend fun countDuplicate(date: String, amount: Double, walletName: String, category: String): Int
    @Delete
    suspend fun delete(transaction: Transaction)

    // Lấy tất cả giao dịch
    @Query("""
        SELECT * FROM transactions
        ORDER BY id DESC
    """)
    fun getAllTransactions(): Flow<List<Transaction>>

    // Lấy các category đã dùng
    @Query("""
        SELECT DISTINCT category
        FROM transactions
    """)
    fun getAllUsedCategories(): Flow<List<String>>

    // Lọc giao dịch theo category
    @Query("""
        SELECT * FROM transactions
        WHERE category = :category
        ORDER BY id DESC
    """)
    fun getTransactionsByCategory(
        category: String
    ): Flow<List<Transaction>>


    @Query("""
        SELECT * FROM transactions
        WHERE walletName = :walletName
        ORDER BY id DESC
    """)
    fun getTransactionsByWallet(walletName: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE walletName = :name")
    fun getWalletBalance(name: String): Flow<Double?>
    @Query("UPDATE transactions SET walletName = :newWalletName WHERE walletName = :oldWalletName")
    suspend fun updateWalletName(oldWalletName: String, newWalletName: String)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE category = :categoryName AND amount < 0
    """)
    suspend fun getTotalExpenseByCategory(categoryName: String): Double?
}
