package com.example.moneykeep.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Transaction::class, Wallet::class, Category::class, UserSetting::class],
    version = 8 // Tăng lên 8 để Room thực hiện cập nhật lại cấu trúc bảng mới
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userSettingDao(): UserSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // Tự động xóa dữ liệu cũ và tạo bảng mới khi có thay đổi cấu trúc
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
