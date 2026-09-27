package com.rotitrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotitrack.app.account.AuthService
import com.rotitrack.app.account.validateEmail
import com.rotitrack.app.account.validatePassword
import kotlinx.coroutines.launch

private enum class Mode { SIGN_IN, SIGN_UP, RESET }

@Composable
fun LoginScreen(auth: AuthService) {
    var mode by remember { mutableStateOf(Mode.SIGN_IN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referral by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null
        info = null
        val problem = when (mode) {
            Mode.SIGN_UP -> if (name.isBlank()) "Enter your name" else validateEmail(email) ?: validatePassword(password)
            Mode.SIGN_IN -> validateEmail(email) ?: if (password.isEmpty()) "Enter your password" else null
            Mode.RESET -> validateEmail(email)
        }
        if (problem != null) {
            error = problem
            return
        }
        busy = true
        scope.launch {
            try {
                when (mode) {
                    Mode.SIGN_IN -> auth.signIn(email.trim(), password)
                    Mode.SIGN_UP -> auth.signUp(name.trim(), email.trim(), password, referral)
                    Mode.RESET -> {
                        auth.sendPasswordReset(email.trim())
                        info = "Check your inbox for a link to reset your password."
                        mode = Mode.SIGN_IN
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Try again."
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Palette.brand).safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(RotiLogo, contentDescription = "Roti Track logo", modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(10.dp))
            Image(WaterDropLogo, contentDescription = null, modifier = Modifier.size(width = 48.dp, height = 64.dp))
        }
        Text("ROTI TRACK", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Indian diet planner, calorie & water tracker",
            style = Type.body.copy(color = Color.White.copy(alpha = 0.8f)), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(26.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when (mode) {
                    Mode.SIGN_IN -> "Welcome back"
                    Mode.SIGN_UP -> "Create your account"
                    Mode.RESET -> "Reset password"
                },
                style = Type.h2,
            )
            if (mode == Mode.SIGN_UP) Field(name, { name = it }, "Full name")
            Field(email, { email = it }, "Email", KeyboardType.Email)
            if (mode != Mode.RESET) {
                Field(
                    password, { password = it }, "Password", KeyboardType.Password,
                    hidden = !showPassword,
                    trailing = {
                        Text(
                            if (showPassword) "Hide" else "Show",
                            style = Type.small.copy(color = Palette.brand),
                            modifier = Modifier.clickable { showPassword = !showPassword }.padding(12.dp),
                        )
                    },
                )
                if (mode == Mode.SIGN_UP) {
                    Text("At least 8 characters, with letters and numbers.", style = Type.small)
                    Field(referral, { referral = it.uppercase().take(12) }, "Referral code (optional)")
                }
            }
            error?.let { Text(it, style = Type.small.copy(color = Palette.danger)) }
            info?.let { Text(it, style = Type.small.copy(color = Palette.leaf)) }

            PillButton(
                if (busy) "Please wait…" else when (mode) {
                    Mode.SIGN_IN -> "Sign in"
                    Mode.SIGN_UP -> "Create account"
                    Mode.RESET -> "Send reset link"
                },
                ::submit, Modifier.fillMaxWidth(), enabled = !busy,
            )
            if (mode == Mode.SIGN_IN) {
                Text(
                    "Forgot password?", style = Type.small.copy(color = Palette.brand),
                    modifier = Modifier.clickable { mode = Mode.RESET; error = null }.padding(vertical = 4.dp),
                )
            }
        }

        Row(Modifier.padding(top = 20.dp)) {
            Text(
                if (mode == Mode.SIGN_UP) "Already have an account? " else "New to Roti Track? ",
                style = Type.body.copy(color = Color.White.copy(alpha = 0.8f)),
            )
            Text(
                if (mode == Mode.SIGN_UP) "Sign in" else "Create account",
                style = Type.body.copy(color = Color.White, fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.clickable {
                    mode = if (mode == Mode.SIGN_UP) Mode.SIGN_IN else Mode.SIGN_UP
                    error = null
                },
            )
        }
        Text(
            "Accounts: ${auth.backendLabel}",
            style = Type.tiny.copy(color = Color.White.copy(alpha = 0.6f)), modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    hidden: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        trailingIcon = trailing,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.brand),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun BlockedScreen(onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Palette.background).safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🚫", fontSize = 56.sp)
        Text("Account blocked", style = Type.h2, modifier = Modifier.padding(top = 12.dp))
        Text(
            "This account has been blocked by an administrator. Contact support if you think this is a mistake.",
            style = Type.body.copy(color = Palette.inkSoft), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp),
        )
        PillButton("Sign out", onSignOut, Modifier.fillMaxWidth())
    }
}
