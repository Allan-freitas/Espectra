package br.com.espectra.ui.profile

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.espectra.data.ApiClient
import br.com.espectra.data.EducationEntry
import br.com.espectra.data.EmploymentEntry
import br.com.espectra.data.Hobbies
import br.com.espectra.data.ImageCompressor
import br.com.espectra.data.ImageLoader
import br.com.espectra.data.ImageUploadRequest
import br.com.espectra.data.PersonalInformation
import br.com.espectra.data.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ProfileUiState(
    val loading: Boolean = true,
    val name: String = "",
    val isMine: Boolean = false,
    /** Caminho/url do avatar (do feed/sessão) ou endpoint profile/avatar?userId=. */
    val avatarPath: String? = null,
    /** Capa (da sessão) ou endpoint profile/cover?userId=. */
    val coverPath: String? = null,
    val postCount: Int = 0,
    val personalInfo: PersonalInformation? = null,
    val hobbies: Hobbies? = null,
    val education: List<EducationEntry> = emptyList(),
    val employment: List<EmploymentEntry> = emptyList(),
    val message: String? = null,
    val uploadingAvatar: Boolean = false,
    val uploadingCover: Boolean = false,
)

/** Espelha AboutComponent + ProfileHeaderComponent (somente leitura). */
class ProfileViewModel(
    private val userId: String?,
    private val fallbackName: String?,
    private val session: SessionManager,
) : ViewModel() {

    var uiState = mutableStateOf(
        ProfileUiState(
            name = fallbackName ?: "",
            isMine = (userId == null) || (userId == session.getUserId()),
            avatarPath = session.getUserAvatar(),
            coverPath = session.getUserCover(),
        ),
    )
        private set

    init {
        load()
    }

    private fun update(transform: (ProfileUiState) -> ProfileUiState) {
        uiState.value = transform(uiState.value)
    }

    fun consumeMessage() = update { it.copy(message = null) }

    // ---------- troca de foto (só perfil próprio; espelha profile.service) ----------

    fun uploadAvatar(raw: ByteArray) {
        if (uiState.value.uploadingAvatar) return
        viewModelScope.launch {
            update { it.copy(uploadingAvatar = true) }
            try {
                val body = ImageUploadRequest(ImageCompressor.compress(raw).map { b -> b.toInt() and 0xFF })
                val res = ApiClient.profileApi.uploadAvatar(body)
                if (!res.isSuccessful) throw HttpException(res)
                val base = session.getUserAvatar()
                    ?: "${ApiClient.API_ORIGIN}/api/profile/avatar"
                update {
                    ImageLoader.evict(it.avatarPath)
                    it.copy(
                        uploadingAvatar = false,
                        avatarPath = "$base?t=${System.currentTimeMillis()}",
                        message = "Foto de perfil atualizada!",
                    )
                }
            } catch (_: Exception) {
                update { it.copy(uploadingAvatar = false, message = "Erro ao fazer upload da foto de perfil. Tente novamente.") }
            }
        }
    }

    fun uploadCover(raw: ByteArray) {
        if (uiState.value.uploadingCover) return
        viewModelScope.launch {
            update { it.copy(uploadingCover = true) }
            try {
                val body = ImageUploadRequest(ImageCompressor.compress(raw).map { b -> b.toInt() and 0xFF })
                val res = ApiClient.profileApi.uploadCover(body)
                if (!res.isSuccessful) throw HttpException(res)
                val base = session.getUserCover()
                    ?: "${ApiClient.API_ORIGIN}/api/profile/cover"
                update {
                    ImageLoader.evict(it.coverPath)
                    it.copy(
                        uploadingCover = false,
                        coverPath = "$base?t=${System.currentTimeMillis()}",
                        message = "Foto de capa atualizada!",
                    )
                }
            } catch (_: Exception) {
                update { it.copy(uploadingCover = false, message = "Erro ao fazer upload da foto de capa. Tente novamente.") }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            update { it.copy(loading = true) }
            val currentUserId = userId
            val mine = currentUserId == null || currentUserId == session.getUserId()
            val queryId = if (mine) null else currentUserId
            try {
                val info = async { ApiClient.profileApi.getPersonalInfo(queryId) }
                val hobbies = async { runCatching { ApiClient.profileApi.getHobbies(queryId) }.getOrNull() }
                val education = async { runCatching { ApiClient.profileApi.getEducation(queryId) }.getOrDefault(emptyList()) }
                val employment = async { runCatching { ApiClient.profileApi.getEmployment(queryId) }.getOrDefault(emptyList()) }
                val count = async {
                    runCatching {
                        if (mine) ApiClient.profileApi.getPostCount()
                        else ApiClient.profileApi.getPostCountByUser(currentUserId)
                    }.getOrDefault(0)
                }
                val personalInfo = info.await()
                val hobbiesValue = hobbies.await()
                val educationList = education.await()
                val employmentList = employment.await()
                val postCountValue = count.await()
                val fullName = "${personalInfo.firstName.orEmpty()} ${personalInfo.lastName.orEmpty()}".trim()
                update {
                    it.copy(
                        loading = false,
                        isMine = mine,
                        name = fullName.ifBlank { fallbackName ?: "" },
                        personalInfo = personalInfo,
                        hobbies = hobbiesValue,
                        education = educationList,
                        employment = employmentList,
                        postCount = postCountValue,
                        avatarPath = if (mine) session.getUserAvatar() else it.avatarPath
                            ?: "${ApiClient.API_ORIGIN}/api/profile/avatar?userId=$currentUserId",
                        coverPath = if (mine) session.getUserCover() else it.coverPath
                            ?: "${ApiClient.API_ORIGIN}/api/profile/cover?userId=$currentUserId",
                    )
                }
            } catch (_: Exception) {
                update { it.copy(loading = false, message = "Falha ao carregar o perfil.") }
            }
        }
    }
}

class ProfileViewModelFactory(
    private val userId: String?,
    private val fallbackName: String?,
    private val session: SessionManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(userId, fallbackName, session) as T
    }
}
