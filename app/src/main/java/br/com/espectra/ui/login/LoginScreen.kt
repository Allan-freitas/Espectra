package br.com.espectra.ui.login

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.ui.theme.EspectraTheme

// Identidade AutiConecta (mesma da landing web)
private val Cream = Color(0xFFFFFBEB)
private val Ink = Color(0xFF1E293B)
private val Muted = Color(0xFF64748B)
private val Brand = Color(0xFFF97316)
private val BrandDark = Color(0xFFEA580C)
private val PillBg = Color(0xFFFFEDD5)
private val PillBorder = Color(0xFFFED7AA)
private val PillText = Color(0xFF9A3412)
private val ErrorRed = Color(0xFFEF4444)
private val ErrorBg = Color(0xFFFEF2F2)
private val Rainbow = listOf(
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFFEAB308),
    Color(0xFF22C55E),
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: (String) -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState

    LaunchedEffect(state.loggedInName) {
        state.loggedInName?.let { onLoggedIn(it) }
    }

    LoginScreenContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
        modifier = modifier,
    )
}

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        // Marca d'água: infinito da neurodiversidade
        Text(
            text = "∞",
            fontSize = 300.sp,
            color = Brand,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(0.06f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = PillBg,
                border = BorderStroke(1.dp, PillBorder),
            ) {
                Text(
                    text = "♾️ Feita para pessoas autistas",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PillText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Text(
                text = buildAnnotatedString {
                    append("Um lugar onde você pode ser ")
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(Rainbow),
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    ) {
                        append("você")
                    }
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp,
                color = Ink,
            )

            Text(
                text = "A AutiConecta é uma rede social acolhedora para pessoas autistas " +
                    "se conectarem, compartilharem momentos e criarem amizades — no seu ritmo.",
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = Muted,
            )

            Button(
                onClick = onRegisterClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Criar conta grátis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            listOf(
                "👥" to "Comunidade que acolhe",
                "📸" to "Compartilhe momentos",
                "💬" to "Converse em tempo real",
            ).forEach { (icon, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White) {
                        Text(
                            text = icon,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                }
            }

            // Card de login (faixa arco-íris no topo, como no web)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Brush.horizontalGradient(Rainbow)),
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Entrar",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = state.emailError != null,
                            supportingText = state.emailError?.let { { Text(it) } },
                            shape = RoundedCornerShape(8.dp),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        var passwordVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = { Text("Senha") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(if (passwordVisible) "🙈" else "👁️")
                                }
                            },
                            isError = state.passwordError != null,
                            supportingText = state.passwordError?.let { { Text(it) } },
                            shape = RoundedCornerShape(8.dp),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        state.loginError?.let { message ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ErrorBg,
                            ) {
                                Text(
                                    text = message,
                                    fontSize = 13.sp,
                                    color = ErrorRed,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                )
                            }
                        }

                        Button(
                            onClick = onLogin,
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
                                text = if (state.loading) "Entrando..." else "Entrar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }

                        TextButton(
                            onClick = onForgotPasswordClick,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(text = "Esqueceu a senha?", fontSize = 13.sp, color = Muted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand,
    focusedLabelColor = BrandDark,
    errorBorderColor = ErrorRed,
    errorLabelColor = ErrorRed,
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    EspectraTheme {
        LoginScreenContent(
            state = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    EspectraTheme {
        LoginScreenContent(
            state = LoginUiState(
                email = "teste@mail.com",
                emailError = "Formato de email inválido",
                loginError = "Invalid email or password.",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
        )
    }
}
