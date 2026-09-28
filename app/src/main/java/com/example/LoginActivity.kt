package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.SoundScriptTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginViewMode {
    data object Login : LoginViewMode()
    data class AccessRestricted(
        val email: String,
        val message: String? = null,
        val isChecking: Boolean = false
    ) : LoginViewMode()
}

class LoginActivity : ComponentActivity() {

    // Web Client ID for Credential Manager Google Sign-In
    val WEB_CLIENT_ID = "386460666392-mdl872rgt4lel8167vpo48t7kk6ohdot.apps.googleusercontent.com"

    lateinit var credentialManager: CredentialManager
    private var errorMessageState = mutableStateOf<String?>(null)
    private var isLoadingState = mutableStateOf(false)
    private var viewModeState = mutableStateOf<LoginViewMode>(LoginViewMode.Login)

    companion object {
        const val DEFAULT_WEB_CLIENT_ID = "386460666392-mdl872rgt4lel8167vpo48t7kk6ohdot.apps.googleusercontent.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.theme.ThemeManager.initTheme(this)
        enableEdgeToEdge()

        // 1. Instantiate the CredentialManager
        credentialManager = CredentialManager.create(this)

        // Check if already signed in; if so, verify remote subscription access
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }

        if (currentUser?.email != null && currentUser.email!!.isNotBlank()) {
            val email = currentUser.email!!
            isLoadingState.value = true
            lifecycleScope.launch {
                checkSubscriptionAndNavigate(email)
            }
        }

        setContent {
            SoundScriptTheme {
                val currentMode by viewModeState
                val errorMessage by errorMessageState
                val isLoading by isLoadingState

                when (val mode = currentMode) {
                    is LoginViewMode.Login -> {
                        LoginScreen(
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onGoogleSignInClick = { onSignInWithGoogleClicked() },
                            onGuestClick = { navigateToMainScreen() }
                        )
                    }
                    is LoginViewMode.AccessRestricted -> {
                        AccessRestrictedScreen(
                            signedInEmail = mode.email,
                            message = mode.message,
                            isRefreshing = mode.isChecking,
                            onRefreshAccess = { refreshAccess(mode.email) },
                            onContactToSubscribe = { contactToSubscribe() },
                            onSignOutClick = { signOutAndReturnToLogin() }
                        )
                    }
                }
            }
        }
    }

    fun onSignInWithGoogleClicked() {
        lifecycleScope.launch {
            isLoadingState.value = true
            errorMessageState.value = null
            signInWithGoogle()
            isLoadingState.value = false
        }
    }

    /**
     * Executes Google Sign-in flow using Android Credential Manager API.
     */
    suspend fun signInWithGoogle(): Boolean {
        return try {
            // 2. Configure a GetGoogleIdOption (with setFilterByAuthorizedAccounts(false) so new users can create an account)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            // Configure a GetCredentialRequest
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 3. Launch credentialManager.getCredential()
            val result = credentialManager.getCredential(
                request = request,
                context = this@LoginActivity
            )

            // 4. Extract the CustomCredential and parse using GoogleIdTokenCredential.createFrom()
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val signedInEmail = googleIdTokenCredential.id.trim()
                Log.d("LoginActivity", "Successfully retrieved Google ID token for ${googleIdTokenCredential.displayName} ($signedInEmail)")

                // Link to Firebase if available
                try {
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                } catch (fbEx: Exception) {
                    Log.w("LoginActivity", "Firebase Auth sign-in notice: ${fbEx.message}")
                }

                // Verify remote email subscription gate
                val effectiveEmail = signedInEmail.ifBlank {
                    FirebaseAuth.getInstance().currentUser?.email.orEmpty().trim()
                }
                checkSubscriptionAndNavigate(effectiveEmail)
                true
            } else {
                val err = "Unexpected credential returned: ${credential.javaClass.name}"
                errorMessageState.value = err
                Log.w("LoginActivity", err)
                false
            }
        } catch (e: GetCredentialException) {
            // 5. Basic error handling for GetCredentialException
            val err = "Google Sign-in failed or was cancelled: ${e.message}"
            Log.e("LoginActivity", "GetCredentialException: ${e.message}", e)
            errorMessageState.value = err
            false
        } catch (e: GoogleIdTokenParsingException) {
            // Error handling for GoogleIdTokenParsingException
            val err = "Google ID token parsing error: ${e.message}"
            Log.e("LoginActivity", "GoogleIdTokenParsingException: ${e.message}", e)
            errorMessageState.value = err
            false
        } catch (e: Exception) {
            val err = "Authentication error: ${e.message}"
            Log.e("LoginActivity", "Exception: ${e.message}", e)
            errorMessageState.value = err
            false
        }
    }

    /**
     * Checks remote subscription access via AccessManager.
     * If user email is in the remote subscribers JSON array, grant access to main app.
     * If not, display the Access Restricted lock screen.
     */
    suspend fun checkSubscriptionAndNavigate(email: String) {
        isLoadingState.value = true
        when (val result = AccessManager.verifyAccess(email)) {
            is AccessManager.AccessResult.Granted -> {
                isLoadingState.value = false
                navigateToMainScreen()
            }
            is AccessManager.AccessResult.Blocked -> {
                isLoadingState.value = false
                viewModeState.value = LoginViewMode.AccessRestricted(
                    email = email,
                    message = result.message,
                    isChecking = false
                )
            }
            is AccessManager.AccessResult.NetworkError -> {
                isLoadingState.value = false
                viewModeState.value = LoginViewMode.AccessRestricted(
                    email = email,
                    message = result.message,
                    isChecking = false
                )
            }
        }
    }

    /**
     * Re-fetches the remote JSON array so user can get in immediately after Gist update.
     */
    fun refreshAccess(email: String) {
        lifecycleScope.launch {
            viewModeState.value = LoginViewMode.AccessRestricted(
                email = email,
                message = getString(R.string.login_rechecking_subscription),
                isChecking = true
            )
            when (val result = AccessManager.verifyAccess(email)) {
                is AccessManager.AccessResult.Granted -> {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_toast_subscription_verified), Toast.LENGTH_SHORT).show()
                    navigateToMainScreen()
                }
                is AccessManager.AccessResult.Blocked -> {
                    viewModeState.value = LoginViewMode.AccessRestricted(
                        email = email,
                        message = result.message ?: getString(R.string.login_toast_account_not_listed),
                        isChecking = false
                    )
                }
                is AccessManager.AccessResult.NetworkError -> {
                    viewModeState.value = LoginViewMode.AccessRestricted(
                        email = email,
                        message = result.message,
                        isChecking = false
                    )
                }
            }
        }
    }

    /**
     * Opens contact link to https://t.me/MMF5C3 via an Intent.
     */
    fun contactToSubscribe() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AccessManager.TELEGRAM_CONTACT_URL))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Could not launch Telegram URL", e)
            Toast.makeText(this, getString(R.string.login_toast_cannot_open_telegram, AccessManager.TELEGRAM_CONTACT_URL), Toast.LENGTH_LONG).show()
        }
    }

    fun signOutAndReturnToLogin() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w("LoginActivity", "Sign out notice: ${e.message}")
        }
        viewModeState.value = LoginViewMode.Login
        errorMessageState.value = null
        isLoadingState.value = false
    }

    fun navigateToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onGuestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Icon Header
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = stringResource(R.string.login_logo_desc),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = stringResource(R.string.app_badge_med),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        // Distinct "Sign in with Google" button
                        Button(
                            onClick = onGoogleSignInClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("sign_in_with_google_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = stringResource(R.string.login_google_icon_desc),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.login_google_sign_in),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Guest entry button
                        OutlinedButton(
                            onClick = onGuestClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("continue_as_guest_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.login_continue_guest),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lock Screen UI displayed when signed-in user's email is not in the remote subscribers JSON array.
 */
@Composable
fun AccessRestrictedScreen(
    signedInEmail: String,
    message: String?,
    isRefreshing: Boolean,
    onRefreshAccess: () -> Unit,
    onContactToSubscribe: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onSignOutClick()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("access_restricted_screen"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("access_restricted_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Lock icon container
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.access_restricted_lock_icon_desc),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.access_restricted_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.access_restricted_subtitle),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Display user's signed-in email card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = stringResource(R.string.access_restricted_email_icon_desc),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.access_restricted_signed_in_account),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = signedInEmail.ifBlank { stringResource(R.string.access_restricted_unknown_account) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("restricted_email_text")
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.access_restricted_subscription_required),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!message.isNullOrBlank()) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isRefreshing) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.access_restricted_checking_directory),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Refresh Access button
                        Button(
                            onClick = onRefreshAccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("refresh_access_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.access_restricted_refresh_icon_desc),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.access_restricted_refresh_access),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Contact to Subscribe button (Opens Intent to https://t.me/MMF5C3)
                        OutlinedButton(
                            onClick = onContactToSubscribe,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("contact_to_subscribe_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.access_restricted_telegram_icon_desc),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.access_restricted_contact_to_subscribe),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Sign out / Return to login
                        TextButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.testTag("sign_out_switch_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.access_restricted_sign_in_different_account),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
