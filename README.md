# Zireva — app Android nativa (Kotlin)

App de música real e instalable (.apk): busca y reproduce desde **YouTube**
(streaming, catálogo enorme) y **Jamendo** (libre de derechos, descargable
para offline), con ecualizador nativo real, idioma automático del sistema,
e interfaz "liquid glass".

## ⚖️ Legal — igual que en la versión web
- **YouTube**: reproducido con su reproductor oficial embebido en un WebView
  (`YoutubeWebPlayer.kt`) — es el método soportado desde que Google descontinuó
  la YouTube Android Player API nativa. El audio nunca sale de ese WebView, así
  que no hay forma de aplicarle ecualizador ni de descargarlo. Es una
  restricción real de la plataforma, no un límite artificial.
- **Jamendo**: catálogo con licencia explícita para streaming y, en muchos
  casos, descarga. Lo descargado se guarda de verdad en el almacenamiento del
  teléfono (`filesDir/tracks/`) + Room, y se reproduce con ExoPlayer +
  `android.media.audiofx.Equalizer` nativo — DSP real, no decorativo.
- No incluye ni incluirá extracción de audio de YouTube (yt-dlp o similar).

## 🔑 Configuración (gratis) — para que la app ya venga con la clave

Tienes dos rutas, según cómo compiles:

### A) Compilando localmente (Android Studio / tu PC)
1. Copia `local.properties.example` → `local.properties` (mismo lugar, raíz del proyecto)
2. Rellena con tus claves reales:
   ```
   youtube.api.key=TU_CLAVE_AQUI
   jamendo.client.id=TU_CLIENT_ID_AQUI
   ```
3. `local.properties` está en `.gitignore` — nunca se sube a GitHub, aunque hagas commit de todo lo demás.
4. Compilas normal y la app ya arranca con esas claves, sin pasar por Ajustes.

### B) Compilando con GitHub Actions (recomendado para tu caso)
Como el repo es público, la clave **no puede ir en el código** — la guardas como
Secret del repositorio, que GitHub cifra y nunca muestra en el código fuente:

1. En tu repo: **Settings → Secrets and variables → Actions → New repository secret**
2. Crea uno llamado `YOUTUBE_API_KEY` con tu clave de YouTube
3. Crea otro llamado `JAMENDO_CLIENT_ID` con tu Client ID de Jamendo
4. Vuelve a correr el workflow (push nuevo, o "Re-run jobs" en Actions)
5. El `.apk` que se genera ya viene con las claves incluidas — nadie en Ajustes
   necesita escribir nada, y nadie que vea tu repo puede ver las claves.

Cómo conseguir las claves (gratis, ~5 min cada una):
- **YouTube Data API key**: console.cloud.google.com → crea proyecto → habilita
  "YouTube Data API v3" → Credenciales → Crear credenciales → Clave de API
- **Jamendo Client ID**: devportal.jamendo.com → crea cuenta → "Create a new
  application" → copia el Client ID


## 🚀 Compilar el APK con GitHub Actions
1. Sube esta carpeta completa (`verdor-android/`, con todo su contenido, tal
   cual) como la raíz de un repositorio de GitHub, rama `main`.
2. El workflow `.github/workflows/build-apk.yml` corre automáticamente en
   cada push: instala JDK 17, el SDK de Android y Gradle, y compila
   `assembleDebug`.
3. Ve a la pestaña **Actions** del repo → la corrida más reciente → al
   terminar, baja el artefacto `zireva-debug-apk` (es un .zip que contiene el
   `.apk`).
4. Instálalo en tu teléfono (activa "Instalar apps de fuentes desconocidas"
   si Android lo pide) o usa `adb install app-debug.apk`.

> Esto genera un **APK de debug**, listo para probar en tu propio teléfono.
> Para publicarlo en Google Play necesitarías firmarlo con una key de
> release (`assembleRelease` + keystore) — puedo agregarlo cuando quieras
> dar ese paso.

## 🎨 Ícono
El ícono que subiste (`Icono.png`, morado, "ZIREVA") ya está integrado como
ícono adaptativo (`mipmap-anydpi-v26/ic_launcher.xml`) + versiones clásicas en
todas las densidades (mdpi a xxxhdpi). El nombre visible de la app se cambió
a **Zireva** en `strings.xml` (ES y EN). El paquete interno sigue siendo
`com.verdor.musica` — cámbialo si quieres que coincida también ahí.

## 🎚️ Ecualizador
`PlayerManager.kt` usa `android.media.audiofx.Equalizer` enganchado al
`audioSessionId` real del ExoPlayer — mueve las bandas de graves/medios/agudos
sobre el audio que efectivamente suena, en dispositivos que soportan el
efecto (la gran mayoría).

## 🗂️ Estructura
```
app/src/main/kotlin/com/verdor/musica/
  MainActivity.kt        → navegación + WebView de YouTube + mini player
  AppViewModel.kt         → estado compartido (reproducción, búsqueda, EQ)
  VerdorApp.kt             → Application (settings + repo de descargas)
  data/                    → Room (biblioteca offline) + DataStore (ajustes)
  network/                 → Retrofit: YouTube Data API + Jamendo API
  player/                  → ExoPlayer+Equalizer, y el WebView de YouTube
  download/                → descarga real de Jamendo a almacenamiento local
  ui/screens/               → Inicio, Buscar, Biblioteca, Ajustes, Reproductor
  ui/components/            → mini player, nav inferior, portadas generadas
  ui/theme/                 → colores, tipografía, panel "liquid glass"
```

## Idioma
Android usa automáticamente `values-en/strings.xml` si el sistema está en
inglés, y `values/strings.xml` (español) en cualquier otro caso — sin
código adicional, es el comportamiento nativo de Android.
