package com.example.moneykeep.data.repository

import com.example.moneykeep.data.local.Wallet
import com.example.moneykeep.data.local.WalletDao
import kotlinx.coroutines.flow.Flow

class WalletRepository(
    private val walletDao: WalletDao
) {

    val allWallets: Flow<List<Wallet>> =
        walletDao.getAllWallets()

    suspend fun insertWallet(wallet: Wallet) {
        walletDao.insert(wallet)
    }
    suspend fun setWalletBalance(name: String, balance: Double) {
        walletDao.setBalance(name, balance)
    }

    suspend fun updateWallet(wallet: Wallet) {
        walletDao.update(wallet)
    }

    suspend fun deleteWallet(wallet: Wallet) {
        walletDao.delete(wallet)
    }

    suspend fun getWalletByName(name: String): Wallet? {
        return walletDao.getWalletByName(name)
    }

    suspend fun adjustWalletBalance(name: String, amount: Double) {
        walletDao.adjustBalance(name, amount)
    }
}

