package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey val id: String,
    val name: String,
    val accountNo: String,
    val balance: Double,
    val isLinked: Boolean = false,
    val lastSyncTime: Long = 0L,
    val isHidden: Boolean = false
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String,
    val amount: Double,
    val timestamp: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "FOOD", "TRANSPORT", "SHOPPING", "BILLS", "SUBSCRIPTIONS", "INCOME", "TRANSFERS", "UNCATEGORIZED"
    val accountSource: String, // ID of the linked BankAccount
    val isUserCorrected: Boolean = false
)

@Entity(tableName = "category_rules")
data class CategoryRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantPattern: String,
    val category: String
)
