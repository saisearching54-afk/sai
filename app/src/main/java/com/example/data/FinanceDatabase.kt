package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM bank_accounts")
    fun getAllAccountsFlow(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts")
    suspend fun getAllAccounts(): List<BankAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<BankAccount>)

    @Update
    suspend fun updateAccount(account: BankAccount)

    @Delete
    suspend fun deleteAccount(account: BankAccount)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountSource = :accountSource ORDER BY timestamp DESC")
    fun getTransactionsForAccountFlow(accountSource: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE accountSource = :accountSource")
    suspend fun deleteTransactionsForAccount(accountSource: String)

    @Query("SELECT * FROM category_rules")
    fun getAllRulesFlow(): Flow<List<CategoryRule>>

    @Query("SELECT * FROM category_rules")
    suspend fun getAllRules(): List<CategoryRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRule)

    @Delete
    suspend fun deleteRule(rule: CategoryRule)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAllAccounts()
}

@Database(entities = [BankAccount::class, Transaction::class, CategoryRule::class], version = 1, exportSchema = false)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "fintrack_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class FinanceRepository(private val financeDao: FinanceDao) {
    val allAccounts: Flow<List<BankAccount>> = financeDao.getAllAccountsFlow()
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactionsFlow()
    val allRules: Flow<List<CategoryRule>> = financeDao.getAllRulesFlow()

    suspend fun insertAccount(account: BankAccount) = financeDao.insertAccount(account)
    suspend fun insertAccounts(accounts: List<BankAccount>) = financeDao.insertAccounts(accounts)
    suspend fun updateAccount(account: BankAccount) = financeDao.updateAccount(account)
    suspend fun deleteAccount(account: BankAccount) = financeDao.deleteAccount(account)

    suspend fun insertTransaction(transaction: Transaction) = financeDao.insertTransaction(transaction)
    suspend fun insertTransactions(transactions: List<Transaction>) = financeDao.insertTransactions(transactions)
    suspend fun updateTransaction(transaction: Transaction) = financeDao.updateTransaction(transaction)
    suspend fun deleteTransactionsForAccount(accountSource: String) = financeDao.deleteTransactionsForAccount(accountSource)

    suspend fun insertRule(rule: CategoryRule) = financeDao.insertRule(rule)
    suspend fun deleteRule(rule: CategoryRule) = financeDao.deleteRule(rule)
    suspend fun getAllRules(): List<CategoryRule> = financeDao.getAllRules()

    suspend fun clearAllData() {
        financeDao.deleteAllTransactions()
        financeDao.deleteAllAccounts()
    }
}
