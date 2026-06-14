package com.example.moneykeep.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Insert
    suspend fun insert(wallet: Wallet)

    @Update
    suspend fun update(wallet: Wallet)

    @androidx.room.Delete
    suspend fun delete(wallet: Wallet)
    @Query("UPDATE wallets SET balance = :balance WHERE name = :name")
    suspend fun setBalance(name: String, balance: Double)

    @Query("SELECT * FROM wallets ORDER BY id ASC")
    fun getAllWallets(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Int): Wallet?

    @Query("SELECT * FROM wallets WHERE name = :name")
    suspend fun getWalletByName(name: String): Wallet?

    @Query("UPDATE wallets SET balance = balance + :amount WHERE name = :name")
    suspend fun adjustBalance(name: String, amount: Double)
}
