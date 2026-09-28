package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.TournamentViewModel

// Green gaming style colors
private val DarkBackground = Color(0xFF0D0D12)
private val CardBackground = Color(0xFF16161E)
private val GamingGreen = Color(0xFF00FF7F)
private val LightText = Color(0xFFEEEEEE)
private val SubtitleText = Color(0xFF999999)

@Composable
fun AuthScreen(
    viewModel: TournamentViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }
    val isLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()

    LaunchedEffect(allAccounts) {
        if (allAccounts.isNotEmpty() && !isLoading) {
            // Auto login or transition is handled by viewmodel if session exists
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Gaming-style App Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GamingGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "App Logo",
                    tint = GamingGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Heading & Subtitle
            Text(
                text = if (isLogin) "Welcome Back" else "Create Account",
                color = LightText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isLogin) "Sign in to continue" else "Join Tournament Arena",
                color = SubtitleText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auth Form Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    if (isLogin) {
                        LoginForm(
                            isLoading = isLoading,
                            authError = authError,
                            onLogin = { id, pass ->
                                viewModel.login(id, pass, onSuccess = onAuthSuccess)
                            },
                            onSwitchToRegister = {
                                isLogin = false
                                viewModel.clearAuthError()
                            }
                        )
                    } else {
                        RegisterForm(
                            isLoading = isLoading,
                            authError = authError,
                            onRegister = { name, email, phone, pass ->
                                viewModel.registerWithEmailAndPhone(
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    pass = pass,
                                    onSuccess = onAuthSuccess
                                )
                            },
                            onSwitchToLogin = {
                                isLogin = true
                                viewModel.clearAuthError()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LoginForm(
    isLoading: Boolean,
    authError: String?,
    onLogin: (String, String) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    var phoneOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = phoneOrEmail,
            onValueChange = { phoneOrEmail = it },
            label = { Text("Phone Number or Email") },
            leadingIcon = {
                val isDigits = phoneOrEmail.isNotEmpty() && phoneOrEmail.all { it.isDigit() || it == '+' || it == ' ' }
                Icon(
                    imageVector = if (isDigits) Icons.Default.Phone else Icons.Default.AlternateEmail,
                    contentDescription = null,
                    tint = GamingGreen
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_login_identifier")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GamingGreen) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = SubtitleText
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (phoneOrEmail.isNotBlank() && password.isNotBlank()) {
                    onLogin(phoneOrEmail, password)
                }
            }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_login_password")
        )

        if (authError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = authError, color = Color(0xFFFF5252), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isValid = phoneOrEmail.isNotBlank() && password.isNotBlank()

        Button(
            onClick = {
                focusManager.clearFocus()
                onLogin(phoneOrEmail, password)
            },
            enabled = !isLoading && isValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GamingGreen,
                contentColor = DarkBackground,
                disabledContainerColor = GamingGreen.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_submit_login")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(24.dp))
            } else {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", color = SubtitleText, fontSize = 14.sp)
            Text(
                text = "Create Account",
                color = GamingGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onSwitchToRegister() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun RegisterForm(
    isLoading: Boolean,
    authError: String?,
    onRegister: (String, String, String, String) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GamingGreen) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_register_name")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { 
                Text(
                    if (phone.isNotBlank()) "Email Address (Optional)" 
                    else "Email Address (or Mobile Number)"
                ) 
            },
            placeholder = { Text("player@example.com") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = null,
                    tint = GamingGreen
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_register_email")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { 
                Text(
                    if (email.isNotBlank()) "Mobile Number (Optional)" 
                    else "Mobile Number (or Email Address)"
                ) 
            },
            placeholder = { Text("10-digit mobile number") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = GamingGreen
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_register_phone")
        )

        Spacer(modifier = Modifier.height(6.dp))
        if (email.isBlank() && phone.isBlank()) {
            Text(
                text = "ℹ Provide either Email or Mobile Number (any one required)",
                color = SubtitleText,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (email.isNotBlank() && phone.isBlank()) {
            Text(
                text = "✓ Email entered (Mobile number is optional)",
                color = GamingGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (phone.isNotBlank() && email.isBlank()) {
            Text(
                text = "✓ Mobile number entered (Email is optional)",
                color = GamingGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6 chars)") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GamingGreen) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = SubtitleText
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_register_password")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GamingGreen) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (name.isNotBlank() && (email.isNotBlank() || phone.isNotBlank()) && password.length >= 6 && password == confirmPassword) {
                    onRegister(name, email, phone, password)
                }
            }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamingGreen,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = GamingGreen,
                unfocusedLabelColor = SubtitleText,
                focusedTextColor = LightText,
                unfocusedTextColor = LightText
            ),
            modifier = Modifier.fillMaxWidth().testTag("input_register_confirm_password")
        )

        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Passwords do not match", color = Color(0xFFFF5252), fontSize = 12.sp)
        }
        
        if (authError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = authError, color = Color(0xFFFF5252), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isValid = name.isNotBlank() && (email.isNotBlank() || phone.isNotBlank()) && 
                      password.length >= 6 && password == confirmPassword

        Button(
            onClick = {
                focusManager.clearFocus()
                onRegister(name, email, phone, password)
            },
            enabled = !isLoading && isValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GamingGreen,
                contentColor = DarkBackground,
                disabledContainerColor = GamingGreen.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_submit_register")
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(24.dp))
            } else {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = SubtitleText, fontSize = 14.sp)
            Text(
                text = "Login",
                color = GamingGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onSwitchToLogin() }
                    .padding(4.dp)
            )
        }
    }
}
