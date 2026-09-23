package com.mychhachh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted

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

    val title = when (mode) {
        "register" -> "Sign up with email"
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

    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Brand(modifier = Modifier.padding(vertical = 8.dp))
        Text(subtitle, color = JellyMuted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        JellyGlass(Modifier.fillMaxWidth(), padding = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PageTitle(title, icon = when (mode) {
                    "register" -> JellyIcons.Plus
                    "verify" -> JellyIcons.Mail
                    "forgot", "reset" -> JellyIcons.Lock
                    else -> JellyIcons.User
                })

                when (mode) {
                    "register" -> {
                        OutlinedTextField(name, { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                        OutlinedTextField(username, { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } }, label = { Text("Username (a-z, 0-9, _)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                        OutlinedTextField(email, { email = it }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text("Password (6+ characters)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    }
                    "verify" -> {
                        if (pendingEmail.isNotBlank()) Text(pendingEmail, color = JellyInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit code") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                    }
                    "forgot" -> {
                        OutlinedTextField(email, { email = it }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                    }
                    "reset" -> {
                        OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit code") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text("New password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        OutlinedTextField(password2, { password2 = it }, label = { Text("Confirm password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    }
                    else -> {
                        OutlinedTextField(identity, { identity = it }, label = { Text("Email, username or phone") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text("Password (6+ characters)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    }
                }

                if (!error.isNullOrBlank()) Text(error, color = androidx.compose.ui.graphics.Color(0xFFB23A55), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                val buttonText = when (mode) {
                    "register" -> "Sign up with email"
                    "verify" -> "Verify"
                    "forgot" -> "Send Reset Code"
                    "reset" -> "Reset Password"
                    else -> "Login"
                }
                JellyButton(if (busy) "Please wait…" else buttonText, modifier = Modifier.fillMaxWidth(), primary = true, icon = when (mode) {
                    "register" -> JellyIcons.Plus
                    "verify" -> JellyIcons.Check
                    "forgot", "reset" -> JellyIcons.Lock
                    else -> JellyIcons.User
                }) {
                    if (busy) return@JellyButton
                    when (mode) {
                        "register" -> onRegister(name.trim(), username.trim(), email.trim(), password)
                        "verify" -> if (code.length == 6) onVerify(code)
                        "forgot" -> if (email.isNotBlank()) onForgotSend(email.trim())
                        "reset" -> if (code.length == 6 && password.length >= 6 && password == password2) onForgotReset(code, password)
                        else -> onLogin(identity.trim(), password)
                    }
                }

                if (mode == "verify") JellyButton("Resend Code", modifier = Modifier.fillMaxWidth(), icon = JellyIcons.Mail, onClick = onResend)

                if (mode == "login") {
                    TextButton(onClick = { onSwitch("forgot") }) { Text("Forgot password?", color = JellyInk, fontWeight = FontWeight.Bold) }
                }
                if (mode == "register" || mode == "login") {
                    Text("I agree to the Terms & Conditions and Privacy Policy.", color = JellyMuted, fontSize = 10.sp)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("Back", color = JellyInk) }
                    when (mode) {
                        "register" -> TextButton(onClick = { onSwitch("login") }) { Text("Already registered? Login", color = JellyInk, fontWeight = FontWeight.Bold) }
                        "login" -> TextButton(onClick = { onSwitch("register") }) { Text("No account? Create one", color = JellyInk, fontWeight = FontWeight.Bold) }
                        "verify", "forgot", "reset" -> TextButton(onClick = { onSwitch("login") }) { Text("Back to login", color = JellyInk, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
