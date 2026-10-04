# Espectra — App Mobile (Android)

Cliente Android nativo da rede social **Espectra** (mesma API do app web
AutiConecta e do backend .NET em `E:\Projetos\Espectra`).

Identidade visual: símbolo do infinito da neurodiversidade em branco sobre o
laranja da marca `#F97316` (fundo creme `#FFFFFBEB`).

## Stack

- **Kotlin** + **Jetpack Compose** (Material3) — UI 100% declarativa
- **Retrofit + Gson** + **OkHttp** (transitivo, sem dependências extras) —
  `Authorization: Bearer` em todas as chamadas (equivale ao
  `jwt.interceptor.ts` do web)
- `minSdk 33` · `targetSdk 37` · build com Gradle (`assembleDebug --offline`
  funciona: nenhuma dependência fora do cache)

## Funcionalidades (paridade com o web)

| Área | Tela | Inclui |
| ---- | ---- | ------ |
| Auth | Login, Cadastro, Esqueci a senha | Mesmas validações/mensagens do web; `DeviceInfo` + IP público (auditoria `LoginHistory`) |
| Feed | Feed | Composer Status/Mídia/Blog, sentimentos, visibilidade 🌍/👥/🔒 (+ amigos específicos), like otimista, comentários, excluir, compartilhar (sheet nativo), paginação infinita, pull-to-refresh |
| Feed | Descobrir | Clima (São Paulo), aniversário próximo, sugestões de amizade, atividades |
| Social | Amigos | Amigos com busca, pedidos recebidos (aceitar/recusar), enviados (cancelar), remover amigo |
| Social | Fotos | Álbuns: criar (título + visibilidade), grade, upload multiplo (seletor do sistema), visualizador, comentários, compartilhar com amigo, excluir foto/álbum |
| Perfil | About | Capa + avatar (troca por câmera/galeria com compressão, só perfil próprio), hobbies, educação/trabalho, info pessoal, redes sociais |
| Sistema | Ícone | Adaptive icon + PNGs `mdpi→xxxhdpi` (normal e round) |

Navegação: abas **Feed · Descobrir · Amigos · Fotos** após o login; nome do
autor (posts e comentários) abre o perfil, com `← Voltar`.

## Estrutura

```
app/src/main/
├── AndroidManifest.xml            # INTERNET + CAMERA + FileProvider
├── java/br/com/espectra/
│   ├── MainActivity.kt            # auth + abas + navegação perfil
│   ├── data/                      # Retrofit (Auth/Feed/Social/Profile/Album),
│   │                              # DTOs, SessionManager, ImageLoader (sem Coil),
│   │                              # ImageCompressor, WeatherRepository, DeviceInfoProvider
│   └── ui/
│       ├── login/ register/ forgot/
│       ├── feed/                  # FeedScreen, Composer, PostCard, Discover, AppTabBar
│       ├── friends/               # FriendsScreen
│       ├── photos/                # AlbumsScreen (álbuns)
│       └── profile/               # ProfileScreen (about)
└── res/                           # ícone Espectra, xml/file_paths (câmera)
```

## API

- Base: `https://agfapp.com/api/` (mesmos contratos do web: `auth/*`,
  `posts/*`, `friendrequests/*`, `activityfeed`, `settings`,
  `personalInformation`, `HobbiesAndInterests`, `education`, `employment`,
  `albums/*`, `photos/*`, `profile/*`)
- Clima: `api.weatherapi.com` (mesma chave do web)
- Upload de avatar/capa: `PUT profile/avatar|cover` com
  `{ imageData: number[] }` (bytes **sem sinal**, como `Array.from(Uint8Array)`)

## Build

```powershell
.\gradlew.bat :app:assembleDebug
# APK em app\build\outputs\apk\debug\app-debug.apk
```

Pré-requisito: Android SDK configurado (`local.properties`, não versionado).

## Notas

- DTOs de leitura usam campos nuláveis: o Gson injeta `null` mesmo em
  propriedades Kotlin não-nulas (causa de NPE já corrigida no perfil).
- Tema claro forçado (`darkTheme = false`): o design é creme/branco e o modo
  escuro do sistema quebrava as abas.
- Calêndário estático e contadores mockados do web (cidades, seguidores) foram
  propositalmente **não** portados.

## Roadmap

Timeline, chat/mensagens, vídeos, configurações da conta, edição dos campos do
perfil (hoje só leitura + troca de fotos).
