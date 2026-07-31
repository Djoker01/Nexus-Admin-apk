package com.nexus.admin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nexus.admin.data.dao.*
import com.nexus.admin.data.entity.*

@Database(
    entities = [
        Product::class, Sale::class, CashMovement::class, Expense::class,
        Client::class, Receivable::class, Shrinkage::class, Restock::class,
        Supplier::class, Quote::class, AppNotification::class,
        User::class, Business::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun clientDao(): ClientDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun shrinkageDao(): ShrinkageDao
    abstract fun restockDao(): RestockDao
    abstract fun supplierDao(): SupplierDao
    abstract fun quoteDao(): QuoteDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userDao(): UserDao
    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexus_admin_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
