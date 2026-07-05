package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BankAccount
import com.example.data.Transaction
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainNavigationContainer(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isWhatsAppChatbotActive by viewModel.isWhatsAppChatbotActive.collectAsState()
    val canGoBack = currentScreen != Screen.Welcome && currentScreen != Screen.Dashboard

    if (canGoBack || isWhatsAppChatbotActive) {
        BackHandler {
            if (isWhatsAppChatbotActive) {
                viewModel.isWhatsAppChatbotActive.value = false
            } else {
                viewModel.navigateBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
                ) + fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                is Screen.Welcome -> WelcomeScreen(
                    onGetStarted = { viewModel.navigateTo(Screen.PhoneInput) }
                )
                is Screen.PhoneInput -> PhoneInputScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onContinue = { phone ->
                        viewModel.navigateTo(Screen.OtpVerification(phone))
                        viewModel.startOtpTimer()
                    }
                )
                is Screen.OtpVerification -> OtpVerificationScreen(
                    viewModel = viewModel,
                    phone = targetScreen.phone,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.LinkAccountPrompt -> LinkAccountPromptScreen(
                    onLinkAccount = { viewModel.navigateTo(Screen.SelectProvider) },
                    onSkip = { viewModel.navigateTo(Screen.Dashboard) }
                )
                is Screen.SelectProvider -> SelectProviderScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onSelectBank = { bankId ->
                        viewModel.navigateTo(Screen.ConsentAndPermissions(bankId))
                    }
                )
                is Screen.ConsentAndPermissions -> ConsentScreen(
                    bankId = targetScreen.bankId,
                    onBack = { viewModel.navigateBack() },
                    onGrant = { viewModel.linkBank(targetScreen.bankId) }
                )
                is Screen.LinkingProgress -> LinkingProgressScreen(
                    viewModel = viewModel,
                    bankId = targetScreen.bankId
                )
                is Screen.Dashboard -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTxDetail = { txId -> viewModel.navigateTo(Screen.TransactionDetail(txId)) },
                    onNavigateToPreferences = { viewModel.navigateTo(Screen.PreferencesHome) },
                    onNavigateToLinkAccount = { viewModel.navigateTo(Screen.SelectProvider) }
                )
                is Screen.TransactionDetail -> TransactionDetailScreen(
                    transactionId = targetScreen.transactionId,
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onEditCategory = { txId -> viewModel.navigateTo(Screen.CategorySelector(txId)) }
                )
                is Screen.CategorySelector -> CategorySelectorScreen(
                    transactionId = targetScreen.transactionId,
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.PreferencesHome -> PreferencesHomeScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onNavigateToNotifications = { viewModel.navigateTo(Screen.NotificationSettings) },
                    onNavigateToLinkedAccounts = { viewModel.navigateTo(Screen.LinkedAccountsSettings) },
                    onNavigateToManageAccounts = { viewModel.navigateTo(Screen.ManageAccounts) },
                    onNavigateToSpendCategories = { viewModel.navigateTo(Screen.SpendCategories) },
                    onNavigateToCreditCategories = { viewModel.navigateTo(Screen.CreditCategories) },
                    onNavigateToTagsManagement = { viewModel.navigateTo(Screen.TagsManagement) }
                )
                is Screen.NotificationSettings -> NotificationSettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.LinkedAccountsSettings -> LinkedAccountsSettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onNavigateToLinkAccount = { viewModel.navigateTo(Screen.SelectProvider) }
                )
                is Screen.ManageAccounts -> ManageAccountsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.SpendCategories -> SpendCategoriesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.CreditCategories -> CreditCategoriesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.TagsManagement -> TagsManagementScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() }
                )
                is Screen.LinkAccounts -> SelectProviderScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onSelectBank = { bankId ->
                        viewModel.navigateTo(Screen.ConsentAndPermissions(bankId))
                    }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: WELCOME SCREEN
// ==========================================
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WelcomePulse")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeroFloat"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Identity Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricBlue.copy(alpha = 0.2f))
                    .border(1.dp, ElectricBlue, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "FinTrack Logo Icon",
                    tint = ElectricBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "FinTrack",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp
            )
        }

        // Illustration Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
                .offset(y = floatOffset.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onGetStarted()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(16f / 11f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderColor, RoundedCornerShape(32.dp))
                    .padding(20.dp)
            ) {
                // Background Glow radial
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MintGreen.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            radius = size.width * 0.65f
                        )
                    )
                }

                // Static Graphic
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS Tracking",
                        tint = MintGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderColor)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Syncing Symbol",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Syncing...", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MintGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+₹85,000", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // messaging text (Title & Description)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "Track Every Expense.",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Automatically monitor your spending across linked bank accounts and UPI transactions in a single unified, secure dashboard.",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                lineHeight = 21.sp
            )

            // Centered Get Started Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Get Started",
                    color = MintGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onGetStarted() }
                        .testTag("get_started_button")
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: PHONE LOGIN
// ==========================================
@Composable
fun PhoneInputScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    val isPhoneValid = phoneInput.length == 10
    val isFormValid = isPhoneValid && nameInput.isNotBlank()
    val keyboardController = LocalSoftwareKeyboardController.current

    val isTruecallerConsentShown by viewModel.isTruecallerConsentShown.collectAsState()
    val phoneChooserDialogShown by viewModel.phoneChooserDialogShown.collectAsState()

    Scaffold(
        topBar = {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = "Back",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verify your Profile",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect securely to start auto-categorizing banking alerts.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 1-Click Truecaller Verification Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0087FF).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF0087FF), RoundedCornerShape(16.dp))
                            .clickable { viewModel.isTruecallerConsentShown.value = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0087FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Truecaller",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Fill with Truecaller",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto-verify in 1-tap using your active profile",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Arrow Right",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "OR ENTER DETAILS MANUALLY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Name Input Field
                    Text(
                        text = "Your Full Name",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Person",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = { Text("John Doe", color = TextMuted, fontSize = 15.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Input Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mobile Number",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose Google Number",
                            color = ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.phoneChooserDialogShown.value = true }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇮🇳 +91",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(BorderColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = phoneInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 10) {
                                    phoneInput = input
                                }
                            },
                            placeholder = { Text("98765 43210", color = TextMuted, fontSize = 15.sp) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Privacy Note Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield Security",
                            tint = ElectricBlue,
                            modifier = Modifier
                                .size(18.dp)
                                .offset(y = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "We use bank-grade data security protocols. Your private credentials, credentials, passwords, and details are kept safe and local.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Bottom CTA Continue Button
                Button(
                    onClick = {
                        if (isFormValid) {
                            keyboardController?.hide()
                            viewModel.userFullName.value = nameInput
                            viewModel.setPhoneNumber(phoneInput)
                            onContinue(phoneInput)
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintGreen,
                        contentColor = DarkBg,
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp)
                        .testTag("phone_continue_button")
                ) {
                    Text(
                        text = "Request OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Truecaller Consent Overlay Sheet/Dialog
            if (isTruecallerConsentShown) {
                AlertDialog(
                    onDismissRequest = { viewModel.isTruecallerConsentShown.value = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(28.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Truecaller logo",
                                tint = Color(0xFF0087FF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Truecaller Profile Verification", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "Quick verification will auto-share your verified profile info with FinTrack to verify your identity.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBg)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0087FF).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("JD", color = Color(0xFF0087FF), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("John Doe", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified badge",
                                            tint = Color(0xFF0087FF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text("+91 98765 43210", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "By tapping Agree & Autofill, you consent to our Terms of Service & Privacy Policy.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.isTruecallerConsentShown.value = false
                                viewModel.userFullName.value = "John Doe"
                                viewModel.setPhoneNumber("9876543210")
                                viewModel.showFeedback("Profile linked with Truecaller")
                                onContinue("9876543210")
                            }
                        ) {
                            Text("Agree & Autofill", color = Color(0xFF0087FF), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.isTruecallerConsentShown.value = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }

            // Google / Device Account Phone Chooser dialog
            if (phoneChooserDialogShown) {
                AlertDialog(
                    onDismissRequest = { viewModel.phoneChooserDialogShown.value = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text("Select a mobile number", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select a phone number registered with Google Services on this phone to continue.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            listOf("9876543210", "9999988888").forEach { number ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkBg)
                                        .clickable {
                                            phoneInput = number
                                            viewModel.phoneChooserDialogShown.value = false
                                            viewModel.showFeedback("Phone number selected from Google chooser")
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🇮🇳", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("+91 $number", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(imageVector = Icons.Default.Email, contentDescription = "Google linked", tint = ElectricBlue.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { viewModel.phoneChooserDialogShown.value = false }) {
                            Text("Enter Manually", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 3: OTP VERIFICATION
// ==========================================
@Composable
fun OtpVerificationScreen(
    viewModel: FinanceViewModel,
    phone: String,
    onBack: () -> Unit
) {
    val otpCode by viewModel.otpCode.collectAsState()
    val otpError by viewModel.otpError.collectAsState()
    val timer by viewModel.otpTimer.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // SMS & WhatsApp simulation state
    var showSmsNotification by remember { mutableStateOf(false) }
    var showWhatsappNotification by remember { mutableStateOf(false) }
    var smsFailed by remember { mutableStateOf(false) }
    var whatsappFailed by remember { mutableStateOf(false) }

    LaunchedEffect(smsFailed) {
        if (!smsFailed) {
            delay(1500)
            showSmsNotification = true
        } else {
            showSmsNotification = false
        }
    }

    LaunchedEffect(whatsappFailed) {
        if (!whatsappFailed) {
            delay(2500)
            showWhatsappNotification = true
        } else {
            showWhatsappNotification = false
        }
    }

    Scaffold(
        topBar = {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = "Back",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verify OTP",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Code sent to +91 $phone",
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // SMS channel status
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (smsFailed) CoralRed.copy(alpha = 0.15f) else MintGreen.copy(alpha = 0.15f))
                                .border(1.dp, if (smsFailed) CoralRed else MintGreen, RoundedCornerShape(12.dp))
                                .clickable { smsFailed = !smsFailed }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SMS STATUS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(if (smsFailed) "FAILED (Tap to fix)" else "DELIVERED (Tap to fail)", color = if (smsFailed) CoralRed else MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // WhatsApp channel status
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (whatsappFailed) CoralRed.copy(alpha = 0.15f) else Color(0xFF25D366).copy(alpha = 0.15f))
                                .border(1.dp, if (whatsappFailed) CoralRed else Color(0xFF25D366), RoundedCornerShape(12.dp))
                                .clickable { whatsappFailed = !whatsappFailed }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WHATSAPP STATUS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(if (whatsappFailed) "FAILED (Tap to fix)" else "DELIVERED (Tap to fail)", color = if (whatsappFailed) CoralRed else Color(0xFF25D366), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                // OTP 6-Digit Row Input
                TextField(
                    value = otpCode,
                    onValueChange = { code ->
                        if (code.length <= 6 && code.all { it.isDigit() }) {
                            viewModel.setOtpCode(code)
                            if (code.length == 6) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("otp_input"),
                    placeholder = {
                        Text(
                            "Enter 6-digit code",
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedIndicatorColor = if (otpError != null) CoralRed else MintGreen,
                        unfocusedIndicatorColor = if (otpError != null) CoralRed else BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error Indicator
                AnimatedVisibility(visible = otpError != null) {
                    otpError?.let { err ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CoralRed.copy(alpha = 0.1f))
                                .border(1.dp, CoralRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = CoralRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                color = CoralRed,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer & Resend Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (timer > 0) {
                        Text(
                            text = "Resend code in ${timer}s",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        TextButton(
                            onClick = { viewModel.startOtpTimer() },
                            colors = ButtonDefaults.textButtonColors(contentColor = ElectricBlue)
                        ) {
                            Text(
                                text = "Resend OTP Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Demo Code hint
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DEMO PIN: 123456 or 422346",
                            color = ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Verify Button
            Button(
                onClick = { viewModel.verifyOtp() },
                enabled = otpCode.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen,
                    contentColor = DarkBg,
                    disabledContainerColor = DarkSurfaceElevated,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp)
                    .testTag("otp_verify_button")
            ) {
                Text(
                    text = "Verify & Access",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Heads-up Notifications Column (SMS & WhatsApp overlay) (US-005)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // SMS Notification Card
            AnimatedVisibility(
                visible = showSmsNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                SwipeDismissableCard(
                    visible = showSmsNotification,
                    onDismiss = { showSmsNotification = false }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setOtpCode("422346")
                                showSmsNotification = false
                                viewModel.verifyOtp()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MintGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "Simulated SMS",
                                    tint = MintGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MESSAGES • NOW",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close SMS Simulation",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { showSmsNotification = false }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "<FinTrack> Use verification PIN 422346 to securely access your account dashboard.",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚡ TAP TO AUTO-FILL & VERIFY (SMS)",
                                    color = MintGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // WhatsApp Notification Card
            AnimatedVisibility(
                visible = showWhatsappNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                SwipeDismissableCard(
                    visible = showWhatsappNotification,
                    onDismiss = { showWhatsappNotification = false }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setOtpCode("422346")
                                showWhatsappNotification = false
                                viewModel.verifyOtp()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "Simulated WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WHATSAPP • NOW",
                                        color = Color(0xFF25D366),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close WA Simulation",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { showWhatsappNotification = false }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "<FinTrack> Use verification PIN 422346 to securely access your account dashboard.",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚡ TAP TO AUTO-FILL & VERIFY (WHATSAPP)",
                                    color = Color(0xFF25D366),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
// SCREEN 4: LINK ACCOUNT PROMPT
// ==========================================
@Composable
fun LinkAccountPromptScreen(
    onLinkAccount: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(48.dp))
            // Security Lock Icon Animation
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MintGreen.copy(alpha = 0.1f))
                    .border(1.dp, MintGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield Lock",
                    tint = MintGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Connect financial accounts",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Link your accounts to automatically consolidate UPI transactions, monitor real-time monthly balances, and unlock smart spending categorization reports.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Checkpoints of value prop
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinkValueRow(
                    icon = Icons.Default.DoneAll,
                    title = "Automatic expense detection",
                    desc = "No manual logs. Read automated banking notifications instantly."
                )
                LinkValueRow(
                    icon = Icons.Default.BarChart,
                    title = "Interactive cashflow diagrams",
                    desc = "Understand exactly where money goes with beautiful visual rings."
                )
                LinkValueRow(
                    icon = Icons.Default.Lock,
                    title = "100% Read-only and encrypted",
                    desc = "Your money is safe. Read-only permissions mean zero transfer capabilities."
                )
            }
        }

        // CTA Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onLinkAccount,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("link_account_btn")
            ) {
                Text(
                    text = "Link Bank Account",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("skip_linking_btn")
            ) {
                Text(
                    text = "Skip for now (Show empty state)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LinkValueRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ElectricBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Bullet Icon",
                tint = ElectricBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

// ==========================================
// SCREEN 5: SELECT PROVIDER
// ==========================================
@Composable
fun SelectProviderScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onSelectBank: (String) -> Unit
) {
    val searchQuery by viewModel.bankSearchQuery.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val linkedBankIds = remember(bankAccounts) { bankAccounts.filter { it.isLinked }.map { it.id } }

    var showAllBanksSearch by remember { mutableStateOf(false) }

    val filteredBanks = remember(searchQuery, showAllBanksSearch) {
        if (!showAllBanksSearch) {
            viewModel.availableBanks
        } else {
            if (searchQuery.isEmpty()) {
                viewModel.allIndianBanks
            } else {
                viewModel.allIndianBanks.filter {
                    it.second.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (showAllBanksSearch) {
                        showAllBanksSearch = false
                        viewModel.setBankSearchQuery("")
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showAllBanksSearch) "Search Other Provider" else "Link Account Provider",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            // Bank Search Bar (Only visible when user clicked Connect Other Provider)
            if (showAllBanksSearch) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setBankSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search 200+ Indian banks...",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setBankSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedIndicatorColor = ElectricBlue,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("bank_search_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showAllBanksSearch) "ALL INDIAN BANKS & PROVIDERS" else "POPULAR BANKS IN INDIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!showAllBanksSearch) {
                    // Display popular banks
                    items(filteredBanks) { item ->
                        val bank = item as BankAccount
                        val isAlreadyLinked = linkedBankIds.contains(bank.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .border(
                                    1.dp,
                                    if (isAlreadyLinked) MintGreen.copy(alpha = 0.5f) else BorderColor,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = !isAlreadyLinked) {
                                    onSelectBank(bank.id)
                                }
                                .padding(16.dp)
                                .testTag("bank_item_${bank.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when (bank.id) {
                                                "hdfc" -> Color(0xFF003366)
                                                "icici" -> Color(0xFFF28500)
                                                "sbi" -> Color(0xFF00A8E1)
                                                else -> ElectricBlue.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bank.name.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = bank.name,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Automated transaction parsing logs",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isAlreadyLinked) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MintGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Linked",
                                            tint = MintGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Linked",
                                            color = MintGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Go Link",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    // "Connect Other Provider" Button Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ElectricBlue.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    ElectricBlue.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    showAllBanksSearch = true
                                }
                                .padding(16.dp)
                                .testTag("connect_other_provider_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElectricBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Other",
                                        tint = DarkBg,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Connect Other Provider",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Search 200+ other Indian banks",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Search",
                                tint = ElectricBlue
                            )
                        }
                    }
                } else {
                    // Display all Indian banks (200+ list)
                    items(filteredBanks) { item ->
                        val pair = item as Pair<*, *>
                        val bankId = pair.first as String
                        val bankName = pair.second as String
                        val isAlreadyLinked = linkedBankIds.contains(bankId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .border(
                                    1.dp,
                                    if (isAlreadyLinked) MintGreen.copy(alpha = 0.5f) else BorderColor,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = !isAlreadyLinked) {
                                    onSelectBank(bankId)
                                }
                                .padding(16.dp)
                                .testTag("bank_item_$bankId"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElectricBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bankName.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = bankName,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Instant automated bank connector",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isAlreadyLinked) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MintGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Linked",
                                            tint = MintGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Linked",
                                            color = MintGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Go Link",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 6: CONSENT AND PERMISSIONS
// ==========================================
@Composable
fun ConsentScreen(
    bankId: String,
    onBack: () -> Unit,
    onGrant: () -> Unit
) {
    val bankName = when (bankId) {
        "hdfc" -> "HDFC Bank"
        "icici" -> "ICICI Bank"
        "sbi" -> "State Bank of India"
        else -> "GPay / UPI Wallet"
    }

    Scaffold(
        topBar = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Consent Agreement",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "By granting consent, you authorize FinTrack to synchronize and categorize data securely from your $bankName transactions.",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "REQUESTED DATA PERMISSIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Permissions Card details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PermissionRow(
                        title = "Read Transaction History",
                        desc = "Parses merchant logs, amount details, and dates to build spending summaries."
                    )
                    PermissionRow(
                        title = "Verify Monthly Ending Balances",
                        desc = "Enables total net worth tracking and displays running bank balances."
                    )
                    PermissionRow(
                        title = "No Debit Permissions (100% Secure)",
                        desc = "FinTrack is strictly read-only. It has zero access to transfer funds, initiate payments, or touch your balances."
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Security Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlue.copy(alpha = 0.08f))
                        .border(1.dp, ElectricBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your data consent will expire automatically in 12 months. You can instantly unlink or wipe your complete transaction logs at any time in Preferences.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Grant Button
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp)
                    .testTag("grant_consent_button")
            ) {
                Text(
                    text = "Grant Secure Access",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PermissionRow(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Permitted",
            tint = MintGreen,
            modifier = Modifier
                .size(18.dp)
                .offset(y = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ==========================================
// SCREEN 7: LINKING PROGRESS
// ==========================================
@Composable
fun LinkingProgressScreen(
    viewModel: FinanceViewModel,
    bankId: String
) {
    val bankName = when (bankId) {
        "hdfc" -> "HDFC Bank"
        "icici" -> "ICICI Bank"
        "sbi" -> "State Bank of India"
        else -> "GPay / UPI Wallet"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotate")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinnerAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Rotating Loading Ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .drawBehind {
                    drawArc(
                        color = BorderColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ElectricBlue,
                        startAngle = angle,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = "Syncing",
                tint = ElectricBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Connecting $bankName...",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Please wait while FinTrack retrieves and parses encrypted transaction reports. This usually takes around 3 seconds.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ==========================================
// SCREEN 8: DASHBOARD (MAIN HUB)
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTxDetail: (Int) -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToLinkAccount: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    val isWeeklySummaryDialogShown by viewModel.isWeeklySummaryDialogShown.collectAsState()
    val isExportStatementDialogShown by viewModel.isExportStatementDialogShown.collectAsState()
    val isReportSMSDialogShown by viewModel.isReportSMSDialogShown.collectAsState()
    val isHelpFAQDialogShown by viewModel.isHelpFAQDialogShown.collectAsState()

    val isSMSPermissionDialogShown by viewModel.isSMSPermissionDialogShown.collectAsState()
    val isAnalyzingSMS by viewModel.isAnalyzingSMS.collectAsState()
    val isBackupDialogShown by viewModel.isBackupDialogShown.collectAsState()
    val isExpenseTrackingEnabled by viewModel.isExpenseTrackingEnabled.collectAsState()

    val linkedAccounts = remember(bankAccounts) { bankAccounts.filter { it.isLinked } }
    val totalBalance = remember(linkedAccounts) { linkedAccounts.sumOf { it.balance } }

    val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }

    val income = remember(transactions) { transactions.filter { it.type == "INCOME" } }
    val totalIncome = remember(income) { income.sumOf { it.amount } }

    var isExpensesBreakdownDialogShown by remember { mutableStateOf(false) }
    var isIncomeBreakdownDialogShown by remember { mutableStateOf(false) }
    var isAccountsDetailDialogShown by remember { mutableStateOf(false) }
    var selectedCategoryForTransactionsDialog by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(300.dp).fillMaxHeight()
            ) {
                val isWhatsAppChatbotActive by viewModel.isWhatsAppChatbotActive.collectAsState()
                if (isWhatsAppChatbotActive) {
                    WhatsAppChatbotDrawerContent(viewModel)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile summary in left drawer (FEAT-017)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "JD",
                                    color = Color(0xFF381E72),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = viewModel.userFullName.collectAsState().value,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+91 " + viewModel.phoneNumber.collectAsState().value.ifEmpty { "9876543210" },
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

                        // Expense Ingestion Tracking controls (FEAT-018, FEAT-042)
                        Text(
                            text = "EXPENSE TRACKING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        val drawerItemModifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.PieChart, contentDescription = "Weekly", tint = ElectricBlue) },
                            label = { Text("Weekly Summary", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.isWeeklySummaryDialogShown.value = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_weekly_summary")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Export", tint = MintGreen) },
                            label = { Text("Export Statement", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.isExportStatementDialogShown.value = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_export_statement")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Sms, contentDescription = "Missed", tint = VividGold) },
                            label = { Text("Report Undetected SMS", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.isReportSMSDialogShown.value = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_report_sms")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help", tint = CoralRed) },
                            label = { Text("Help / FAQ", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.isHelpFAQDialogShown.value = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_help_faq")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Preferences", tint = TextSecondary) },
                            label = { Text("Preferences", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToPreferences()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_preferences")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Share, contentDescription = "Invite Friends", tint = ElectricBlue) },
                            label = { Text("Invite Friends & Family", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                val inviteMessage = "Track your finances securely with FinTrack! Download the app today: https://fintrack.example.com/download"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share app link via"))
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_invite_friends")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Chat, contentDescription = "WhatsApp Bot", tint = Color(0xFF25D366)) },
                            label = { Text("WhatsApp Chatbot 💬", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                viewModel.isWhatsAppChatbotActive.value = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_whatsapp_chatbot")
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

                        Text(
                            text = "GENERAL OPTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Link, contentDescription = "Linked Accounts", tint = TextSecondary) },
                            label = { Text("Linked Bank Accounts", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToLinkAccount()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_linked_banks")
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = CoralRed) },
                            label = { Text("Sign Out", color = CoralRed, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    viewModel.resetAllForDemo()
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = ElectricBlue.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = drawerItemModifier.testTag("drawer_sign_out")
                        )
                    }
            } // Close the else block for standard drawer menu content
        }
    }
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left rounded symbol navigation (FEAT-015) opens associated menu/panel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlue.copy(alpha = 0.2f))
                                .clickable { scope.launch { drawerState.open() } }
                                .testTag("left_rounded_menu_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Logo menu button",
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FinTrack",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Header Profile & Settings Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.forceSync() },
                            modifier = Modifier.testTag("dashboard_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Now",
                                tint = ElectricBlue
                            )
                        }
                        IconButton(
                            onClick = onNavigateToPreferences,
                            modifier = Modifier.testTag("dashboard_preferences_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings Preferences",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            },
            containerColor = DarkBg
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (linkedAccounts.isEmpty()) {
                        DashboardEmptyState(onLinkAccount = onNavigateToLinkAccount)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        syncError?.let { err ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, CoralRed),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = "Sync Error",
                                                tint = CoralRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = err,
                                                color = CoralRed,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.clearSyncError() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = CoralRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Enable Expense Ingestion Card (FEAT-036)
                        if (!isExpenseTrackingEnabled) {
                            item {
                                ExpenseTrackingSetupCard(viewModel = viewModel)
                            }
                        }

                        // Balance Card summary with dynamic layout
                        item {
                            BalanceSummaryCard(
                                totalBalance = totalBalance,
                                income = totalIncome,
                                expenses = totalExpenses,
                                linkedAccounts = linkedAccounts,
                                onBalanceClick = { isAccountsDetailDialogShown = true },
                                onIncomeClick = { isIncomeBreakdownDialogShown = true },
                                onExpensesClick = { isExpensesBreakdownDialogShown = true }
                            )
                        }

                        // Spend Ring & Breakdown Chart
                        item {
                            CategoryBreakdownChart(
                                expenses = expenses,
                                onCategoryClick = { category ->
                                    selectedCategoryForTransactionsDialog = category
                                },
                                onTotalOutClick = {
                                    isExpensesBreakdownDialogShown = true
                                }
                            )
                        }

                        // Interactive Insights Section
                        item {
                            DashboardInsightsSection(expenses = expenses, income = income, viewModel = viewModel)
                        }

                        // Recent transactions list header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent UPI & Bank Logs",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${transactions.size} records",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Recent transactions list
                        items(transactions) { tx ->
                            TransactionRowItem(
                                transaction = tx,
                                onClick = { onNavigateToTxDetail(tx.id) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                // Sync Loader Overlay
                if (isSyncing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBg.copy(alpha = 0.8f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MintGreen)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Syncing live accounts...",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

    // --- DIALOGS FOR SETUP FLOW (FEAT-038, FEAT-039, FEAT-040, FEAT-041) ---

    // SMS Permission Dialog
    if (isSMSPermissionDialogShown) {
        AlertDialog(
            onDismissRequest = { viewModel.isSMSPermissionDialogShown.value = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("SMS Permission Request", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FinTrack automatically categorizes spends from SMS. To proceed, allow reading permission.", color = TextSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Secure", tint = MintGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("We process all bank alerts locally. No private messages or logins are read.", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.isSMSPermissionDialogShown.value = false
                        viewModel.isAnalyzingSMS.value = true
                        scope.launch {
                            delay(2000)
                            viewModel.isAnalyzingSMS.value = false
                            viewModel.isBackupDialogShown.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F3DEB), contentColor = Color.White),
                    modifier = Modifier.testTag("dialog_grant_sms_btn")
                ) {
                    Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.isSMSPermissionDialogShown.value = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Analyzing SMS Overlay
    if (isAnalyzingSMS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg.copy(alpha = 0.85f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MintGreen)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Analyzing service SMS...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ingesting transaction entries and establishing categories", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }

    // Backup Found Dialog
    if (isBackupDialogShown) {
        AlertDialog(
            onDismissRequest = { viewModel.isBackupDialogShown.value = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Backup Found", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "We found a secure cloud backup containing 7 past transactions, custom rules, and categorized tags from June 2026.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreTransactionsFromBackup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = DarkBg),
                    modifier = Modifier.testTag("dialog_restore_backup_setup_btn")
                ) {
                    Text("Restore Past Spends", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.isBackupDialogShown.value = false
                        viewModel.isExpenseTrackingEnabled.value = true
                        viewModel.showFeedback("Real-time tracking activated! 0 transactions found.")
                    },
                    modifier = Modifier.testTag("dialog_skip_backup_btn")
                ) {
                    Text("Skip & Start Fresh", color = TextSecondary)
                }
            }
        )
    }

    // --- DRAWERS DIALOGS (FEAT-043, FEAT-044, FEAT-045, FEAT-046) ---

    // Weekly Summary Dialog
    if (isWeeklySummaryDialogShown) {
        AlertDialog(
            onDismissRequest = { viewModel.isWeeklySummaryDialogShown.value = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = "Weekly Summary", tint = ElectricBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Weekly Expense Summary", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Categorized outflows for the week ending June 2026:", color = TextSecondary, fontSize = 13.sp)
                    
                    val weeklyCategories = listOf(
                        Triple("Food & Swiggy", 1790.0, MintGreen),
                        Triple("Transport & Uber", 120.0, ElectricBlue),
                        Triple("ACT Fibernet Bills", 1050.0, VividGold),
                        Triple("Zara Shopping", 3400.0, Color(0xFFAB47BC)),
                        Triple("Netflix Subscriptions", 649.0, CoralRed)
                    )
                    
                    val totalWeekly = weeklyCategories.sumOf { it.second }
                    
                    Text("Total Weekly Outflow: ₹${NumberFormat.getNumberInstance(Locale.US).format(totalWeekly)}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    
                    weeklyCategories.forEach { (catName, amt, col) ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(catName, color = TextPrimary, fontSize = 12.sp)
                                }
                                Text("₹${amt.toInt()}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = (amt / totalWeekly).toFloat()
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = col,
                                trackColor = BorderColor
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.isWeeklySummaryDialogShown.value = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Export Statement Dialog
    if (isExportStatementDialogShown) {
        var selectedFormat by remember { mutableStateOf("PDF") }
        var selectedPeriod by remember { mutableStateOf("Last 30 Days") }
        var isExporting by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!isExporting) viewModel.isExportStatementDialogShown.value = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Export Statement", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MintGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating statement...", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Compressing files and encrypting download", color = TextSecondary, fontSize = 11.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Select report parameters to export your categorized transaction data.", color = TextSecondary, fontSize = 12.sp)
                        
                        Column {
                            Text("TIME PERIOD", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Last 7 Days", "Last 30 Days", "Custom").forEach { period ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedPeriod == period) ElectricBlue.copy(alpha = 0.2f) else DarkBg)
                                            .border(1.dp, if (selectedPeriod == period) ElectricBlue else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedPeriod = period }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(period, color = if (selectedPeriod == period) ElectricBlue else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Column {
                            Text("EXPORT FORMAT", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("PDF", "Excel", "CSV").forEach { format ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedFormat == format) MintGreen.copy(alpha = 0.2f) else DarkBg)
                                            .border(1.dp, if (selectedFormat == format) MintGreen else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedFormat = format }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(format, color = if (selectedFormat == format) MintGreen else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isExporting) {
                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                delay(1200)
                                isExporting = false
                                viewModel.isExportStatementDialogShown.value = false
                                viewModel.showFeedback("Statement exported successfully in $selectedFormat format!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = DarkBg),
                        modifier = Modifier.testTag("dialog_export_btn")
                    ) {
                        Text("Export Now", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isExporting) {
                    TextButton(onClick = { viewModel.isExportStatementDialogShown.value = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }

    // Report Undetected SMS Dialog
    if (isReportSMSDialogShown) {
        var smsText by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { viewModel.isReportSMSDialogShown.value = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Report Undetected SMS", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("If FinTrack missed a banking or UPI transaction alert, paste the SMS content below so we can integrate its pattern.", color = TextSecondary, fontSize = 12.sp)
                    TextField(
                        value = smsText,
                        onValueChange = { smsText = it },
                        placeholder = { Text("Paste transaction SMS alert here...", color = TextMuted, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = ElectricBlue,
                            unfocusedIndicatorColor = BorderColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("missed_sms_input"),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.isReportSMSDialogShown.value = false
                        viewModel.showFeedback("Pattern submitted! SMS added to queue for classification.")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7F3DEB),
                        contentColor = Color.White,
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    enabled = smsText.isNotBlank(),
                    modifier = Modifier.testTag("dialog_submit_report_sms_btn")
                ) {
                    Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.isReportSMSDialogShown.value = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Help / FAQ Dialog
    if (isHelpFAQDialogShown) {
        AlertDialog(
            onDismissRequest = { viewModel.isHelpFAQDialogShown.value = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Help & FAQ", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    val faqs = listOf(
                        "How does automatic expense tracking work?" to "FinTrack uses client-side regex algorithms to safely scan your financial notifications & bank SMS headers to record expenses instantly.",
                        "Are my credit card passwords stored?" to "Absolutely not. We never ask for or store passwords, PINs, or credentials. FinTrack is strictly read-only and local.",
                        "What happens to my cloud backups?" to "Backups are stored in your private, secure cloud account, fully encrypted. Only you can restore or wipe them.",
                        "Why does the app need SMS permission?" to "SMS permission allows real-time ingestion of transaction receipts so you never have to enter spends manually."
                    )
                    
                    faqs.forEach { (q, a) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .padding(12.dp)
                        ) {
                            Text(q, color = MintGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(a, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.isHelpFAQDialogShown.value = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }

    // 1. Category-wise Expense Breakdown Dialog
    if (isExpensesBreakdownDialogShown) {
        val expenseSums = remember(expenses) {
            val sums = mutableMapOf<String, Double>()
            expenses.forEach { tx ->
                sums[tx.category] = (sums[tx.category] ?: 0.0) + tx.amount
            }
            sums.toList().sortedByDescending { it.second }
        }
        AlertDialog(
            onDismissRequest = { isExpensesBreakdownDialogShown = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Expenses by Category", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Total spending: ₹${totalExpenses.toInt()}", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (expenseSums.isEmpty()) {
                        Text("No recorded expenses yet.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        expenseSums.forEach { (cat, amt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBg)
                                    .clickable {
                                        isExpensesBreakdownDialogShown = false
                                        selectedCategoryForTransactionsDialog = cat
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(cat))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.uppercase(),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${amt.toInt()}",
                                        color = CoralRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                        contentDescription = "View details",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isExpensesBreakdownDialogShown = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }

    // 2. Category-wise Income Breakdown Dialog
    if (isIncomeBreakdownDialogShown) {
        val incomeSums = remember(income) {
            val sums = mutableMapOf<String, Double>()
            income.forEach { tx ->
                sums[tx.category] = (sums[tx.category] ?: 0.0) + tx.amount
            }
            sums.toList().sortedByDescending { it.second }
        }
        AlertDialog(
            onDismissRequest = { isIncomeBreakdownDialogShown = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Income by Category", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Total income received: ₹${totalIncome.toInt()}", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (incomeSums.isEmpty()) {
                        Text("No recorded income yet.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        incomeSums.forEach { (cat, amt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBg)
                                    .clickable {
                                        isIncomeBreakdownDialogShown = false
                                        selectedCategoryForTransactionsDialog = cat
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.uppercase(),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${amt.toInt()}",
                                        color = MintGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                        contentDescription = "View details",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isIncomeBreakdownDialogShown = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }

    // 3. Bank Accounts Detail and Live Status Dialog
    if (isAccountsDetailDialogShown) {
        AlertDialog(
            onDismissRequest = { isAccountsDetailDialogShown = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Linked Accounts Status", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Securely linked client-side bank accounts:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (linkedAccounts.isEmpty()) {
                        Text("No active accounts linked.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        linkedAccounts.forEach { acc ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBg)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = acc.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MintGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SYNCED",
                                            color = MintGreen,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Account: ****${acc.accountNo.takeLast(4)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Balance",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "₹${acc.balance.toInt()}",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isAccountsDetailDialogShown = false
                            viewModel.forceSync()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F3DEB), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Force Sync Accounts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAccountsDetailDialogShown = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }

    // 4. Category-specific Transactions Dialog
    if (selectedCategoryForTransactionsDialog != null) {
        val category = selectedCategoryForTransactionsDialog!!
        val filteredTxs = remember(transactions, category) {
            transactions.filter { it.category == category }
        }
        AlertDialog(
            onDismissRequest = { selectedCategoryForTransactionsDialog = null },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "${category.lowercase().replaceFirstChar { it.uppercase() }} Logs",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (filteredTxs.isEmpty()) {
                        Text("No logs in this category.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        filteredTxs.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBg)
                                    .clickable {
                                        selectedCategoryForTransactionsDialog = null
                                        onNavigateToTxDetail(tx.id)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.merchantName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val dateStr = remember(tx.timestamp) {
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(tx.timestamp))
                                    }
                                    Text(
                                        text = dateStr,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "${if (tx.type == "EXPENSE") "-" else "+"}₹${tx.amount.toInt()}",
                                    color = if (tx.type == "EXPENSE") CoralRed else MintGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCategoryForTransactionsDialog = null }) {
                    Text("Dismiss", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ExpenseTrackingSetupCard(viewModel: FinanceViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("expense_tracking_setup_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS Tracking",
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enable Auto Expense",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track bank alerts & UPI spends from SMS automatically",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { viewModel.isSMSPermissionDialogShown.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F3DEB), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("enable_expense_tracking_btn")
            ) {
                Text("Enable Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ==========================================
// SUB-COMPONENT: DASHBOARD EMPTY STATE
// ==========================================
@Composable
fun DashboardEmptyState(
    onLinkAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BorderColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "No Linked Accounts",
                tint = TextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No linked transaction accounts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Link your secure bank accounts or UPI wallets to let FinTrack monitor transactions, organize spending categories, and display cashflow reports.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLinkAccount,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F3DEB), contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("empty_state_link_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Link Your First Bank", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// SUB-COMPONENT: BALANCE SUMMARY CARD
// ==========================================
@Composable
fun BalanceSummaryCard(
    totalBalance: Double,
    income: Double,
    expenses: Double,
    linkedAccounts: List<BankAccount>,
    onBalanceClick: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpensesClick: () -> Unit
) {
    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4F378B)),
        border = BorderStroke(1.dp, Color(0xFFEADDFF).copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Clickable Balance Title and Amount
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBalanceClick() }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "TOTAL COMBINED BALANCE",
                    color = Color(0xFFEADDFF).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Balance Amount
                Text(
                    text = rupeeFormat.format(totalBalance).replace("INR", "₹"),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFEADDFF).copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Row
            Row(modifier = Modifier.fillMaxWidth()) {
                // Clickable Income Col
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onIncomeClick() }
                        .padding(vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB4E197).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income",
                                tint = Color(0xFFB4E197),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Income", color = Color(0xFFEADDFF).copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rupeeFormat.format(income).replace("INR", "₹"),
                        color = Color(0xFFB4E197),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color(0xFFEADDFF).copy(alpha = 0.2f))
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(16.dp))

                // Clickable Expense Col
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExpensesClick() }
                        .padding(vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF2B8B5).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense",
                                tint = Color(0xFFF2B8B5),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Expenses", color = Color(0xFFEADDFF).copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rupeeFormat.format(expenses).replace("INR", "₹"),
                        color = Color(0xFFF2B8B5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFEADDFF).copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Linked details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Sources: ${linkedAccounts.joinToString { it.name }}",
                    fontSize = 11.sp,
                    color = Color(0xFFEADDFF).copy(alpha = 0.7f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB4E197))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted",
                        fontSize = 11.sp,
                        color = Color(0xFFB4E197),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENT: CATEGORY SPENDING DONUT CHART
// ==========================================
@Composable
fun CategoryBreakdownChart(
    expenses: List<Transaction>,
    onCategoryClick: (String) -> Unit,
    onTotalOutClick: () -> Unit = {}
) {
    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    val totalCost = expenses.sumOf { it.amount }

    // Aggregate category expenses
    val categoryTotals = remember(expenses) {
        val sums = mutableMapOf<String, Double>()
        expenses.forEach { tx ->
            sums[tx.category] = (sums[tx.category] ?: 0.0) + tx.amount
        }
        sums.toList().sortedByDescending { it.second }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Expense Categories Distribution",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded expenses yet this period.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Donut Canvas Drawing
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .clickable { onTotalOutClick() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var currentStartAngle = -90f
                            categoryTotals.forEach { (cat, amt) ->
                                val pct = (amt / totalCost).toFloat()
                                val sweep = pct * 360f
                                drawArc(
                                    color = getCategoryColor(cat),
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                                currentStartAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Out",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rupeeFormat.format(totalCost).replace("INR", "₹"),
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Labels Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryTotals.take(4).forEach { (cat, amt) ->
                            val pct = (amt / totalCost * 100).toInt()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategoryClick(cat) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(cat))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat.lowercase()
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$pct%",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to resolve colors for categories
fun getCategoryColor(cat: String): Color {
    return when (cat.uppercase()) {
        "FOOD" -> MintGreen
        "TRANSPORT" -> ElectricBlue
        "BILLS" -> VividGold
        "SUBSCRIPTIONS" -> CoralRed
        "SHOPPING" -> Color(0xFFD500F9) // Purple/Magenta
        "INCOME" -> Color(0xFF00C853)
        "TRANSFERS" -> Color(0xFFECEFF1)
        else -> Color(0xFF90A4AE) // Slate gray for uncategorized
    }
}

// Helper to resolve icons for categories
fun getCategoryIcon(cat: String): ImageVector {
    return when (cat.uppercase()) {
        "FOOD" -> Icons.Default.Restaurant
        "TRANSPORT" -> Icons.Default.DirectionsCar
        "BILLS" -> Icons.Default.ReceiptLong
        "SUBSCRIPTIONS" -> Icons.Default.Star
        "SHOPPING" -> Icons.Default.ShoppingBag
        "INCOME" -> Icons.Default.AddCard
        "TRANSFERS" -> Icons.Default.CompareArrows
        else -> Icons.Default.HelpOutline // Help Outline for uncategorized
    }
}

// ==========================================
// SUB-COMPONENT: RECENT TRANSACTION ROW
// ==========================================
@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val dateString = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("tx_item_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = getCategoryColor(transaction.category),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = transaction.merchantName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category.lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        color = getCategoryColor(transaction.category),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateString,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Amount on the right side
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (transaction.type == "INCOME") "+ " else "- ") +
                        rupeeFormat.format(transaction.amount).replace("INR", "₹"),
                color = if (transaction.type == "INCOME") MintGreen else TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            // User corrected tag
            if (transaction.isUserCorrected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ElectricBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MANUAL",
                        color = ElectricBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENT: DASHBOARD INSIGHTS SECTION
// ==========================================
@Composable
fun DashboardInsightsSection(
    expenses: List<Transaction>,
    income: List<Transaction>,
    viewModel: FinanceViewModel
) {
    val aiInsights by viewModel.aiInsights.collectAsState()
    val isGenerating by viewModel.isGeneratingInsights.collectAsState()

    // Auto-generate on first-time display if empty
    LaunchedEffect(Unit) {
        if (aiInsights.isEmpty()) {
            viewModel.generateRAGInsights()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Automated AI Cash Insights",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "RAG MODEL",
                            color = ElectricBlue,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Powered by Gemini 3.5",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = { viewModel.generateRAGInsights() },
                enabled = !isGenerating,
                modifier = Modifier.size(36.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ElectricBlue
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate insights with RAG",
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isGenerating && aiInsights.isEmpty()) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated.copy(alpha = 0.5f))
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Retrieving bank data & generating smart insights...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            aiInsights.forEach { insight ->
                val icon = when (insight.iconType.uppercase()) {
                    "FOOD" -> Icons.Default.Restaurant
                    "BILLS" -> Icons.Default.ReceiptLong
                    "SUBSCRIPTIONS" -> Icons.Default.Star
                    "SAVINGS" -> Icons.Default.Savings
                    else -> Icons.Default.Warning
                }
                val tint = when (insight.iconType.uppercase()) {
                    "FOOD" -> MintGreen
                    "BILLS" -> VividGold
                    "SUBSCRIPTIONS" -> CoralRed
                    "SAVINGS" -> MintGreen
                    else -> CoralRed
                }
                
                InsightItemRow(
                    icon = icon,
                    iconTint = tint,
                    title = insight.title,
                    desc = insight.desc,
                    severity = insight.severity
                )
            }
        }
    }
}

@Composable
fun InsightItemRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String,
    severity: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Insight icon",
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sevBg = when (severity.uppercase()) {
                    "HIGH" -> CoralRed.copy(alpha = 0.15f)
                    "MEDIUM" -> VividGold.copy(alpha = 0.15f)
                    else -> MintGreen.copy(alpha = 0.15f)
                }
                val sevText = when (severity.uppercase()) {
                    "HIGH" -> CoralRed
                    "MEDIUM" -> VividGold
                    else -> MintGreen
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(sevBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = severity,
                        color = sevText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ==========================================
// SCREEN 9: TRANSACTION DETAIL
// ==========================================
@Composable
fun TransactionDetailScreen(
    transactionId: Int,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onEditCategory: (Int) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val tx = transactions.find { it.id == transactionId }

    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transaction Detail",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        if (tx == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Transaction record not found", color = TextSecondary)
            }
        } else {
            val dateString = remember(tx.timestamp) {
                val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                sdf.format(Date(tx.timestamp))
            }
            val bankName = remember(tx.accountSource) {
                when (tx.accountSource) {
                    "hdfc" -> "HDFC Bank (Debited)"
                    "icici" -> "ICICI Bank (Debited)"
                    "sbi" -> "State Bank of India (Debited)"
                    else -> "GPay / UPI Wallet"
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Floating animated big category icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(tx.category).copy(alpha = 0.15f))
                            .border(1.dp, getCategoryColor(tx.category), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(tx.category),
                            contentDescription = tx.category,
                            tint = getCategoryColor(tx.category),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = tx.merchantName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (if (tx.type == "INCOME") "+ " else "- ") +
                                rupeeFormat.format(tx.amount).replace("INR", "₹"),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = if (tx.type == "INCOME") MintGreen else TextPrimary
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Detail Fields Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailRow(label = "Category Label", value = tx.category.lowercase().replaceFirstChar { it.titlecase() }, valueColor = getCategoryColor(tx.category))
                        DetailRow(label = "Date & Hour", value = dateString)
                        DetailRow(label = "Linked Source", value = bankName)
                        DetailRow(label = "Verification Status", value = if (tx.isUserCorrected) "Corrected Manually" else "Automatically Parsed", valueColor = if (tx.isUserCorrected) ElectricBlue else MintGreen)
                    }
                }

                // Edit Category CTA
                Button(
                    onClick = { onEditCategory(tx.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F3DEB), contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp)
                        .testTag("edit_category_detail_btn")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Modify Category Label", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// SCREEN 10: CATEGORY SELECTOR
// ==========================================
@Composable
fun CategorySelectorScreen(
    transactionId: Int,
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val tx = transactions.find { it.id == transactionId }

    var applyToFutureRules by remember { mutableStateOf(true) }

    val categories = listOf(
        "FOOD", "TRANSPORT", "BILLS", "SUBSCRIPTIONS", "SHOPPING", "INCOME", "TRANSFERS", "UNCATEGORIZED"
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assign Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        if (tx == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Transaction record not found", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Modify Category for",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = tx.merchantName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Grid-like list of category blocks
                    Text(
                        text = "SELECT A CATEGORY LABEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { cat ->
                            val isSelected = tx.category == cat
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) getCategoryColor(cat) else BorderColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.updateTransactionCategory(tx.id, cat, applyToFutureRules)
                                        onBack() // Go back with update confirmed
                                    }
                                    .padding(14.dp)
                                    .testTag("select_cat_$cat"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(cat).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getCategoryIcon(cat),
                                            contentDescription = cat,
                                            tint = getCategoryColor(cat),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = cat.lowercase()
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = getCategoryColor(cat)
                                    )
                                }
                            }
                        }
                    }
                }

                // Rule checkbox at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .clickable { applyToFutureRules = !applyToFutureRules }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToFutureRules,
                        onCheckedChange = { applyToFutureRules = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply to similar future transactions",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Save custom rule to auto-categorize ${tx.merchantName} henceforth.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 11: PREFERENCES HOME
// ==========================================
@Composable
fun PreferencesHomeScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLinkedAccounts: () -> Unit,
    onNavigateToManageAccounts: () -> Unit,
    onNavigateToSpendCategories: () -> Unit,
    onNavigateToCreditCategories: () -> Unit,
    onNavigateToTagsManagement: () -> Unit
) {
    val launchInMoneyManager by viewModel.launchInMoneyManager.collectAsState()
    val manageRemindersEnabled by viewModel.manageRemindersEnabled.collectAsState()
    val isRescanningSMS by viewModel.isRescanningSMS.collectAsState()
    val context = LocalContext.current
    
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Preferences",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Profile card summary with Name, Mobile, and Email addition option (FEAT-017, FEAT-020)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MintGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Photo",
                                tint = MintGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.userFullName.collectAsState().value.ifEmpty { "John Doe" },
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+91 " + viewModel.phoneNumber.collectAsState().value.ifEmpty { "9876543210" },
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val isEmailAdded by viewModel.isEmailAdded.collectAsState()
                            val linkedEmail by viewModel.linkedEmail.collectAsState()
                            val isScanningEmail by viewModel.isScanningEmail.collectAsState()
                            if (isEmailAdded) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email added",
                                                tint = ElectricBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = linkedEmail,
                                                color = ElectricBlue,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.unlinkEmailAccount()
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                                        ) {
                                            Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (isScanningEmail) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Text("Scanning email for financial statements...", color = TextSecondary, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                color = ElectricBlue,
                                                trackColor = DarkBg,
                                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.emailChooserDialogShown.value = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue.copy(alpha = 0.15f), contentColor = ElectricBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).testTag("add_email_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add email ID",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Email ID for Tracking", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "SETTINGS AREAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // 1. Launch in money manager (FEAT-022)
                    PreferenceToggleItem(
                        icon = Icons.Default.TrendingUp,
                        iconColor = MintGreen,
                        title = "Launch in Money Manager",
                        desc = "Open directly in finance dashboards upon app launch",
                        checked = launchInMoneyManager,
                        onCheckedChange = { viewModel.launchInMoneyManager.value = it },
                        tag = "launch_money_manager"
                    )

                    // 2. Manage accounts (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.Visibility,
                        iconColor = Color(0xFF00E676),
                        title = "Manage Accounts Visibility",
                        desc = "Hide or show specific bank accounts on dashboard",
                        onClick = onNavigateToManageAccounts,
                        tag = "manage_accounts"
                    )

                    // 3. Link accounts (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.Link,
                        iconColor = ElectricBlue,
                        title = "Link Accounts",
                        desc = "Review active bank connectors and sync histories",
                        onClick = onNavigateToLinkedAccounts,
                        tag = "link_accounts"
                    )

                    // 4. Manage reminders (FEAT-022)
                    PreferenceToggleItem(
                        icon = Icons.Default.Notifications,
                        iconColor = VividGold,
                        title = "Manage Reminders",
                        desc = "Configure bill payments and dynamic alerts",
                        checked = manageRemindersEnabled,
                        onCheckedChange = { viewModel.manageRemindersEnabled.value = it },
                        tag = "manage_reminders"
                    )

                    // 5. Spend Categories (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.Category,
                        iconColor = MintGreen,
                        title = "Spend Categories",
                        desc = "Manage custom Merchant Categories & Override patterns",
                        onClick = onNavigateToSpendCategories,
                        tag = "categories"
                    )

                    // 6. Credit Categories (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.CreditCard,
                        iconColor = Color(0xFF29B6F6),
                        title = "Credit Categories",
                        desc = "Manage custom refund, salary and inflow categories",
                        onClick = onNavigateToCreditCategories,
                        tag = "credit_categories"
                    )

                    // 7. Tags (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.LocalOffer,
                        iconColor = Color(0xFFFF9100),
                        title = "Tags Management",
                        desc = "Review and edit custom transaction categorizing tags",
                        onClick = onNavigateToTagsManagement,
                        tag = "tags"
                    )

                    // 8. Notification & Reminders (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.Notifications,
                        iconColor = VividGold,
                        title = "Notification & Reminders Settings",
                        desc = "Manage unusual spends and weekly summaries controls",
                        onClick = onNavigateToNotifications,
                        tag = "notifications_reminders"
                    )

                    // 9. Backup (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.CloudUpload,
                        iconColor = Color(0xFFAB47BC),
                        title = "Backup & Restore",
                        desc = "Save or restore secure categories and historical logs",
                        onClick = { showBackupDialog = true },
                        tag = "backup"
                    )

                    // 10. Security (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.Security,
                        iconColor = CoralRed,
                        title = "Security & Encryption",
                        desc = "Review PIN protection and banking data security policies",
                        onClick = { showSecurityDialog = true },
                        tag = "security"
                    )

                    // 11. Rescan SMS inbox (FEAT-022)
                    PreferenceMenuItem(
                        icon = Icons.Default.Sms,
                        iconColor = ElectricBlue,
                        title = "Rescan SMS Inbox",
                        desc = "Analyze SMS alerts for missing transaction entries",
                        onClick = { viewModel.runRescanSMS() },
                        tag = "rescan_sms"
                    )

                    // 12. Invite Friends & Family (US-002)
                    PreferenceMenuItem(
                        icon = Icons.Default.Share,
                        iconColor = ElectricBlue,
                        title = "Invite Friends & Family",
                        desc = "Share FinTrack app link with friends and earn rewards",
                        onClick = {
                            val inviteMessage = "Track your finances securely with FinTrack! Download the app today: https://fintrack.example.com/download"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, inviteMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share app link via"))
                        },
                        tag = "invite_friends"
                    )
                }
            }

            // Email Chooser dialog (FEAT-020)
            val emailChooserDialogShown by viewModel.emailChooserDialogShown.collectAsState()
            if (emailChooserDialogShown) {
                var selectedEmail by remember { mutableStateOf<String?>(null) }
                AlertDialog(
                    onDismissRequest = { viewModel.emailChooserDialogShown.value = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text("Select Email Account", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select a device email account to sync bank Statements and UPI notification receipts.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            listOf("saipulipaka54@gmail.com", "john.doe@gmail.com", "personal.fintrack@yahoo.com").forEach { email ->
                                val isSelected = selectedEmail == email
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) ElectricBlue.copy(alpha = 0.15f) else DarkBg)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) ElectricBlue else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedEmail = email
                                        }
                                        .padding(14.dp).testTag("email_item_$email"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email icon",
                                            tint = if (isSelected) ElectricBlue else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = email,
                                            color = if (isSelected) ElectricBlue else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                selectedEmail?.let { email ->
                                    viewModel.linkedEmail.value = email
                                    viewModel.isEmailAdded.value = true
                                    viewModel.emailChooserDialogShown.value = false
                                    viewModel.scanEmailForTransactions(email)
                                }
                            },
                            enabled = selectedEmail != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.White,
                                disabledContainerColor = DarkSurfaceElevated,
                                disabledContentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("confirm_link_email_btn")
                        ) {
                            Text("Link Email", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.emailChooserDialogShown.value = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }

            // Backup & Restore Dialog (FEAT-022, FEAT-041)
            if (showBackupDialog) {
                AlertDialog(
                    onDismissRequest = { showBackupDialog = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text("Secure Cloud Backup", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Cloud backups preserve your custom categories, tag rules, and historic transaction digests safely.", color = TextSecondary, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBg)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Last Synchronized: Today, 04:30 AM", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Backups Found: 1 active cloud backup", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBackupDialog = false
                                viewModel.restoreTransactionsFromBackup()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = DarkBg),
                            modifier = Modifier.testTag("dialog_restore_backup_btn")
                        ) {
                            Text("Restore Backup", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBackupDialog = false }) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                )
            }

            // Security dialog (FEAT-022)
            if (showSecurityDialog) {
                AlertDialog(
                    onDismissRequest = { showSecurityDialog = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text("Bank-Grade Security", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("FinTrack keeps your financial credentials fully secure:", color = TextSecondary, fontSize = 13.sp)
                            
                            val items = listOf(
                                "Biometric fingerprint authorization lock",
                                "Zero remote storage of banking passwords",
                                "Strict client-side read-only SMS analysis"
                            )
                            items.forEach { bullet ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = "Shield", tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(bullet, color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showSecurityDialog = false }) {
                            Text("Done", color = MintGreen)
                        }
                    }
                )
            }

            // Rescan SMS progress overlay loader (FEAT-022)
            if (isRescanningSMS) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg.copy(alpha = 0.85f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MintGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Rescanning device SMS inbox...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reading bank spend alerts and UPI messages", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("pref_toggle_$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MintGreen,
                checkedTrackColor = MintGreen.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBg
            ),
            modifier = Modifier.testTag("switch_$tag")
        )
    }
}

@Composable
fun PreferenceMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    desc: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("pref_menu_$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Forward",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ==========================================
// SCREEN 12: NOTIFICATION SETTINGS
// ==========================================
@Composable
fun NotificationSettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val weeklySummaryToggle by viewModel.weeklySummaryToggle.collectAsState()
    val unusualSpendToggle by viewModel.unusualSpendToggle.collectAsState()
    val accountIssuesToggle by viewModel.accountIssuesToggle.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notification Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "RECEIVE REPORTS & REMINDERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                // Toggle Row 1
                NotificationToggleRow(
                    title = "Weekly Financial Summaries",
                    desc = "Get custom analytics on ending bank balances and cash outflows",
                    checked = weeklySummaryToggle,
                    onCheckedChange = { viewModel.weeklySummaryToggle.value = it }
                )

                // Toggle Row 2
                NotificationToggleRow(
                    title = "Unusual Spend Alerts",
                    desc = "Alert when transaction volumes exceed standard thresholds",
                    checked = unusualSpendToggle,
                    onCheckedChange = { viewModel.unusualSpendToggle.value = it }
                )

                // Toggle Row 3
                NotificationToggleRow(
                    title = "Account Connection Warnings",
                    desc = "Notifications if banking API permissions require manual renewal syncs",
                    checked = accountIssuesToggle,
                    onCheckedChange = { viewModel.accountIssuesToggle.value = it }
                )
            }

            // Confirmation banner at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintGreen.copy(alpha = 0.08f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Notification updates are synchronized instantly. Turn off cellular warnings anytime without modifying banking consents.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBg,
                checkedTrackColor = MintGreen,
                uncheckedTrackColor = BorderColor
            )
        )
    }
}

// ==========================================
// SCREEN 13: LINKED ACCOUNTS LIST & UNLINK
// ==========================================
@Composable
fun LinkedAccountsSettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onNavigateToLinkAccount: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val linkedAccounts = remember(bankAccounts) { bankAccounts.filter { it.isLinked } }

    var accountToUnlink by remember { mutableStateOf<BankAccount?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connected Banks",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ACTIVE SYNC PIPELINES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (linkedAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active bank accounts linked.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(linkedAccounts) { account ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (account.id) {
                                                        "hdfc" -> Color(0xFF003366)
                                                        "icici" -> Color(0xFFF28500)
                                                        "sbi" -> Color(0xFF00A8E1)
                                                        else -> ElectricBlue.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = account.name.take(2).uppercase(),
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = account.name,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = account.accountNo,
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    IconButton(onClick = { accountToUnlink = account }) {
                                        Icon(
                                            imageVector = Icons.Default.LinkOff,
                                            contentDescription = "Unlink",
                                            tint = CoralRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add another bank connector CTA
                Button(
                    onClick = onNavigateToLinkAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F3DEB), contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(bottom = 16.dp)
                        .testTag("preferences_add_bank_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Connect Another Provider", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Unlink Confirmation Dialog
            accountToUnlink?.let { account ->
                AlertDialog(
                    onDismissRequest = { accountToUnlink = null },
                    containerColor = DarkSurfaceElevated,
                    title = {
                        Text(
                            text = "Unlink ${account.name}?",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Unlinking this account will remove its connection and delete all ${account.name} transaction records from your dashboard. This cannot be undone.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.unlinkBank(account.id)
                                accountToUnlink = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                        ) {
                            Text(text = "Unlink and Delete", fontWeight = FontWeight.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { accountToUnlink = null },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(text = "Keep Connected")
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// NEW SCREEN: MANAGE ACCOUNTS (Visibility & Linking)
// ==========================================
@Composable
fun ManageAccountsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val bankAccounts by viewModel.bankAccounts.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage Accounts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BANK ACCOUNT TRACKING VISIBILITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Control which linked bank accounts are visible on your primary financial dashboard. Hidden accounts are preserved but excluded from total balance aggregation.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(bankAccounts) { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (account.id) {
                                        "hdfc" -> Color(0xFF003366)
                                        "icici" -> Color(0xFFF28500)
                                        "sbi" -> Color(0xFF00A8E1)
                                        else -> ElectricBlue.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account.name.take(2).uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!account.isLinked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Unlinked",
                                        color = CoralRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(CoralRed.copy(alpha = 0.1f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Balance: ₹${account.balance}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (account.isLinked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (account.isHidden) "Hidden" else "Visible",
                                color = if (account.isHidden) CoralRed else MintGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = !account.isHidden,
                                onCheckedChange = { checked ->
                                    viewModel.toggleAccountVisibility(account.id, !checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MintGreen,
                                    checkedTrackColor = MintGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBg
                                )
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.toggleAccountLinking(account.id, true) }
                        ) {
                            Text("Link", color = ElectricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NEW SCREEN: SPEND CATEGORIES MANAGEMENT
// ==========================================
@Composable
fun SpendCategoriesScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val customCategories by viewModel.customCategories.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF81C784)) }
    var selectedEmoji by remember { mutableStateOf("🛍️") }

    val preselectedColors = listOf(
        Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFB74D),
        Color(0xFFBA68C8), Color(0xFFE57373), Color(0xFF4DB6AC), Color(0xFF90A4AE)
    )
    val preselectedEmojis = listOf("🍔", "🚗", "🛍️", "⚡", "🎬", "🏠", "✈️", "🩺", "🎓", "💸")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Custom Spend Categories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Category Creator Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ADD CUSTOM CATEGORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Emoji Box Picker Toggle (Simplistic simulation)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedColor.copy(alpha = 0.15f))
                                .border(1.dp, selectedColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedEmoji, fontSize = 24.sp)
                        }

                        TextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            placeholder = { Text("E.g., Pet Care", color = TextMuted) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedIndicatorColor = MintGreen,
                                unfocusedIndicatorColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Emojis Selector Row
                    Text("Select Icon", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(preselectedEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedEmoji == emoji) DarkBg else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selectedEmoji == emoji) MintGreen else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Colors Selector Row
                    Text("Select Color Theme", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        preselectedColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (selectedColor == color) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.createCategory(newCategoryName, selectedColor, selectedEmoji)
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintGreen,
                            contentColor = DarkBg,
                            disabledContainerColor = DarkSurfaceElevated,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Category", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ACTIVE CATEGORIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(customCategories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(category.color.copy(alpha = 0.15f))
                                    .border(1.dp, category.color, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = category.icon, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Protect basic template categories from deletion
                        val cannotDelete = listOf("FOOD", "TRANSPORT", "SHOPPING", "BILLS", "UNCATEGORIZED").contains(category.id)
                        if (!cannotDelete) {
                            IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete category",
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Text(text = "System", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NEW SCREEN: CREDIT CATEGORIES MANAGEMENT
// ==========================================
@Composable
fun CreditCategoriesScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val creditCategories by viewModel.creditCategories.collectAsState()
    var newCreditName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Credit & Income Categories",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Creator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = newCreditName,
                    onValueChange = { newCreditName = it },
                    placeholder = { Text("E.g., Freelance", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedIndicatorColor = MintGreen,
                        unfocusedIndicatorColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (newCreditName.isNotBlank()) {
                            viewModel.createCreditCategory(newCreditName)
                            newCreditName = ""
                        }
                    },
                    enabled = newCreditName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = DarkBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ACTIVE CREDIT LABELS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(creditCategories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Income",
                                tint = MintGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = category, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { viewModel.deleteCreditCategory(category) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NEW SCREEN: TAGS MANAGEMENT
// ==========================================
@Composable
fun TagsManagementScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val tags by viewModel.tags.collectAsState()
    var newTagName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tags Management",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Creator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("E.g., BusinessTrip", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedIndicatorColor = MintGreen,
                        unfocusedIndicatorColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName)
                            newTagName = ""
                        }
                    },
                    enabled = newTagName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = DarkBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CUSTOM TRANSACTION TAGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Flexbox-like flow layout using a wrapped row, but simple LazyColumn lists them beautifully as tags!
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tags) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VividGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "#$tag", color = VividGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(onClick = { viewModel.deleteTag(tag) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppChatbotDrawerContent(viewModel: FinanceViewModel) {
    val isWhatsAppConnected by viewModel.isWhatsAppConnected.collectAsState()
    val isWhatsAppBotTyping by viewModel.isWhatsAppBotTyping.collectAsState()
    val whatsAppChatMessages by viewModel.whatsAppChatMessages.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message when size changes
    LaunchedEffect(whatsAppChatMessages.size, isWhatsAppBotTyping) {
        if (whatsAppChatMessages.isNotEmpty()) {
            listState.animateScrollToItem(whatsAppChatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A)) // WhatsApp dark mode background color
    ) {
        // 1. WhatsApp styled header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2C34)) // WhatsApp dark grey header
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.isWhatsAppChatbotActive.value = false },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to drawer menu",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "WhatsApp Bot Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WhatsApp Finance Bot",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isWhatsAppBotTyping) "typing..." else if (isWhatsAppConnected) "Online 🟢" else "Offline ⚪",
                    color = if (isWhatsAppBotTyping) Color(0xFF25D366) else Color(0xFF8696A0),
                    fontSize = 11.sp
                )
            }
        }

        // 2. Chat messages viewport with WhatsApp custom design
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0B141A)) // Dark chat bg
        ) {
            if (whatsAppChatMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(whatsAppChatMessages) { message ->
                        val isUser = message.isFromUser
                        val bubbleBg = if (isUser) Color(0xFF005C4B) else Color(0xFF202C33)
                        val alignment = if (isUser) Alignment.End else Alignment.Start
                        val textColor = if (isUser) Color.White else Color(0xFFE9EDEF)
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = alignment
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                        )
                                    )
                                    .background(bubbleBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(max = 220.dp)
                            ) {
                                Column {
                                    Text(
                                        text = message.text,
                                        color = textColor,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                                        Text(
                                            text = timeStr,
                                            color = Color(0xFF8696A0),
                                            fontSize = 9.sp
                                        )
                                        if (isUser) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Read status",
                                                tint = Color(0xFF53BDEB),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isWhatsAppBotTyping && isWhatsAppConnected) {
                        item {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 0.dp))
                                    .background(Color(0xFF202C33))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("typing...", color = Color(0xFF25D366), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom controls (Connect button or text message input bar)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1F2C34), // WhatsApp bottom input panel
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isWhatsAppConnected) {
                    Button(
                        onClick = { viewModel.connectWhatsAppBot() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("connect_whatsapp_bot_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = "Connect")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect WhatsApp Bot 🟢", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    // Quick-suggestion chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Total Balance 📊", "Spent 💸", "Recent 🕒").forEach { query ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF2A3942))
                                    .clickable {
                                        val cleanText = when(query) {
                                            "Total Balance 📊" -> "What is my total balance?"
                                            "Spent 💸" -> "How much did I spend?"
                                            else -> "Show recent transactions"
                                        }
                                        viewModel.sendWhatsAppMessage(cleanText)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(query, color = Color(0xFF25D366), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Disconnect",
                            color = CoralRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.disconnectWhatsAppBot() }
                                .padding(vertical = 5.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2A3942))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("whatsapp_message_input"),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF25D366)),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text("Type a message...", color = Color(0xFF8696A0), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00A884))
                                .clickable(enabled = inputText.isNotBlank()) {
                                    viewModel.sendWhatsAppMessage(inputText.trim())
                                    inputText = ""
                                }
                                .testTag("whatsapp_send_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send message",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeDismissableCard(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!visible) return
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SwipeDismiss"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > size.width * 0.35f) {
                            onDismiss()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        content()
    }
}
