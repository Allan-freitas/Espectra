package br.com.espectra.ui.pin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.espectra.data.PinManager
import br.com.espectra.ui.feed.FeedBrand
import br.com.espectra.ui.feed.FeedCream
import br.com.espectra.ui.feed.FeedInk
import br.com.espectra.ui.feed.FeedMuted

/** Criação do PIN logo após o login (pode pular). */
@Composable
fun PinSetupScreen(
    userName: String?,
    onComplete: (pin: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var first by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit(pin: String) {
        if (first.isEmpty()) {
            first = pin
            error = null
        } else if (pin == first) {
            onComplete(pin)
        } else {
            first = ""
            error = "Os PINs não conferem. Tente de novo."
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(FeedCream)
            .padding(24.dp),
    ) {
        Text(text = "🔐", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Acesso rápido",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FeedInk,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (first.isEmpty()) {
                "Crie um PIN de ${PinManager.PIN_LENGTH} dígitos para entrar sem digitar a senha."
            } else {
                "Confirme o PIN."
            },
            fontSize = 14.sp,
            color = FeedMuted,
            textAlign = TextAlign.Center,
        )
        if (!userName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = userName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeedInk)
        }
        Spacer(modifier = Modifier.height(20.dp))
        PinPad(
            onDone = ::submit,
            onChange = { if (error != null) error = null },
        )
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = Color(0xFFEF4444))
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text(text = "Agora não", fontSize = 14.sp, color = FeedMuted)
        }
    }
}

/** Desbloqueio: PIN -> login silencioso. */
@Composable
fun UnlockScreen(
    viewModel: UnlockViewModel,
    onUnlocked: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState
    val storedName = state.userName

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(FeedCream)
            .padding(24.dp),
    ) {
        Text(text = "🧩", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Olá de novo${if (!storedName.isNullOrBlank()) ", ${storedName.split(" ").first()}" else ""}!",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FeedInk,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Digite seu PIN para entrar.",
            fontSize = 14.sp,
            color = FeedMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (state.loading) {
            CircularProgressIndicator(color = FeedBrand)
        } else {
            PinPad(
                onDone = { pin ->
                    viewModel.unlock(pin) { name ->
                        if (name != null) onUnlocked() else onUsePassword()
                    }
                },
                onChange = { viewModel.clearError() },
            )
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (state.attemptsLeft in 1..2) {
                    "$it Restam ${state.attemptsLeft} tentativa(s) antes do bloqueio."
                } else {
                    it
                },
                fontSize = 13.sp,
                color = Color(0xFFEF4444),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onUsePassword) {
            Text(text = "Usar email e senha", fontSize = 14.sp, color = FeedMuted)
        }
    }
}

/** Teclado numérico com bolinhas; chama onDone ao completar 4 dígitos. */
@Composable
private fun PinPad(
    onDone: (String) -> Unit,
    onChange: () -> Unit = {},
) {
    var digits by remember { mutableStateOf("") }
    val max = PinManager.PIN_LENGTH

    // Avisa o pai a cada tecla (p/ limpar erro) sem reenviar o PIN pronto.
    LaunchedEffect(digits) {
        if (digits.length == max) {
            val pin = digits
            digits = ""
            onDone(pin)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(max) { i ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            if (i < digits.length) FeedBrand else Color(0xFFE2E8F0),
                            CircleShape,
                        ),
                ) {}
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { d ->
                    DigitKey(label = d) {
                        if (digits.length < max) {
                            digits += d
                            onChange()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(64.dp))
            DigitKey(label = "0") {
                if (digits.length < max) {
                    digits += "0"
                    onChange()
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (digits.isNotEmpty()) {
                            digits = digits.dropLast(1)
                            onChange()
                        }
                    },
            ) {
                Text(text = "⌫", fontSize = 24.sp, color = FeedInk)
            }
        }
    }
}

@Composable
private fun DigitKey(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .background(Color.White, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Text(text = label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FeedInk)
    }
}
