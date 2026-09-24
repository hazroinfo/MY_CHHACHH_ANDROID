package com.mychhachh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted
import com.mychhachh.app.ui.theme.LiveJellyTheme

@Composable
fun AuthScreen(
    mode: String,
    busy: Boolean,
    error: String?,
    pendingEmail: String,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onForgotSend: (String) -> Unit,
    onForgotReset: (String, String) -> Unit,
    googleEnabled: Boolean,
    googleClientId: String,
    onGoogleCredential: (String) -> Unit,
    onSwitch: (String) -> Unit,
    onBack: () -> Unit
) {
    var identity by remember(mode) { mutableStateOf("") }
    var password by remember(mode) { mutableStateOf("") }
    var password2 by remember(mode) { mutableStateOf("") }
    var name by remember(mode) { mutableStateOf("") }
    var username by remember(mode) { mutableStateOf("") }
    var email by remember(mode, pendingEmail) { mutableStateOf(pendingEmail) }
    var code by remember(mode) { mutableStateOf("") }
    var termsAccepted by remember(mode) { mutableStateOf(false) }

    val context = LocalContext.current
    val googleScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var googleBusy by remember(mode) { mutableStateOf(false) }
    var googleError by remember(mode) { mutableStateOf<String?>(null) }

    fun startGoogleSignIn() {
        if (!googleEnabled || googleClientId.isBlank() || googleBusy || busy) return
        googleScope.launch {
            googleBusy = true
            googleError = null
            try {
                val option = GetSignInWithGoogleOption.Builder(
                    serverClientId = googleClientId
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                val credential = result.credential
                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                    onGoogleCredential(google.idToken)
                } else {
                    googleError = "Google sign-in did not return an ID token."
                }
            } catch (e: GetCredentialException) {
                googleError = e.message ?: "Google sign-in was cancelled."
            } catch (e: Exception) {
                googleError = e.message ?: "Google sign-in failed."
            } finally {
                googleBusy = false
            }
        }
    }

    val title = when (mode) {
        "register" -> "Create account"
        "verify" -> "Verify Email"
        "forgot" -> "Reset password"
        "reset" -> "Enter reset code"
        else -> "Welcome back"
    }
    val subtitle = when (mode) {
        "register" -> "Create your account with email."
        "verify" -> "Enter the 6-digit code sent to your email."
        "forgot" -> "Enter the email address saved on your account."
        "reset" -> "Enter the code from your email and choose a new password."
        else -> "Sign in to My Chhachh"
    }

    Box(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        JellyGlass(
            Modifier.fillMaxWidth().widthIn(max = 430.dp),
            radius = 26.dp,
            padding = 18.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    title,
                    color = JellyInk,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
                Text(subtitle, color = JellyMuted, fontSize = 11.sp)

                if ((mode == "login" || mode == "register") && googleEnabled && googleClientId.isNotBlank()) {
                    JellyButton(
                        if (googleBusy) "Opening Google…" else "Continue with Google",
                        modifier = Modifier.fillMaxWidth(),
                        icon = JellyIcons.User,
                        enabled = !busy && !googleBusy
                    ) { startGoogleSignIn() }
                    googleError?.let {
                        Text(
                            it,
                            color = androidx.compose.ui.graphics.Color(0xFFB23A55),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5f.sp
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text("or", color = JellyMuted, fontSize = 9.5f.sp)
                        Spacer(Modifier.weight(1f))
                    }
                }

                when (mode) {
                    "register" -> {
                        OutlinedTextField(name, { name = it }, placeholder = { Text("Full name") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                        OutlinedTextField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } }, placeholder = { Text("Username (a-z, 0-9, _)") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                        OutlinedTextField(email, { email = it }, placeholder = { Text("Email address") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                        OutlinedTextField(password, { password = it }, placeholder = { Text("Password (6+ characters)") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { termsAccepted = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "I agree to the Terms & Conditions and Privacy Policy.",
                                Modifier.weight(1f),
                                color = JellyMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    "verify" -> {
                        if (pendingEmail.isNotBlank()) Text(pendingEmail, color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, placeholder = { Text("6-digit code") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                    }
                    "forgot" -> {
                        OutlinedTextField(email, { email = it }, placeholder = { Text("Email address") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                    }
                    "reset" -> {
                        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, placeholder = { Text("6-digit code") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                        OutlinedTextField(password, { password = it }, placeholder = { Text("New password") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        OutlinedTextField(password2, { password2 = it }, placeholder = { Text("Confirm password") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    }
                    else -> {
                        OutlinedTextField(identity, { identity = it }, placeholder = { Text("Email, username or phone") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), singleLine = true)
                        OutlinedTextField(password, { password = it }, placeholder = { Text("Password") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(LiveJellyTheme.inputRadius.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    }
                }

                if (!error.isNullOrBlank()) {
                    Text(error, color = androidx.compose.ui.graphics.Color(0xFFB23A55), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                val buttonText = when (mode) {
                    "register" -> "Sign up with email"
                    "verify" -> "Verify"
                    "forgot" -> "Send Reset Code"
                    "reset" -> "Reset Password"
                    else -> "Login"
                }
                val submitEnabled = !busy && when (mode) {
                    "register" -> name.trim().isNotBlank() && username.length >= 3 && email.isNotBlank() && password.length >= 6 && termsAccepted
                    "verify" -> code.length == 6
                    "forgot" -> email.isNotBlank()
                    "reset" -> code.length == 6 && password.length >= 6 && password == password2
                    else -> identity.isNotBlank() && password.length >= 6
                }
                JellyButton(
                    if (busy) "Please wait…" else buttonText,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    primary = true,
                    icon = when (mode) {
                        "register" -> JellyIcons.Plus
                        "verify" -> JellyIcons.Check
                        "forgot", "reset" -> JellyIcons.Lock
                        else -> JellyIcons.User
                    },
                    enabled = submitEnabled
                ) {
                    when (mode) {
                        "register" -> onRegister(name.trim(), username.trim(), email.trim(), password)
                        "verify" -> onVerify(code)
                        "forgot" -> onForgotSend(email.trim())
                        "reset" -> onForgotReset(code, password)
                        else -> onLogin(identity.trim(), password)
                    }
                }

                if (mode == "verify") {
                    JellyButton("Resend Code", modifier = Modifier.fillMaxWidth(), icon = JellyIcons.Mail, onClick = onResend)
                }

                when (mode) {
                    "login" -> {
                        TextButton(onClick = { onSwitch("forgot") }) {
                            Text("Forgot password?", color = JellyInk, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { onSwitch("register") }) {
                            Text("No account? Create one", color = JellyInk, fontWeight = FontWeight.Bold)
                        }
                    }
                    "register" -> {
                        TextButton(onClick = { onSwitch("login") }) {
                            Text("Already registered? Login", color = JellyInk, fontWeight = FontWeight.Bold)
                        }
                    }
                    "forgot", "reset" -> {
                        TextButton(onClick = { onSwitch("login") }) {
                            Text("Back to login", color = JellyInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }}
