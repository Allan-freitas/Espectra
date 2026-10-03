package br.com.espectra.ui.forgot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = "🔑", fontSize = 40.sp)
        Text(
            text = "Esqueceu a senha?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
        Text(
            text = "Informe seu email. Se ele estiver cadastrado, você recebe um link de redefinição.",
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
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { Text(it) } },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand,
                        focusedLabelColor = BrandDark,
                        errorBorderColor = ErrorRed,
                        errorLabelColor = ErrorRed,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

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

                if (state.sent) {
                    Text(
                        text = "Se o email estiver cadastrado, um link de redefinição foi enviado.",
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
                    onClick = viewModel::submit,
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
                        text = if (state.loading) "Enviando..." else "Enviar link",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text = "Voltar para entrar", fontSize = 13.sp, color = Muted)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
