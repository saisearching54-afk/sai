package com.example.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Welcome : Screen()
    object PhoneInput : Screen()
    data class OtpVerification(val phone: String) : Screen()
    object LinkAccountPrompt : Screen()
    object SelectProvider : Screen()
    data class ConsentAndPermissions(val bankId: String) : Screen()
    data class LinkingProgress(val bankId: String) : Screen()
    object Dashboard : Screen()
    data class TransactionDetail(val transactionId: Int) : Screen()
    data class CategorySelector(val transactionId: Int) : Screen()
    object PreferencesHome : Screen()
    object NotificationSettings : Screen()
    object LinkedAccountsSettings : Screen()
    object ManageAccounts : Screen()
    object LinkAccounts : Screen()
    object SpendCategories : Screen()
    object CreditCategories : Screen()
    object TagsManagement : Screen()
}

class CustomCategory(
    val id: String,
    val name: String,
    val color: Color,
    val icon: String = "💰"
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    // WhatsApp Chatbot State (US-001)
    val isWhatsAppChatbotActive = MutableStateFlow(false)
    val isWhatsAppConnected = MutableStateFlow(false)
    val isWhatsAppBotTyping = MutableStateFlow(false)
    val whatsAppChatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            id = "welcome_1",
            text = "👋 Hello! I'm your WhatsApp Finance Assistant powered by a real-time secure AI RAG Model. Connect to sync and ask me any queries about your account balances, spending trends, or transaction history!",
            isFromUser = false
        )
    ))

    fun connectWhatsAppBot() {
        viewModelScope.launch {
            isWhatsAppBotTyping.value = true
            delay(1000)
            isWhatsAppConnected.value = true
            val connectedMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = "🟢 *WhatsApp Bot Connected successfully!*\n\nI have successfully retrieved your secure cashflow logs. Ask me questions like:\n• _What is my total balance?_\n• _How much did I spend?_\n• _Show recent transactions_",
                isFromUser = false
            )
            whatsAppChatMessages.value = whatsAppChatMessages.value + connectedMsg
            isWhatsAppBotTyping.value = false
        }
    }

    fun disconnectWhatsAppBot() {
        isWhatsAppConnected.value = false
        whatsAppChatMessages.value = listOf(
            ChatMessage(
                id = "welcome_1",
                text = "👋 Hello! I'm your WhatsApp Finance Assistant powered by a real-time secure AI RAG Model. Connect to sync and ask me any queries about your account balances, spending trends, or transaction history!",
                isFromUser = false
            )
        )
    }

    fun sendWhatsAppMessage(text: String) {
        val userMsg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = text,
            isFromUser = true
        )
        whatsAppChatMessages.value = whatsAppChatMessages.value + userMsg

        viewModelScope.launch {
            isWhatsAppBotTyping.value = true
            delay(1500) // Realistic typing delay

            val replyText = generateBotReply(text)
            val botMsg = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = replyText,
                isFromUser = false
            )
            whatsAppChatMessages.value = whatsAppChatMessages.value + botMsg
            isWhatsAppBotTyping.value = false
        }
    }

    private suspend fun generateBotReply(query: String): String {
        val accounts = bankAccounts.value
        val txs = transactions.value
        val linked = accounts.filter { it.isLinked }
        val totalBalance = linked.sumOf { it.balance }
        val expenses = txs.filter { it.type == "EXPENSE" }
        val totalExpenses = expenses.sumOf { it.amount }
        val income = txs.filter { it.type == "INCOME" }
        val totalIncome = income.sumOf { it.amount }

        val q = query.lowercase()

        // 1. Try Gemini if configured
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val accountsText = linked.joinToString("\n") { "- ${it.name}: Balance: ₹${it.balance}" }
                val txsText = txs.take(15).joinToString("\n") { "- ${it.merchantName}: ₹${it.amount} [${it.type}, Category: ${it.category}]" }
                val prompt = """
                    You are the WhatsApp Finance Chatbot for FinTrack.
                    The user has asked: "$query"
                    
                    Current Financial Status (RAG Context):
                    - Connected Accounts: ${linked.size}
                    - Total Balance: ₹$totalBalance
                    - Total Income: ₹$totalIncome
                    - Total Expenses: ₹$totalExpenses
                    
                    Linked Accounts Detail:
                    $accountsText
                    
                    Recent Transactions:
                    $txsText
                    
                    Answer the user's question directly, helpfully, and professionally. Maintain a friendly chatbot persona. Keep the reply short (max 3-4 sentences) and well-formatted for WhatsApp (you can use bullet points or simple emojis, and markdown like *bold* or _italic_).
                    Do not mention internal technical terms or "RAG context". Just reply naturally as a friendly WhatsApp contact.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.6f)
                )

                val response = RetrofitGeminiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    return responseText.trim()
                }
            } catch (e: Exception) {
                // Fallback to local heuristic
            }
        }

        // 2. Intelligent local heuristic parser
        return when {
            q.contains("balance") || q.contains("how much money") || q.contains("net worth") -> {
                val accountList = if (linked.isEmpty()) "• No linked accounts found." else linked.joinToString("\n") { "• *${it.name}*: ₹${String.format("%.2f", it.balance)}" }
                "📊 *Account Balances*:\n$accountList\n\n*Total Net Worth*: ₹${String.format("%.2f", totalBalance)}"
            }
            q.contains("expense") || q.contains("spent") || q.contains("spending") -> {
                val foodExp = expenses.filter { it.category == "FOOD" }.sumOf { it.amount }
                val billsExp = expenses.filter { it.category == "BILLS" }.sumOf { it.amount }
                val subExp = expenses.filter { it.category == "SUBSCRIPTIONS" }.sumOf { it.amount }
                val shopExp = expenses.filter { it.category == "SHOPPING" }.sumOf { it.amount }
                val otherExp = totalExpenses - (foodExp + billsExp + subExp + shopExp)
                
                "💸 *Expense Breakdown*:\n" +
                        "• 🍔 *Food*: ₹${String.format("%.2f", foodExp)}\n" +
                        "• ⚡ *Bills*: ₹${String.format("%.2f", billsExp)}\n" +
                        "• 📺 *Subscriptions*: ₹${String.format("%.2f", subExp)}\n" +
                        "• 🛍️ *Shopping*: ₹${String.format("%.2f", shopExp)}\n" +
                        "• 📦 *Other*: ₹${String.format("%.2f", otherExp)}\n\n" +
                        "*Total Monthly Expenses*: ₹${String.format("%.2f", totalExpenses)}"
            }
            q.contains("income") || q.contains("salary") || q.contains("earned") -> {
                val incomeList = if (income.isEmpty()) "• No income records found." else income.take(3).joinToString("\n") { "• *${it.merchantName}*: ₹${String.format("%.2f", it.amount)}" }
                "💰 *Recent Income Credits*:\n$incomeList\n\n*Total Monthly Income*: ₹${String.format("%.2f", totalIncome)}"
            }
            q.contains("transaction") || q.contains("recent") || q.contains("history") -> {
                val txList = if (txs.isEmpty()) "• No transactions found." else txs.take(5).joinToString("\n") { 
                    val prefix = if (it.type == "EXPENSE") "🔴" else "🟢"
                    "$prefix *${it.merchantName}*: ₹${String.format("%.2f", it.amount)} (${it.category})"
                }
                "🕒 *Recent Transactions*:\n$txList"
            }
            q.contains("hello") || q.contains("hi") || q.contains("hey") -> {
                "👋 Hello! I'm your WhatsApp Chatbot Assistant. How can I help you manage your finances today?\n\nTry asking me:\n• _What is my balance?_\n• _Show my recent expenses_\n• _List my recent transactions_"
            }
            else -> {
                "🤖 I'm here to help track your cashflow! I can assist with balances, expense category breakdowns, recent transaction logs, or budget summaries. Try asking about your *balances* or *recent transactions*!"
            }
        }
    }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Welcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Onboarding & Profile Info
    val userFullName = MutableStateFlow("John Doe")
    val isTruecallerConsentShown = MutableStateFlow(false)
    val phoneChooserDialogShown = MutableStateFlow(false)
    val emailChooserDialogShown = MutableStateFlow(false)
    val isEmailAdded = MutableStateFlow(false)
    val linkedEmail = MutableStateFlow("")
    val isScanningEmail = MutableStateFlow(false)

    // Expense Tracking Setup & Ingestion
    val isExpenseTrackingEnabled = MutableStateFlow(false)
    val isAnalyzingSMS = MutableStateFlow(false)
    val isBackupDialogShown = MutableStateFlow(false)
    val isSMSPermissionDialogShown = MutableStateFlow(false)
    val isRescanningSMS = MutableStateFlow(false)

    // Left Drawer Dialog States
    val isWeeklySummaryDialogShown = MutableStateFlow(false)
    val isExportStatementDialogShown = MutableStateFlow(false)
    val isReportSMSDialogShown = MutableStateFlow(false)
    val isHelpFAQDialogShown = MutableStateFlow(false)

    // Feedback Indicator
    val feedbackMessage = MutableStateFlow<String?>(null)

    // Launch in Money Manager preference state
    val launchInMoneyManager = MutableStateFlow(true)
    val manageRemindersEnabled = MutableStateFlow(true)

    // Credit Categories & Tags lists
    val creditCategories = MutableStateFlow<List<String>>(listOf("Salary", "Investment", "Refund", "Cash Deposit", "Interest"))
    val tags = MutableStateFlow<List<String>>(listOf("Reimbursable", "Office", "Personal Trip", "Gym", "Gift"))

    // Custom Category Management
    val customCategories = MutableStateFlow<List<CustomCategory>>(
        listOf(
            CustomCategory("FOOD", "Food & Dining", Color(0xFFB4E197), "🍔"),
            CustomCategory("TRANSPORT", "Transport", Color(0xFFD0BCFF), "🚗"),
            CustomCategory("SHOPPING", "Shopping", Color(0xFFFDD835), "🛍️"),
            CustomCategory("BILLS", "Bills & Utilities", Color(0xFFF2B8B5), "⚡"),
            CustomCategory("SUBSCRIPTIONS", "Subscriptions", Color(0xFFFF8A65), "🎬"),
            CustomCategory("TRANSFERS", "Transfers & Rent", Color(0xFF4DB6AC), "🏠"),
            CustomCategory("UNCATEGORIZED", "Uncategorized", Color(0xFF90A4AE), "❓")
        )
    )

    fun createCategory(name: String, color: Color, icon: String) {
        val newId = name.uppercase().replace(" ", "_")
        val newCat = CustomCategory(newId, name, color, icon)
        customCategories.value = customCategories.value + newCat
        showFeedback("Category '$name' created successfully")
    }

    fun deleteCategory(id: String) {
        val cat = customCategories.value.find { it.id == id }
        if (cat != null) {
            customCategories.value = customCategories.value.filter { it.id != id }
            showFeedback("Category '${cat.name}' deleted")
        }
    }

    fun createCreditCategory(name: String) {
        creditCategories.value = creditCategories.value + name
        showFeedback("Credit category '$name' created")
    }

    fun deleteCreditCategory(name: String) {
        creditCategories.value = creditCategories.value.filter { it != name }
        showFeedback("Credit category '$name' deleted")
    }

    fun createTag(name: String) {
        tags.value = tags.value + name
        showFeedback("Tag '$name' created")
    }

    fun deleteTag(name: String) {
        tags.value = tags.value.filter { it != name }
        showFeedback("Tag '$name' deleted")
    }

    fun showFeedback(msg: String) {
        viewModelScope.launch {
            feedbackMessage.value = msg
            delay(1500)
            if (feedbackMessage.value == msg) {
                feedbackMessage.value = null
            }
        }
    }

    fun toggleAccountVisibility(bankId: String, isHidden: Boolean) {
        viewModelScope.launch {
            val account = bankAccounts.value.find { it.id == bankId }
            if (account != null) {
                repository.updateAccount(account.copy(isHidden = isHidden))
                showFeedback(if (isHidden) "Account hidden from tracking" else "Account unhidden for tracking")
            }
        }
    }

    fun toggleAccountLinking(bankId: String, isLinked: Boolean) {
        viewModelScope.launch {
            val account = bankAccounts.value.find { it.id == bankId }
            if (account != null) {
                val updated = account.copy(isLinked = isLinked)
                repository.updateAccount(updated)
                if (isLinked) {
                    loadMockTransactionsForBank(bankId)
                    showFeedback("Account linked successfully")
                } else {
                    repository.deleteTransactionsForAccount(bankId)
                    showFeedback("Account unlinked from tracking")
                }
            } else {
                val originalBank = availableBanks.find { it.id == bankId }
                if (originalBank != null) {
                    val newAccount = originalBank.copy(isLinked = isLinked)
                    repository.insertAccount(newAccount)
                    if (isLinked) {
                        loadMockTransactionsForBank(bankId)
                        showFeedback("Account linked successfully")
                    }
                }
            }
        }
    }

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError.asStateFlow()

    private val _otpTimer = MutableStateFlow(30)
    val otpTimer: StateFlow<Int> = _otpTimer.asStateFlow()

    private val _bankSearchQuery = MutableStateFlow("")
    val bankSearchQuery: StateFlow<String> = _bankSearchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Screen navigation stack
    private val screenStack = mutableListOf<Screen>()

    // Notification Toggles (saved locally in-memory for MVP, can be toggled)
    val weeklySummaryToggle = MutableStateFlow(true)
    val unusualSpendToggle = MutableStateFlow(true)
    val accountIssuesToggle = MutableStateFlow(false)

    // Expose database states
    val bankAccounts: StateFlow<List<BankAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules: StateFlow<List<CategoryRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated available banks to link
    val availableBanks = listOf(
        BankAccount("hdfc", "HDFC Bank", "XXXX 8821", 48250.00),
        BankAccount("icici", "ICICI Bank", "XXXX 4192", 12140.00),
        BankAccount("sbi", "State Bank of India", "XXXX 0938", 8400.00),
        BankAccount("gpay", "GPay / UPI Wallet", "XXXX 5012", 1500.00)
    )

    val allIndianBanks = listOf(
        Pair("sbi", "State Bank of India"),
        Pair("hdfc", "HDFC Bank"),
        Pair("icici", "ICICI Bank"),
        Pair("pnb", "Punjab National Bank"),
        Pair("bob", "Bank of Baroda"),
        Pair("canara", "Canara Bank"),
        Pair("union_bank", "Union Bank of India"),
        Pair("axis", "Axis Bank"),
        Pair("kotak", "Kotak Mahindra Bank"),
        Pair("indusind", "IndusInd Bank"),
        Pair("boi", "Bank of India"),
        Pair("central_bank", "Central Bank of India"),
        Pair("indian_bank", "Indian Bank"),
        Pair("uco", "UCO Bank"),
        Pair("iob", "Indian Overseas Bank"),
        Pair("psb", "Punjab & Sind Bank"),
        Pair("idbi", "IDBI Bank"),
        Pair("yes_bank", "YES Bank"),
        Pair("federal", "Federal Bank"),
        Pair("idfc_first", "IDFC First Bank"),
        Pair("bandhan", "Bandhan Bank"),
        Pair("south_indian", "South Indian Bank"),
        Pair("karur_vysya", "Karur Vysya Bank"),
        Pair("karnataka_bank", "Karnataka Bank"),
        Pair("jk_bank", "Jammu & Kashmir Bank"),
        Pair("city_union", "City Union Bank"),
        Pair("tmb", "Tamilnad Mercantile Bank"),
        Pair("rbl", "RBL Bank"),
        Pair("dcb", "DCB Bank"),
        Pair("nainital", "Nainital Bank"),
        Pair("paytm", "Paytm Payments Bank"),
        Pair("airtel", "Airtel Payments Bank"),
        Pair("ippb", "India Post Payments Bank"),
        Pair("jio", "Jio Payments Bank"),
        Pair("fino", "Fino Payments Bank"),
        Pair("nsdl", "NSDL Payments Bank"),
        Pair("au_sf", "AU Small Finance Bank"),
        Pair("equitas_sf", "Equitas Small Finance Bank"),
        Pair("ujjivan_sf", "Ujjivan Small Finance Bank"),
        Pair("capital_sf", "Capital Small Finance Bank"),
        Pair("esaf_sf", "ESAF Small Finance Bank"),
        Pair("suryoday_sf", "Suryoday Small Finance Bank"),
        Pair("utkarsh_sf", "Utkarsh Small Finance Bank"),
        Pair("fincare_sf", "Fincare Small Finance Bank"),
        Pair("jana_sf", "Jana Small Finance Bank"),
        Pair("shivalik_sf", "Shivalik Small Finance Bank"),
        Pair("unity_sf", "Unity Small Finance Bank"),
        Pair("northeast_sf", "North East Small Finance Bank"),
        Pair("saraswat_coop", "Saraswat Co-operative Bank"),
        Pair("cosmos_coop", "Cosmos Co-operative Bank"),
        Pair("svc_coop", "SVC Co-operative Bank"),
        Pair("abhyudaya_coop", "Abhyudaya Co-operative Bank"),
        Pair("bharat_coop", "Bharat Co-operative Bank"),
        Pair("tjsb_coop", "TJSB Sahakari Bank"),
        Pair("citizen_coop", "Citizencredit Co-operative Bank"),
        Pair("kalupur_coop", "Kalupur Commercial Co-operative Bank"),
        Pair("mehsana_coop", "Mehsana Urban Co-operative Bank"),
        Pair("amd_coop", "Ahmedabad Mercantile Co-operative Bank"),
        Pair("nutan_coop", "Nutan Nagarik Sahakari Bank"),
        Pair("rajkot_coop", "Rajkot Nagarik Sahakari Bank"),
        Pair("surat_coop", "Surat People's Co-operative Bank"),
        Pair("varachha_coop", "Varachha Co-operative Bank"),
        Pair("zoroastrian_coop", "Zoroastrian Co-operative Bank"),
        Pair("dombivli_coop", "Dombivli Nagari Sahakari Bank"),
        Pair("kalyan_coop", "Kalyan Janata Sahakari Bank"),
        Pair("janaseva_coop", "Janaseva Sahakari Bank"),
        Pair("jalgaon_coop", "Jalgaon Janata Sahakari Bank"),
        Pair("sangli_coop", "Sangli Urban Co-operative Bank"),
        Pair("satara_coop", "Satara Sahakari Bank"),
        Pair("apna_coop", "Apna Sahakari Bank"),
        Pair("kurla_coop", "Kurla Nagarik Sahakari Bank"),
        Pair("mughal_coop", "Mughal Co-operative Bank"),
        Pair("kokan_coop", "Kokan Mercantile Co-operative Bank"),
        Pair("newindia_coop", "New India Co-operative Bank"),
        Pair("pratham_coop", "Pratham Co-operative Bank"),
        Pair("prime_coop", "Prime Co-operative Bank"),
        Pair("national_coop", "National Co-operative Bank"),
        Pair("bombay_coop", "Bombay Mercantile Co-operative Bank"),
        Pair("maha_coop", "Maharashtra State Co-operative Bank"),
        Pair("ap_coop", "Andhra Pradesh State Co-operative Bank"),
        Pair("telangana_coop", "Telangana State Co-operative Bank"),
        Pair("karnataka_coop", "Karnataka State Co-operative Bank"),
        Pair("kerala_coop", "Kerala State Co-operative Bank"),
        Pair("tn_coop", "Tamil Nadu State Apex Co-operative Bank"),
        Pair("pondi_coop", "Pondicherry State Co-operative Bank"),
        Pair("goa_coop", "Goa State Co-operative Bank"),
        Pair("gujarat_coop", "Gujarat State Co-operative Bank"),
        Pair("rajasthan_coop", "Rajasthan State Co-operative Bank"),
        Pair("mp_coop", "Madhya Pradesh State Co-operative Bank"),
        Pair("up_coop", "Uttar Pradesh Co-operative Bank"),
        Pair("bihar_coop", "Bihar State Co-operative Bank"),
        Pair("jharkhand_coop", "Jharkhand State Co-operative Bank"),
        Pair("wb_coop", "West Bengal State Co-operative Bank"),
        Pair("odisha_coop", "Odisha State Co-operative Bank"),
        Pair("assam_coop", "Assam Cooperative Apex Bank"),
        Pair("meghalaya_coop", "Meghalaya Co-operative Apex Bank"),
        Pair("mizoram_coop", "Mizoram Cooperative Apex Bank"),
        Pair("tripura_coop", "Tripura State Co-operative Bank"),
        Pair("nagaland_coop", "Nagaland State Co-operative Bank"),
        Pair("manipur_coop", "Manipur State Co-operative Bank"),
        Pair("arunachal_coop", "Arunachal Pradesh State Co-operative Bank"),
        Pair("sikkim_coop", "Sikkim State Co-operative Bank"),
        Pair("hp_coop", "Himachal Pradesh State Co-operative Bank"),
        Pair("punjab_coop", "Punjab State Co-operative Bank"),
        Pair("haryana_coop", "Haryana State Co-operative Bank"),
        Pair("delhi_coop", "Delhi State Co-operative Bank"),
        Pair("std_chartered", "Standard Chartered Bank"),
        Pair("hsbc", "HSBC India"),
        Pair("citibank", "Citibank India"),
        Pair("deutsche", "Deutsche Bank India"),
        Pair("dbs", "DBS Bank India"),
        Pair("barclays", "Barclays Bank India"),
        Pair("bankofamerica", "Bank of America India"),
        Pair("jpmorgan", "JP Morgan Chase Bank India"),
        Pair("socgen", "Societe Generale India"),
        Pair("bnpparibas", "BNP Paribas India"),
        Pair("mizuho", "Mizuho Bank India"),
        Pair("mufg", "MUFG Bank India"),
        Pair("smbc", "Sumitomo Mitsui Banking Corporation"),
        Pair("shinhan", "Shinhan Bank India"),
        Pair("woori", "Woori Bank India"),
        Pair("keb_hana", "KEB Hana Bank"),
        Pair("icbc", "Industrial & Commercial Bank of China"),
        Pair("bankofchina", "Bank of China India"),
        Pair("sbm", "State Bank of Mauritius"),
        Pair("sberbank", "Sberbank India"),
        Pair("vtb", "VTB Bank India"),
        Pair("qnb", "Qatar National Bank India"),
        Pair("dohabank", "Doha Bank India"),
        Pair("fab", "FirstAbuDhabi Bank"),
        Pair("emirates_nbd", "Emirates NBD India"),
        Pair("mashreq", "Mashreq Bank India"),
        Pair("nbk", "National Bank of Kuwait"),
        Pair("oman_int", "Oman International Bank"),
        Pair("bbk", "Bank of Bahrain & Kuwait"),
        Pair("ab_bank", "Arab Bangladesh Bank"),
        Pair("sonali", "Sonali Bank India"),
        Pair("nepal_sbi", "Nepal SBI Bank"),
        Pair("everest", "Everest Bank India"),
        Pair("ceylon", "Bank of Ceylon India"),
        Pair("seylan", "Seylan Bank"),
        Pair("hatton", "Hatton National Bank"),
        Pair("krung_thai", "Krung Thai Bank"),
        Pair("siam_comm", "Siam Commercial Bank"),
        Pair("maybank", "Maybank India"),
        Pair("cimb", "CIMB Bank India"),
        Pair("rhb", "RHB Bank India"),
        Pair("public_berhad", "Public Bank Berhad India"),
        Pair("uob", "United Overseas Bank India"),
        Pair("ocbc", "Oversea-Chinese Banking Corporation"),
        Pair("btm_ufj", "Bank of Tokyo-Mitsubishi UFJ"),
        Pair("sm_trust", "Sumitomo Mitsui Trust Bank"),
        Pair("norinchukin", "Norinchukin Bank"),
        Pair("shoko_chukin", "Shoko Chukin Bank"),
        Pair("shinsei", "Shinsei Bank"),
        Pair("aozora", "Aozora Bank"),
        Pair("resona", "Resona Bank"),
        Pair("saitama_resona", "Saitama Resona Bank"),
        Pair("fukuoka_bank", "Fukuoka Bank"),
        Pair("nishi_nippon", "Nishi-Nippon City Bank"),
        Pair("hachijuni", "Hachijuni Bank"),
        Pair("chiba_bank", "Chiba Bank"),
        Pair("shizuoka_bank", "Shizuoka Bank"),
        Pair("kyoto_bank", "Bank of Kyoto"),
        Pair("juroku", "Juroku Bank"),
        Pair("ogaki_kyoritsu", "Ogaki Kyoritsu Bank"),
        Pair("hyakugo", "Hyakugo Bank"),
        Pair("shiga_bank", "Shiga Bank"),
        Pair("nanto", "Nanto Bank"),
        Pair("hiroshima_bank", "Hiroshima Bank"),
        Pair("yamaguchi_bank", "Yamaguchi Bank"),
        Pair("iyo_bank", "Iyo Bank"),
        Pair("san_in_godo", "San-In Godo Bank"),
        Pair("hokuriku_bank", "Hokuriku Bank"),
        Pair("hokkaido_bank", "Hokkaido Bank"),
        Pair("bank_77", "77 Bank"),
        Pair("toho_bank", "Toho Bank"),
        Pair("gunma_bank", "Gunma Bank"),
        Pair("joyo_bank", "Joyo Bank"),
        Pair("ashikaga_bank", "Ashikaga Bank"),
        Pair("musashino", "Musashino Bank"),
        Pair("keiyo", "Keiyo Bank"),
        Pair("chiba_kogyo", "Chiba Kogyo Bank"),
        Pair("yachiyo", "Yachiyo Bank"),
        Pair("tokyostar", "Tokyo Star Bank"),
        Pair("kansai_urban", "Kansai Urban Bank"),
        Pair("kinki_osaka", "Kinki Osaka Bank"),
        Pair("minato_bank", "Minato Bank"),
        Pair("shiga_central", "Shiga Central Bank"),
        Pair("tajimi_shinkin", "Tajimi Shinkin Bank"),
        Pair("okazaki_shinkin", "Okazaki Shinkin Bank"),
        Pair("ichinomiya_shinkin", "Ichinomiya Shinkin Bank"),
        Pair("seto_shinkin", "Seto Shinkin Bank"),
        Pair("anjo_shinkin", "Anjo Shinkin Bank"),
        Pair("handa_shinkin", "Handa Shinkin Bank"),
        Pair("toyokawa_shinkin", "Toyokawa Shinkin Bank"),
        Pair("gamagori_shinkin", "Gamagori Shinkin Bank"),
        Pair("toyohashi_shinkin", "Toyohashi Shinkin Bank"),
        Pair("shizuoka_shinkin", "Shizuoka Shinkin Bank"),
        Pair("hamamatsu_shinkin", "Hamamatsu Shinkin Bank"),
        Pair("numazu_shinkin", "Numazu Shinkin Bank"),
        Pair("mishima_shinkin", "Mishima Shinkin Bank"),
        Pair("fujimiya_shinkin", "Fujimiya Shinkin Bank"),
        Pair("fujieda_shinkin", "Fujieda Shinkin Bank"),
        Pair("shikoku_bank", "Shikoku Bank"),
        Pair("oita_bank", "Oita Bank"),
        Pair("miyazaki_bank", "Miyazaki Bank"),
        Pair("higo_bank", "Higo Bank"),
        Pair("kagoshima_bank", "Kagoshima Bank"),
        Pair("okinawa_bank", "Bank of Okinawa")
    )

    private var otpTimerJob: Job? = null

    init {
        // Clear db initially to start fresh for onboarding demo,
        // or check if accounts are already linked.
        viewModelScope.launch {
            // Check if user is already onboarded. For convenience, if there are linked accounts,
            // we can auto-advance to Dashboard.
            val existingAccounts = repository.allAccounts.first()
            if (existingAccounts.any { it.isLinked }) {
                _currentScreen.value = Screen.Dashboard
            }
        }
    }

    fun navigateTo(screen: Screen) {
        screenStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = prev
        } else {
            // Default fallbacks
            _currentScreen.value = Screen.Welcome
        }
    }

    fun setPhoneNumber(num: String) {
        _phoneNumber.value = num
    }

    fun setOtpCode(code: String) {
        _otpCode.value = code
        _otpError.value = null
    }

    fun startOtpTimer() {
        otpTimerJob?.cancel()
        _otpTimer.value = 30
        otpTimerJob = viewModelScope.launch {
            while (_otpTimer.value > 0) {
                delay(1000)
                _otpTimer.value -= 1
            }
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            if (_otpCode.value == "123456" || _otpCode.value == "422346") {
                _otpError.value = null
                _otpCode.value = ""
                // Advanced to link account prompt
                navigateTo(Screen.LinkAccountPrompt)
            } else {
                _otpError.value = "That code didn’t work. Please try again or request a new code."
            }
        }
    }

    fun setBankSearchQuery(query: String) {
        _bankSearchQuery.value = query
    }

    fun linkBank(bankId: String) {
        viewModelScope.launch {
            _currentScreen.value = Screen.LinkingProgress(bankId)
            delay(3000) // Simulate import speed

            // Complete linking and load simulated transactions
            var originalBank = availableBanks.find { it.id == bankId }
            if (originalBank == null) {
                val foundPair = allIndianBanks.find { it.first == bankId }
                if (foundPair != null) {
                    val randNo = (1000..9999).random()
                    val randBalance = (5000..65000).random().toDouble()
                    originalBank = BankAccount(foundPair.first, foundPair.second, "XXXX $randNo", randBalance)
                }
            }
            if (originalBank == null) return@launch
            val linkedBank = originalBank.copy(isLinked = true, lastSyncTime = System.currentTimeMillis())

            repository.insertAccount(linkedBank)
            loadMockTransactionsForBank(bankId)

            _currentScreen.value = Screen.Dashboard
        }
    }

    fun unlinkBank(bankId: String) {
        viewModelScope.launch {
            val account = bankAccounts.value.find { it.id == bankId }
            if (account != null) {
                // Delete its transactions and marks it unlinked or delete it from db
                repository.deleteTransactionsForAccount(bankId)
                val unlinked = account.copy(isLinked = false, lastSyncTime = 0)
                repository.insertAccount(unlinked)
            }
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            delay(2000) // Simulation
            // Randomly trigger error 10% of the time for testing, or just succeed
            _isSyncing.value = false
            // Update last sync times
            bankAccounts.value.forEach { account ->
                if (account.isLinked) {
                    repository.insertAccount(account.copy(lastSyncTime = System.currentTimeMillis()))
                }
            }
        }
    }

    fun triggerSyncErrorForDemo() {
        viewModelScope.launch {
            _isSyncing.value = true
            delay(1500)
            _isSyncing.value = false
            _syncError.value = "We couldn't refresh your transactions. Last updated at ${getFormattedLastSyncTime()}."
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun scanEmailForTransactions(email: String) {
        viewModelScope.launch {
            isScanningEmail.value = true
            showFeedback("Scanning $email for statements & receipts...")
            delay(3000) // Simulate scanning speed
            
            // Insert virtual email account so its balance counts towards the total
            val emailAccount = BankAccount(
                id = "email",
                name = "Email Sync Account",
                accountNo = email,
                balance = 21310.00,
                isLinked = true,
                lastSyncTime = System.currentTimeMillis()
            )
            repository.insertAccount(emailAccount)

            val now = System.currentTimeMillis()
            val dayMs = 24 * 3600 * 1000L
            val emailTransactions = listOf(
                Transaction(
                    merchantName = "Refund from Amazon E-Receipt",
                    amount = 1850.0,
                    timestamp = now - 1 * dayMs,
                    type = "INCOME",
                    category = "INCOME",
                    accountSource = "email"
                ),
                Transaction(
                    merchantName = "Netflix Email Invoice",
                    amount = 649.0,
                    timestamp = now - 2 * dayMs,
                    type = "EXPENSE",
                    category = "SUBSCRIPTIONS",
                    accountSource = "email"
                ),
                Transaction(
                    merchantName = "Uber Email Ride Receipt",
                    amount = 320.0,
                    timestamp = now - 3 * dayMs,
                    type = "EXPENSE",
                    category = "TRANSPORT",
                    accountSource = "email"
                ),
                Transaction(
                    merchantName = "Zomato Dineout Receipt",
                    amount = 750.0,
                    timestamp = now - 4 * dayMs,
                    type = "EXPENSE",
                    category = "FOOD",
                    accountSource = "email"
                ),
                Transaction(
                    merchantName = "Freelance Client Payslip",
                    amount = 12000.0,
                    timestamp = now - 5 * dayMs,
                    type = "INCOME",
                    category = "INCOME",
                    accountSource = "email"
                )
            )
            repository.insertTransactions(emailTransactions)
            isScanningEmail.value = false
            showFeedback("Extracted 5 transactions from email statements!")
        }
    }

    fun unlinkEmailAccount() {
        viewModelScope.launch {
            isEmailAdded.value = false
            val email = linkedEmail.value
            linkedEmail.value = ""
            repository.deleteTransactionsForAccount("email")
            val emailAccount = bankAccounts.value.find { it.id == "email" }
            if (emailAccount != null) {
                repository.deleteAccount(emailAccount)
            }
            showFeedback("Disconnected email $email and removed synced statements.")
        }
    }

    fun runRescanSMS() {
        viewModelScope.launch {
            isRescanningSMS.value = true
            delay(2000)
            isRescanningSMS.value = false
            val now = System.currentTimeMillis()
            val newTx1 = Transaction(
                merchantName = "Starbucks Coffee Override",
                amount = 280.0,
                timestamp = now,
                type = "EXPENSE",
                category = "FOOD",
                accountSource = "gpay"
            )
            val newTx2 = Transaction(
                merchantName = "Airtel Direct Recharge",
                amount = 499.0,
                timestamp = now - 3600000L,
                type = "EXPENSE",
                category = "BILLS",
                accountSource = "sbi"
            )
            repository.insertTransaction(newTx1)
            repository.insertTransaction(newTx2)
            showFeedback("Rescan complete! Processed inbox SMS and added 2 transactions.")
        }
    }

    fun restoreTransactionsFromBackup() {
        viewModelScope.launch {
            isAnalyzingSMS.value = true
            delay(1500)
            isAnalyzingSMS.value = false
            
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val backupTxList = listOf(
                Transaction(merchantName = "Uber Prime Auto", amount = 120.0, timestamp = now - 1 * dayMs, type = "EXPENSE", category = "TRANSPORT", accountSource = "hdfc"),
                Transaction(merchantName = "Swiggy Instant Grocery", amount = 640.0, timestamp = now - 2 * dayMs, type = "EXPENSE", category = "FOOD", accountSource = "hdfc"),
                Transaction(merchantName = "Zomato Premium Dinner", amount = 1150.0, timestamp = now - 3 * dayMs, type = "EXPENSE", category = "FOOD", accountSource = "icici"),
                Transaction(merchantName = "ACT Fibernet Broadband", amount = 1050.0, timestamp = now - 4 * dayMs, type = "EXPENSE", category = "BILLS", accountSource = "sbi"),
                Transaction(merchantName = "Zara Fashion Store", amount = 3400.0, timestamp = now - 5 * dayMs, type = "EXPENSE", category = "SHOPPING", accountSource = "icici"),
                Transaction(merchantName = "Netflix Streaming Premium", amount = 649.0, timestamp = now - 6 * dayMs, type = "EXPENSE", category = "SUBSCRIPTIONS", accountSource = "hdfc"),
                Transaction(merchantName = "Dividend Payout received", amount = 4200.0, timestamp = now - 7 * dayMs, type = "INCOME", category = "INCOME", accountSource = "hdfc")
            )
            repository.insertTransactions(backupTxList)
            
            availableBanks.forEach { bank ->
                repository.insertAccount(bank.copy(isLinked = true, lastSyncTime = System.currentTimeMillis()))
            }
            
            isExpenseTrackingEnabled.value = true
            isBackupDialogShown.value = false
            showFeedback("Backup restored! 7 past transactions and categories loaded.")
        }
    }

    fun updateTransactionCategory(transactionId: Int, newCategory: String, applyToFuture: Boolean) {
        viewModelScope.launch {
            val txList = transactions.value
            val tx = txList.find { it.id == transactionId } ?: return@launch
            val updatedTx = tx.copy(category = newCategory, isUserCorrected = true)
            repository.updateTransaction(updatedTx)

            if (applyToFuture) {
                // Save a custom categorization rule for this merchant
                repository.insertRule(CategoryRule(merchantPattern = tx.merchantName, category = newCategory))
                
                // Retroactively update all other transactions with this merchant pattern
                txList.forEach { item ->
                    if (item.merchantName == tx.merchantName && item.id != transactionId) {
                        repository.updateTransaction(item.copy(category = newCategory, isUserCorrected = true))
                    }
                }
            }
        }
    }

    private fun getFormattedLastSyncTime(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private suspend fun loadMockTransactionsForBank(bankId: String) {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        val mockList = when (bankId) {
            "hdfc" -> listOf(
                Transaction(merchantName = "Swiggy Food Delivery", amount = 350.0, timestamp = now - 2 * 3600 * 1000L, type = "EXPENSE", category = "FOOD", accountSource = "hdfc"),
                Transaction(merchantName = "Netflix Subscription", amount = 199.0, timestamp = now - 1 * dayMs, type = "EXPENSE", category = "SUBSCRIPTIONS", accountSource = "hdfc"),
                Transaction(merchantName = "Monthly Salary Credited", amount = 75000.0, timestamp = now - 5 * dayMs, type = "INCOME", category = "INCOME", accountSource = "hdfc"),
                Transaction(merchantName = "Landlord Rent Payment", amount = 15000.0, timestamp = now - 4 * dayMs, type = "EXPENSE", category = "TRANSFERS", accountSource = "hdfc"),
                Transaction(merchantName = "Amazon India Shopping", amount = 1299.0, timestamp = now - 3 * dayMs, type = "EXPENSE", category = "SHOPPING", accountSource = "hdfc")
            )
            "icici" -> listOf(
                Transaction(merchantName = "Uber India Rides", amount = 180.0, timestamp = now - 5 * 3600 * 1000L, type = "EXPENSE", category = "TRANSPORT", accountSource = "icici"),
                Transaction(merchantName = "Starbucks Coffee", amount = 320.0, timestamp = now - 2 * dayMs, type = "EXPENSE", category = "FOOD", accountSource = "icici")
            )
            "sbi" -> listOf(
                Transaction(merchantName = "Airtel Broadband Bill", amount = 799.0, timestamp = now - 12 * 3600 * 1000L, type = "EXPENSE", category = "BILLS", accountSource = "sbi")
            )
            "gpay" -> listOf(
                Transaction(merchantName = "Zomato Dineout", amount = 450.0, timestamp = now - 1 * dayMs, type = "EXPENSE", category = "FOOD", accountSource = "gpay"),
                Transaction(merchantName = "Local Chai Tapri Shop", amount = 40.0, timestamp = now - 15 * 60 * 1000L, type = "EXPENSE", category = "UNCATEGORIZED", accountSource = "gpay")
            )
            else -> emptyList()
        }

        // Apply rules to loaded mock transactions
        val activeRules = repository.getAllRules()
        val processedMockList = mockList.map { tx ->
            val matchingRule = activeRules.find { rule -> rule.merchantPattern == tx.merchantName }
            if (matchingRule != null) {
                tx.copy(category = matchingRule.category, isUserCorrected = true)
            } else {
                tx
            }
        }

        repository.insertTransactions(processedMockList)
    }

    val aiInsights = MutableStateFlow<List<AIInsightRaw>>(emptyList())
    val isGeneratingInsights = MutableStateFlow(false)
    val insightsError = MutableStateFlow<String?>(null)

    fun generateRAGInsights() {
        viewModelScope.launch {
            isGeneratingInsights.value = true
            insightsError.value = null
            
            val accounts = bankAccounts.value.filter { it.isLinked && !it.isHidden }
            val txs = transactions.value
            
            if (accounts.isEmpty() && txs.isEmpty()) {
                aiInsights.value = listOf(
                    AIInsightRaw(
                        title = "No linked accounts yet",
                        desc = "Link a secure bank account or UPI wallet to receive automated cashflow summaries and personalized optimization tips.",
                        iconType = "ALERT",
                        severity = "MEDIUM"
                    ),
                    AIInsightRaw(
                        title = "Offline Simulation Ready",
                        desc = "You can click 'Restore Backup' in the setup menu to load simulated transaction histories for full AI analysis.",
                        iconType = "SAVINGS",
                        severity = "LOW"
                    )
                )
                isGeneratingInsights.value = false
                return@launch
            }

            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1200)
                generateLocalHeuristicInsights(accounts, txs)
                isGeneratingInsights.value = false
                return@launch
            }

            try {
                val accountsText = accounts.joinToString("\n") {
                    "- ${it.name} (${it.accountNo}): Balance: ₹${it.balance}"
                }
                val txsText = txs.take(40).joinToString("\n") {
                    "- ${it.merchantName}: ₹${it.amount} [${it.type}, Category: ${it.category}]"
                }

                val prompt = """
                    You are FinTrack AI, a professional client-side financial advisor.
                    Below is the retrieved real-time financial status and recent transaction data for the user (RAG context).

                    --- Linked Accounts ---
                    $accountsText

                    --- Recent Transactions ---
                    $txsText

                    Based on the above real retrieved data, generate exactly 3 highly specific, action-oriented, and realistic cashflow insights, financial trends, or alerts.
                    Do not hallucinate any information. Focus on their actual spending patterns (e.g., if Swiggy/Zomato or bills is high, point it out. If balance margins are healthy or tight, comment on savings).

                    Return the output strictly as a JSON array of exactly 3 objects. Each object MUST have these exact fields:
                    1. "title": A brief, punchy title (max 5 words).
                    2. "desc": A detailed, highly helpful advice or alert (exactly 1-2 sentences) referencing actual amounts or merchants from the data.
                    3. "iconType": A string that matches one of these: "FOOD", "BILLS", "SUBSCRIPTIONS", "SAVINGS", "ALERT".
                    4. "severity": "HIGH", "MEDIUM", or "LOW".

                    Strictly return ONLY the JSON array without any markdown wrappers (do not include ```json or ```). Ensure it is perfectly parseable.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(
                        responseFormat = ResponseFormat(mimeType = "application/json"),
                        temperature = 0.5f
                    )
                )

                val response = RetrofitGeminiClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (!responseText.isNullOrBlank()) {
                    var cleanedText = responseText.trim()
                    if (cleanedText.startsWith("```json")) {
                        cleanedText = cleanedText.substringAfter("```json")
                    } else if (cleanedText.startsWith("```")) {
                        cleanedText = cleanedText.substringAfter("```")
                    }
                    if (cleanedText.endsWith("```")) {
                        cleanedText = cleanedText.substringBeforeLast("```")
                    }
                    cleanedText = cleanedText.trim()

                    val moshi = com.squareup.moshi.Moshi.Builder()
                        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, AIInsightRaw::class.java)
                    val adapter = moshi.adapter<List<AIInsightRaw>>(type)
                    val parsed = adapter.fromJson(cleanedText)

                    if (!parsed.isNullOrEmpty()) {
                        aiInsights.value = parsed
                    } else {
                        generateLocalHeuristicInsights(accounts, txs)
                    }
                } else {
                    generateLocalHeuristicInsights(accounts, txs)
                }
            } catch (e: Exception) {
                generateLocalHeuristicInsights(accounts, txs)
            } finally {
                isGeneratingInsights.value = false
            }
        }
    }

    private fun generateLocalHeuristicInsights(accounts: List<BankAccount>, txs: List<Transaction>) {
        val list = mutableListOf<AIInsightRaw>()
        val foodExp = txs.filter { it.category == "FOOD" }.sumOf { it.amount }
        val subsExp = txs.filter { it.category == "SUBSCRIPTIONS" }.sumOf { it.amount }
        val billsExp = txs.filter { it.category == "BILLS" }.sumOf { it.amount }

        if (foodExp > 200) {
            list.add(
                AIInsightRaw(
                    title = "Dining & food expenses up",
                    desc = "Your food deliveries sum up to ₹${foodExp.toInt()} this week. Cooking twice as often can save you roughly ₹350 next week!",
                    iconType = "FOOD",
                    severity = "MEDIUM"
                )
            )
        }
        if (subsExp > 0) {
            list.add(
                AIInsightRaw(
                    title = "Subscription alerts",
                    desc = "Your recurring active subscription charges total ₹${subsExp.toInt()}. Review unused plans to boost your savings margin.",
                    iconType = "SUBSCRIPTIONS",
                    severity = "LOW"
                )
            )
        }
        if (billsExp > 500) {
            list.add(
                AIInsightRaw(
                    title = "Utility billing alert",
                    desc = "Utility and broadband charges sum up to ₹${billsExp.toInt()}. Ensure automatic mandates are set up to avoid delay penalties.",
                    iconType = "BILLS",
                    severity = "LOW"
                )
            )
        }
        
        val balance = accounts.sumOf { it.balance }
        if (list.size < 3) {
            if (balance > 10000) {
                list.add(
                    AIInsightRaw(
                        title = "Healthy balance cushion",
                        desc = "You have a solid total balance cushion of ₹${balance.toInt()}. Consider starting a monthly sweep-in deposit of ₹2,500.",
                        iconType = "SAVINGS",
                        severity = "LOW"
                    )
                )
            } else {
                list.add(
                    AIInsightRaw(
                        title = "Tight balance margin",
                        desc = "Your current combined balance is ₹${balance.toInt()}. Delay major shopping and non-essential expenses for 4 days.",
                        iconType = "ALERT",
                        severity = "HIGH"
                    )
                )
            }
        }
        if (list.size < 3) {
            list.add(
                AIInsightRaw(
                    title = "Automated cash insights",
                    desc = "Weekly sync completed successfully. Your total spending is well within standard limits.",
                    iconType = "SAVINGS",
                    severity = "LOW"
                )
            )
        }
        
        aiInsights.value = list.take(3)
    }

    fun resetAllForDemo() {
        viewModelScope.launch {
            repository.clearAllData()
            _phoneNumber.value = ""
            _otpCode.value = ""
            _otpError.value = null
            screenStack.clear()
            _currentScreen.value = Screen.Welcome
        }
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
