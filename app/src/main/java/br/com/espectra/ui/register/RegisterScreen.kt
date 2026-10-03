package br.com.espectra.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFFFFBEB)
private val Ink = Color(0xFF1E293B)
private val Muted = Color(0xFF64748B)
private val Brand = Color(0xFFF97316)
private val BrandDark = Color(0xFFEA580C)
private val ErrorRed = Color(0xFFEF4444)
private val ErrorBg = Color(0xFFFEF2F2)
private val SuccessBg = Color(0xFFF0FDF4)
private val SuccessText = Color(0xFF15803D)

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    RegisterScreenContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onBirthdayChange = viewModel::onBirthdayChange,
        onGenderChange = viewModel::onGenderChange,
        onRegister = viewModel::register,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreenContent(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onRegister: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🧩", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "auticonecta",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
            )
        }

        Text(
            text = "Criar conta",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
        Text(
            text = "Leva menos de um minuto. Verifique seu email para confirmar e entrar.",
            fontSize = 14.sp,
            color = Muted,
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = { Text("Nome") },
                        singleLine = true,
                        isError = state.nameError != null,
                        supportingText = state.nameError?.let { { Text(it) } },
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = onLastNameChange,
                        label = { Text("Sobrenome") },
                        singleLine = true,
                        isError = state.lastNameError != null,
                        supportingText = state.lastNameError?.let { { Text(it) } },
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { Text(it) } },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Senha (mín. 6)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { { Text(it) } },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = state.confirmPasswordError != null,
                    supportingText = state.confirmPasswordError?.let { { Text(it) } },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.birthday,
                    onValueChange = onBirthdayChange,
                    label = { Text("Nascimento (AAAA-MM-DD, opcional)") },
                    singleLine = true,
                    placeholder = { Text("1990-05-20") },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(text = "Gênero", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("male" to "Masculino", "female" to "Feminino", "other" to "Outro").forEach { (value, label) ->
                        FilterChip(
                            selected = state.gender == value,
                            onClick = { onGenderChange(value) },
                            label = { Text(label) },
                        )
                    }
                }
                state.genderError?.let {
                    Text(text = it, fontSize = 12.sp, color = ErrorRed)
                }

                state.formError?.let { message ->
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorBg, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                    )
                }

                if (state.registered) {
                    Text(
                        text = "Conta criada! Verifique seu email para confirmar e entrar.",
                        fontSize = 13.sp,
                        color = SuccessText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SuccessBg, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                    )
                }

                Button(
                    onClick = onRegister,
                    enabled = !state.loading,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand,
                        contentColor = Color.White,
                        disabledContainerColor = Brand.copy(alpha = 0.6f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (state.loading) "Criando..." else "Concluir cadastro",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text = "Já tenho conta — entrar", fontSize = 13.sp, color = Muted)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand,
    focusedLabelColor = BrandDark,
    errorBorderColor = ErrorRed,
    errorLabelColor = ErrorRed,
)
