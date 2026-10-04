package br.com.espectra.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import br.com.espectra.ui.feed.FeedBrand
import br.com.espectra.ui.feed.FeedBrandDark
import br.com.espectra.ui.feed.FeedCardBg
import br.com.espectra.ui.feed.FeedCream
import br.com.espectra.ui.feed.FeedInk
import br.com.espectra.ui.feed.FeedMuted
import br.com.espectra.ui.feed.InitialsAvatar
import br.com.espectra.ui.feed.RemoteImage

/** Espelha about.component + profile-header (leitura; sem upload por ora). */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** "avatar" | "cover" | null — qual foto o diálogo Câmera/Galeria vai trocar. */
    var pickerFor by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraTarget by remember { mutableStateOf("avatar") }

    fun readBytes(uri: Uri, onDone: (ByteArray) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) {
                if (bytes != null && bytes.isNotEmpty()) onDone(bytes)
            }
        }
    }

    fun uploadPicked(target: String, bytes: ByteArray) {
        if (target == "avatar") viewModel.uploadAvatar(bytes) else viewModel.uploadCover(bytes)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            readBytes(uri) { uploadPicked(pendingCameraTarget, it) }
        }
        cameraUri = null
    }

    fun startCamera(target: String) {
        try {
            val file = File.createTempFile("camera_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "br.com.espectra.fileprovider", file)
            cameraUri = uri
            pickerFor = null
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("Não foi possível abrir a câmera.") }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startCamera(pendingCameraTarget)
        } else {
            scope.launch { snackbar.showSnackbar("Permissão de câmera negada.") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pickerFor
        pickerFor = null
        if (uri != null && target != null) {
            readBytes(uri) { uploadPicked(target, it) }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    fun openUrl(url: String) {
        val fixed = if (url.startsWith("http")) url else "https://$url"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, fixed.toUri()))
        } catch (e: Exception) {
            // sem navegador
        }
    }

    Box(modifier = modifier.fillMaxSize().background(FeedCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "← Voltar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeedBrandDark)
                }
            }

            if (state.loading && state.personalInfo == null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = FeedBrand)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                ) {
                    // ---- header (cover + avatar + nome) ----
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Box {
                                RemoteImage(
                                    pathOrUrl = state.coverPath,
                                    contentDescription = "Capa",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(androidx.compose.ui.graphics.Color(0xFFFFEDD5))
                                        .then(
                                            if (state.isMine && !state.uploadingCover) {
                                                Modifier.clickable { pickerFor = "cover" }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    fallback = { Text(text = "♾️", fontSize = 40.sp) },
                                )
                                if (state.isMine && !state.uploadingCover) {
                                    Text(
                                        text = "📷 Trocar capa",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                                                RoundedCornerShape(999.dp),
                                            )
                                            .clickable { pickerFor = "cover" }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                                if (state.uploadingCover) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                                    ) {
                                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = (-34).dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                androidx.compose.ui.graphics.Color.White,
                                                androidx.compose.foundation.shape.CircleShape,
                                            )
                                            .padding(4.dp)
                                            .then(
                                                if (state.isMine && !state.uploadingAvatar) {
                                                    Modifier.clickable { pickerFor = "avatar" }
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            InitialsAvatar(
                                                name = state.name.ifBlank { "?" },
                                                imagePath = state.avatarPath,
                                                fontSize = 24.sp,
                                                modifier = Modifier.size(88.dp),
                                            )
                                            if (state.uploadingAvatar) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(88.dp)
                                                        .background(
                                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
                                                            androidx.compose.foundation.shape.CircleShape,
                                                        ),
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = androidx.compose.ui.graphics.Color.White,
                                                        modifier = Modifier.size(28.dp),
                                                    )
                                                }
                                            }
                                        }
                                        if (state.isMine && !state.uploadingAvatar) {
                                            Text(
                                                text = "📷",
                                                fontSize = 14.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(6.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = state.name.ifBlank { "Perfil" },
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FeedInk,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                val info = state.personalInfo
                                val subtitle = listOf(
                                    info?.religiousBeliefs?.takeIf { !it.isNullOrBlank() },
                                    info?.occupation?.takeIf { !it.isNullOrBlank() },
                                ).filterNotNull().joinToString(" · ")
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        fontSize = 13.sp,
                                        color = FeedMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                ) {
                                    Text(text = "${state.postCount}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = FeedInk)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Posts", fontSize = 13.sp, color = FeedMuted)
                                }
                            }
                        }
                    }

                    // ---- hobbies ----
                    state.hobbies?.let { h ->
                        SectionCard(title = "Hobbies e interesses") {
                            InfoRow("Hobbies", h.hobbies)
                            InfoRow("Séries favoritas", h.favouriteTvShows)
                            InfoRow("Filmes favoritos", h.favouriteMovies)
                            InfoRow("Jogos favoritos", h.favouriteGames)
                            InfoRow("Bandas / artistas", h.favouriteMusicBands)
                            InfoRow("Livros favoritos", h.favouriteBooks)
                            InfoRow("Escritores favoritos", h.favouriteWriters)
                            InfoRow("Outros interesses", h.otherInterests)
                        }
                    }

                    // ---- formação ----
                    SectionCard(title = "Educação e trabalho") {
                        if (state.education.isEmpty() && state.employment.isEmpty()) {
                            Text(text = "Nenhuma informação cadastrada.", fontSize = 13.sp, color = FeedMuted)
                        } else {
                            state.education.forEach { e ->
                                EntryRow(title = e.title, period = e.period, description = e.description)
                            }
                            state.employment.forEach { e ->
                                EntryRow(title = e.title, period = e.period, description = e.description)
                            }
                        }
                    }

                    // ---- info pessoal ----
                    state.personalInfo?.let { p ->
                        SectionCard(title = "Informações pessoais") {
                            InfoRow("Sobre mim", p.about)
                            InfoRow("Aniversário", p.birthday)
                            InfoRow("Natural de", p.birthplace)
                            val livesIn = listOf(p.city, p.state).filter { !it.isNullOrBlank() }.joinToString(", ")
                            InfoRow("Mora em", livesIn)
                            InfoRow("Ocupação", p.occupation)
                            InfoRow("Gênero", p.gender)
                            InfoRow("Status", p.status)
                            InfoRow("Email", p.email)
                            if (!p.website.isNullOrBlank()) {
                                LabeledLink(label = "Website", text = p.website) { openUrl(p.website) }
                            }
                            InfoRow("Telefone", p.phone)
                            InfoRow("Crenças", p.religiousBeliefs)
                            InfoRow("Posição política", p.politicalIncline)
                        }

                        val socials = listOf(
                            "Facebook" to p.facebook,
                            "Twitter" to p.twitter,
                            "Dribbble" to p.dribbble,
                        ).filter { !it.second.isNullOrBlank() }
                        if (socials.isNotEmpty()) {
                            SectionCard(title = "Outras redes") {
                                socials.forEach { (label, url) ->
                                    Text(
                                        text = "🔗 $label",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FeedBrandDark,
                                        modifier = Modifier
                                            .clickable { openUrl(url.orEmpty()) }
                                            .padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        pickerFor?.let { target ->
            AlertDialog(
                onDismissRequest = { pickerFor = null },
                title = {
                    Text(
                        text = if (target == "avatar") "Trocar foto de perfil" else "Trocar foto de capa",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "📷 Câmera",
                            fontSize = 16.sp,
                            color = FeedInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingCameraTarget = target
                                    pickerFor = null
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                                .padding(vertical = 12.dp),
                        )
                        Text(
                            text = "🖼️ Galeria",
                            fontSize = 16.sp,
                            color = FeedInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { pickerFor = null }) { Text(text = "Cancelar") }
                },
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FeedInk)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FeedMuted)
        Text(text = value, fontSize = 14.sp, color = FeedInk)
    }
}

@Composable
private fun EntryRow(title: String?, period: String?, description: String?) {
    if (title.isNullOrBlank() && description.isNullOrBlank()) return
    Column {
        if (!title.isNullOrBlank()) Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeedInk)
        if (!period.isNullOrBlank()) Text(text = period, fontSize = 12.sp, color = FeedMuted)
        if (!description.isNullOrBlank()) Text(text = description, fontSize = 13.sp, color = FeedInk)
    }
}

@Composable
private fun LabeledLink(label: String, text: String, onClick: () -> Unit) {
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FeedMuted)
        Text(
            text = text,
            fontSize = 14.sp,
            color = FeedBrandDark,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}
