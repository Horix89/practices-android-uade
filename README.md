# feature/biometric — Biometría, Re-autenticación e Imagen de Perfil

Proyecto de prácticas en Android (Java). Este branch porta tres funcionalidades desde el repositorio [`mobile-practices-android`](https://github.com/Horix89/mobile-practices-android) (branches `feature/biometric`, `feature/biometric-reauth` y `feature/profile-image`):

1. **Biometría standalone** (sección 1) — pantalla de práctica con `BiometricPrompt`, accesible desde Home → **"Biometría"**.
2. **Re-autenticación biométrica en el Login** (sección 2) — el login recuerda al usuario y, en accesos siguientes, pide huella/PIN en vez del formulario; además la sesión se cierra sola si el backend devuelve 401.
3. **Selección de imagen de perfil** (sección 3) — elegir una foto de la galería y mostrarla recortada en círculo con Glide, accesible desde Home → Storage → **"Imagen de Perfil"**.

---

## 1. Biometría (standalone) — `BiometricFragment.java`

**Caso de uso:** práctica aislada de la API `androidx.biometric`, sin integrarla todavía al flujo de login. Se llega desde Home → **"Biometría"**.

### Conceptos teóricos

#### Los tres factores de autenticación

En cualquier sistema, la identidad de un usuario se puede verificar con tres tipos de factores:

| Factor | Nombre técnico | Ejemplos | Ventaja | Desventaja |
|--------|---------------|---------|---------|------------|
| **Algo que sabés** | Knowledge factor | PIN, contraseña, patrón | Fácil de implementar | Se olvida, se comparte, se adivina |
| **Algo que tenés** | Possession factor | Celular, tarjeta, token físico | Requiere robar el objeto | Se puede perder o robar |
| **Algo que sos** | Inherence factor | Huella, rostro, iris, voz | No se olvida ni se pierde | Si se filtra, no se puede "cambiar" |

> **MFA / 2FA:** combinar factores de **distinto tipo**. Dos contraseñas no son 2FA. El login de este proyecto (sección 2) combina "algo que sabés" (contraseña, en el primer ingreso) con "algo que sos" (huella, en los siguientes).

#### Clasificación de biometría en Android

Android clasifica los métodos biométricos en tres clases:

- **Class 3 (STRONG):** permite operaciones criptográficas. Ejemplo: sensor de huella certificado.
- **Class 2 (WEAK):** solo sirve para desbloquear la app, sin acceso a criptografía.
- **Class 1:** no se usa en aplicaciones de terceros.

Por eso este proyecto pide siempre `Authenticators.BIOMETRIC_STRONG`: es el único nivel con garantías suficientes como para, más adelante, liberar un token de sesión (sección 2).

#### Por qué AndroidX Biometric y no la API nativa

La API biométrica de Android cambió varias veces:

```
API 23 (Marshmallow)  →  FingerprintManager      — Solo huella. Hoy deprecado.
API 28 (Pie)          →  BiometricPrompt nativo   — Más moderno, pero solo versiones nuevas.
API 21+ (AndroidX)    →  androidx.biometric       — Unifica todo. Es la recomendada.
```

`AndroidX Biometric` unifica todo en una sola librería compatible desde API 21, por eso es la que se usa acá.

#### Enrolamiento

Una app **no puede usar biometría** si el usuario no tiene ningún método enrolado en el dispositivo. Ante esa situación hay dos opciones: deshabilitar el botón con un mensaje explicativo, o redirigir al usuario a los Ajustes del sistema.

### Dependencia
```toml
# gradle/libs.versions.toml
[versions]
biometric = "1.2.0-alpha05"
[libraries]
biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
```
```kotlin
// app/build.gradle.kts
implementation(libs.biometric)
```

### Paso 1 — `BiometricManager`: preguntar si la biometría está disponible

Antes de mostrar cualquier prompt hay que consultar el estado del sistema — si se muestra el prompt sin verificar, puede lanzar una excepción.

```java
private static final int ALLOWED_AUTHENTICATORS =
        Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;

BiometricManager manager = BiometricManager.from(requireContext());
int result = manager.canAuthenticate(ALLOWED_AUTHENTICATORS);

switch (result) {
    case BiometricManager.BIOMETRIC_SUCCESS:
        // Todo OK: hay hardware, está disponible y hay credenciales enroladas
        break;
    case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
        // El dispositivo no tiene sensor biométrico
        break;
    case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
        // El hardware existe pero no está disponible en este momento
        break;
    case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
        // No hay biometría ni PIN/patrón enrolados → ofrecer ir a Ajustes
        break;
}
```

**Estados posibles:**

| Constante | Significado | Acción en `BiometricFragment` |
|-----------|------------|-------------------------------|
| `BIOMETRIC_SUCCESS` | Listo para autenticar | Habilita `btnAuthenticate` |
| `ERROR_NO_HARDWARE` | Sin sensor biométrico | Deshabilita el botón |
| `ERROR_HW_UNAVAILABLE` | Sensor temporalmente no disponible | Deshabilita el botón |
| `ERROR_NONE_ENROLLED` | No hay biometría ni PIN enrolados | Muestra `btnGoToSettings` |

`BIOMETRIC_STRONG | DEVICE_CREDENTIAL` permite que el sistema acepte también el PIN/patrón del dispositivo como alternativa a la huella — útil en emuladores o dispositivos sin sensor físico.

**Ir a Ajustes si no hay enrolamiento:**
```java
startActivity(new Intent(Settings.ACTION_BIOMETRIC_ENROLL));
```

### Paso 2 — `PromptInfo`: configurar el diálogo del sistema

`PromptInfo` define el texto que ve el usuario en el diálogo de autenticación:

```java
BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
        .setTitle("Práctica Biometría")
        .setSubtitle("Confirmá tu identidad")
        .setDescription("Usá tu huella digital o el PIN del dispositivo")
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build();
```

> **Trampa frecuente:** si usás `DEVICE_CREDENTIAL`, **no podés** llamar a `.setNegativeButtonText()`. El botón de fallback ya es el PIN/patrón del dispositivo. Si querés un botón "Cancelar" explícito, usá solo `BIOMETRIC_STRONG` sin `DEVICE_CREDENTIAL`.

### Paso 3 — `BiometricPrompt`: lanzar el prompt y escuchar los tres callbacks

`BiometricPrompt` necesita tres cosas: el Fragment (o Activity), un `Executor` para el hilo de los callbacks, y un `AuthenticationCallback`:

```java
Executor executor = ContextCompat.getMainExecutor(requireContext());

BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
        new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                tvStatus.setText("Autenticacion exitosa!");
            }
            @Override
            public void onAuthenticationFailed() {
                tvStatus.setText("Intento fallido. Reintenta.");
            }
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                tvStatus.setText("Error: " + errString);
            }
        });
biometricPrompt.authenticate(promptInfo);
```

**Los tres callbacks:**

| Callback | Cuándo se dispara | El prompt... |
|----------|-------------------|-------------|
| `onAuthenticationSucceeded` | Biometría validada correctamente | Se cierra |
| `onAuthenticationFailed` | Intento no coincide | **Sigue abierto** — no hay que cerrarlo ni deshabilitar nada acá |
| `onAuthenticationError` | Error del sistema o cancelación del usuario | Se cierra (es terminal) |

### Flujo completo de navegación

```
Home
  └─► BiometricFragment
        │
        ├─ [BIOMETRIC_SUCCESS]       → botón habilitado
        ├─ [ERROR_NO_HARDWARE]       → botón deshabilitado
        ├─ [ERROR_HW_UNAVAILABLE]    → botón deshabilitado
        └─ [ERROR_NONE_ENROLLED]     → botón "Ir a Configuración" visible
              │
              └─► [click Autenticar con Biometría]
                    │
                    ├─ onAuthenticationSucceeded → "Autenticacion exitosa!"
                    ├─ onAuthenticationFailed    → "Intento fallido. Reintenta."
                    └─ onAuthenticationError     → "Error: {mensaje}"
```

### Archivos modificados / creados

| Archivo | Cambio |
|---------|--------|
| `gradle/libs.versions.toml` | Versión y alias de `androidx.biometric` |
| `app/build.gradle.kts` | `implementation(libs.biometric)` |
| `app/src/main/res/layout/fragment_biometric.xml` | Layout nuevo con `tvStatus`, `btnAuthenticate`, `btnGoToSettings` |
| `app/src/main/java/.../ui/biometric/BiometricFragment.java` | Fragment nuevo con la lógica de los 3 pasos |
| `app/src/main/res/navigation/home_nav_graph.xml` | Destino y acción `action_home_to_biometric` |
| `app/src/main/res/layout/fragment_home.xml` | Botón "Biometría" nuevo |
| `app/src/main/java/.../ui/home/HomeFragment.java` | Click handler del nuevo botón |

---

## 2. Re-autenticación biométrica en el Login — `LoginFragment.java`, `TokenManager.java`, `AuthEventBus.java`

**Caso de uso:** integrar la biometría al flujo real de autenticación (no solo como demo aislada) y resolver el problema que la sección 1 deja abierto: ¿de qué sirve validar la huella si después no se hace nada con esa confirmación? Acá el resultado del `BiometricPrompt` efectivamente **reemplaza** al formulario de usuario/contraseña en los accesos siguientes.

### Por qué no alcanza con la biometría "sola"

El sensor biométrico solo responde `true`/`false` — "sí, es la misma persona que enroló la huella" — pero no sabe nada sobre la sesión del usuario en el backend. Por eso la biometría acá **no reemplaza al token**: solo actúa como el "candado" que decide si se le entrega al usuario el token que ya se había guardado. Esto es lo mismo que hace, por ejemplo, la app de un banco cuando pide huella "para ver el saldo": la sesión ya existe, la huella solo autoriza mostrarla.

### Flujo

```
LoginFragment.onViewCreated()
        │
        ▼
tokenManager.isBiometricEnabled() ?
        │
   ┌────┴────┐
   NO         SÍ
   │           │
   ▼           ▼
layoutCredentials    layoutBiometric → BiometricPrompt
   │                        │
   ▼                        ├─ onAuthenticationSucceeded → getEncryptedToken() → saveToken(...) → Home
[Ingresar]                  ├─ onAuthenticationFailed    → (el prompt sigue abierto, sin acción)
   │                        └─ onAuthenticationError     → showCredentialForm() (fallback)
   ▼
saveToken("fake-token-abc123")
   │
   ▼
AlertDialog "¿Activar biometría?"
   │
   ├─ Sí, activar  → setBiometricEnabled(true) + saveEncryptedToken(...) → Home
   └─ No, gracias  →  Home (sin activar nada; la próxima vez vuelve a pedir credenciales)
```

1. **Primer login:** se muestra el formulario de credenciales (`layoutCredentials`). Al presionar "Ingresar" se guarda un token simulado y se pregunta con un `AlertDialog` si el usuario quiere activar el acceso biométrico.
2. **Si acepta:** se guarda `biometric_enabled = true` y una copia del token cifrada con `EncryptedSharedPreferences` (`TokenManager.saveEncryptedToken(...)`).
3. **Logins siguientes:** `onViewCreated` detecta `tokenManager.isBiometricEnabled()` y muestra directamente el `BiometricPrompt` (`layoutBiometric`) en vez del formulario. Al validar la huella, se recupera el token cifrado y se navega a Home. Si el usuario cancela o el hardware no está disponible, cae de nuevo al formulario de credenciales (`btnUsarCredenciales` también permite volver manualmente).

```java
if (tokenManager.isBiometricEnabled()) {
    showBiometricPrompt();
} else {
    showCredentialForm();
}
```

### `TokenManager.java` — dos capas de almacenamiento

El `TokenManager` ya existente guardaba el token de sesión en `SharedPreferences` plano (`auth_prefs.xml`). Esta feature lo amplía agregando:

```java
// Flag de activación
public boolean isBiometricEnabled();
public void setBiometricEnabled(boolean enabled);

// Token cifrado — mismo mecanismo de EncryptedSharedPreferences (MasterKey del
// Android Keystore + AES256_SIV para claves, AES256_GCM para valores),
// pero en un archivo separado: "encrypted_auth_prefs"
public void saveEncryptedToken(String token);
public String getEncryptedToken();
```

| Copia del token | Dónde vive | Para qué sirve |
|------------------|-----------|-----------------|
| Plano (`token`) | `auth_prefs.xml` | Sesión activa — se lee en cada request (interceptor de `NetworkModule`) |
| Cifrado (`encrypted_token`) | `encrypted_auth_prefs.xml` (EncryptedSharedPreferences) | Se guarda solo si el usuario activó biometría; se recupera **después** de validar la huella |

La razón de tener dos copias: el token plano necesita ser rápido de leer (se agrega a cada header `Authorization`), pero eso lo hace legible por cualquiera con acceso al almacenamiento interno. La copia cifrada es la que se usa para *restaurar* la sesión tras el prompt biométrico — así, aunque alguien lea el archivo en disco, no puede reactivar la sesión sin pasar antes por el sensor.

### Cierre de sesión automático ante un 401 — `AuthEventBus.java`

Para que la re-autenticación tenga sentido real, la sesión tiene que poder "vencer" — si nunca expirara, pedir biometría en cada apertura de la app sería solo cosmético. Se agregó un event bus con Hilt (`@Singleton`) basado en `LiveData`, que permite que una clase sin acceso a la UI (el interceptor de red) le avise a la Activity que la sesión murió:

```java
@Singleton
public class AuthEventBus {
    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>();
    public LiveData<Boolean> getSessionExpired() { return sessionExpired; }
    public void emitSessionExpired() { sessionExpired.postValue(true); } // postValue: thread-safe desde el hilo de red
}
```

`postValue(...)` (y no `setValue(...)`) es obligatorio acá porque el interceptor de OkHttp corre en un hilo de background, nunca en el main thread.

El interceptor de OkHttp en `NetworkModule.java` detecta la respuesta 401, limpia el token y emite el evento:

```java
okhttp3.Response response = chain.proceed(request);
if (response.code() == 401) {
    tokenManager.clearToken();
    authEventBus.emitSessionExpired();
}
return response;
```

`MainActivity.java` observa el `LiveData` y navega de vuelta a `auth_nav_graph` (limpiando el back stack) apenas se emite el evento:

```java
authEventBus.getSessionExpired().observe(this, expired -> {
    if (Boolean.TRUE.equals(expired)) {
        navController.navigate(R.id.auth_nav_graph, null,
                new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build());
    }
});
```

Nótese que `biometric_enabled` **no** se borra al expirar la sesión — el usuario vuelve a la pantalla de login y, si ya lo había activado, el próximo ingreso vuelve a pedir biometría en lugar del formulario (solo cambia el token, no la preferencia del usuario).

### Archivos modificados / creados

| Archivo | Cambio |
|---------|--------|
| `data/local/TokenManager.java` | Agrega `biometric_enabled` y token cifrado (`EncryptedSharedPreferences`) |
| `di/AuthEventBus.java` | Nuevo — `@Singleton` con `LiveData<Boolean> sessionExpired` |
| `di/NetworkModule.java` | El interceptor detecta HTTP 401, limpia el token y emite el evento |
| `ui/MainActivity.java` | Inyecta `AuthEventBus` y navega a `auth_nav_graph` cuando la sesión expira |
| `ui/auth/LoginFragment.java` | Muestra `BiometricPrompt` o el formulario según `isBiometricEnabled()`; ofrece activarlo tras el primer login |
| `res/layout/fragment_login.xml` | Agrega `layoutCredentials` / `layoutBiometric` (visibilidad alternada) |

---

## 3. Imagen de Perfil (Galería + Glide) — `ProfileImageFragment.java`

**Caso de uso:** elegir una foto de la galería del dispositivo, persistir la selección y mostrarla con recorte circular. Cubre permisos runtime, `ActivityResultLauncher`, `Uri` de content provider y carga de imágenes con Glide. Se llega desde Home → Storage → **"Imagen de Perfil"**.

### Flujo completo

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 1. Declarar permiso   →   2. Pedir en runtime   →   3. Abrir galería     │
│    (Manifest)               (checkSelfPermission)      (GetContent)      │
│                                                                           │
│ 6. Mostrar con Glide  ←   5. Guardar la Uri     ←   4. Recibir Uri       │
│    (circleCrop)              (SharedPreferences)        (launcher callback)│
└──────────────────────────────────────────────────────────────────────────┘
```

### Paso 1 — Declarar el permiso en el Manifest

Sin declarar el permiso en el Manifest, el sistema lo bloquea directamente — aunque el código lo pida en runtime, nunca se concede.

Android fragmentó el permiso de lectura de imágenes en versiones recientes para dar al usuario más control granular:

| Versión de Android | API Level | Permiso requerido | Razón del cambio |
|---------------------|-----------|--------------------|-------------------|
| Android 12 e inferior | ≤ 32 | `READ_EXTERNAL_STORAGE` | Permiso amplio: acceso a todo el almacenamiento externo |
| Android 13+ | ≥ 33 | `READ_MEDIA_IMAGES` | Permiso granular: solo imágenes, sin acceso a otros archivos |

```xml
<!-- app/src/main/AndroidManifest.xml -->
<uses-permission
    android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33" />
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

Con `minSdkVersion="33"` en el primero y `maxSdkVersion="32"` en el segundo, el sistema aplica automáticamente el permiso correcto según la versión del dispositivo.

### Paso 2 — Pedir el permiso en runtime

Desde Android 6.0 (API 23), los permisos **peligrosos** (los que acceden a datos sensibles) requieren aprobación explícita del usuario en el momento en que la app los necesita — declararlos en el Manifest ya no alcanza.

**Detectar la versión y verificar si ya está otorgado:**
```java
// Build.VERSION_CODES.TIRAMISU == 33 (Android 13)
String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ? Manifest.permission.READ_MEDIA_IMAGES
        : Manifest.permission.READ_EXTERNAL_STORAGE;

if (ContextCompat.checkSelfPermission(requireContext(), permission)
        == PackageManager.PERMISSION_GRANTED) {
    openGallery();               // ya lo teníamos de una sesión anterior
} else {
    permissionLauncher.launch(permission); // dispara el diálogo del sistema
}
```

**`ActivityResultLauncher`** reemplaza al viejo `onActivityResult(requestCode, ...)`. Se declara como campo del Fragment (debe existir antes de `onStart()`) y se registra con un **contrato** (qué tipo de operación se lanza) más un **callback** (qué hacer con el resultado):

```java
private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), // contrato: pedir 1 permiso
                isGranted -> {                                   // callback: resultado booleano
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }
                });
```

`RequestPermission` define: **Input** `String` (el permiso a pedir) → **Output** `Boolean` (concedido o no).

**Flujo del permiso:**
```
click en "Seleccionar imagen"
        │
        ▼
checkSelfPermission()
        │
   ┌────┴────┐
GRANTED     DENIED
   │           │
   ▼           ▼
openGallery()  permissionLauncher.launch(permission) → diálogo del sistema
                    │
               ┌────┴────┐
           Permitir    Denegar
               │           │
               ▼           ▼
          openGallery()   Toast "Permiso denegado"
```

### Paso 3 — Abrir la galería

Se usa el mismo mecanismo del paso 2, con otro contrato: `ActivityResultContracts.GetContent`.

```java
private final ActivityResultLauncher<String> galleryLauncher =
        registerForActivityResult(
                new ActivityResultContracts.GetContent(), // contrato: elegir contenido por MIME type
                uri -> {                                  // callback: Uri de la imagen elegida
                    if (uri != null) {
                        saveAndDisplay(uri);
                    }
                });

private void openGallery() {
    galleryLauncher.launch("image/*"); // filtro MIME: solo imágenes
}
```

`GetContent` define: **Input** `String` (MIME type, `"image/*"`) → **Output** `Uri` (la imagen elegida, o `null` si se canceló). Delega en la app de galería instalada en el sistema (Google Photos, Samsung Gallery, etc.) — no hace falta declarar ningún `<intent-filter>` propio.

### Paso 4 — Recibir la `Uri` en el callback

Con `GetContent`, el callback recibe la `Uri` directamente, sin `resultCode` ni `Intent` de respuesta que procesar:

```java
uri -> {
    if (uri != null) {   // null si el usuario canceló
        saveAndDisplay(uri);
    }
}
```

**Qué es una `Uri` acá:** no es una ruta de archivo como `/sdcard/DCIM/foto.jpg` — es un identificador que apunta a un recurso gestionado por un `ContentProvider`:

```
content://media/external/images/media/42
│         │                            │
│         │                            └── ID del recurso en MediaStore
│         └── Autoridad: "media" (proveedor de contenido de Android)
└── Esquema: "content" (datos gestionados por un ContentProvider)
```

Glide entiende este esquema nativamente: puede cargar la imagen directo desde la `Uri`, sin copiarla ni convertirla.

### Paso 5 — Persistir la `Uri` (no la imagen)

La `Uri` que llega al callback es un objeto en memoria — no sobrevive al cierre de la app. Para que la selección persista entre sesiones, se guarda como `String` en `SharedPreferences`:

```java
private void saveAndDisplay(Uri uri) {
    SharedPreferences prefs = requireContext()
            .getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
    prefs.edit().putString("image_uri", uri.toString()).apply();
    displayImage(uri);
}

private void loadSavedImage() {
    SharedPreferences prefs = requireContext()
            .getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
    String uriString = prefs.getString("image_uri", null);
    if (uriString != null) {
        displayImage(Uri.parse(uriString)); // String → Uri
    }
}
```

La imagen real **no** se copia al sandbox de la app — sigue viviendo en la galería del dispositivo; la app solo recuerda la referencia.

### Paso 6 — Mostrar con Glide y `circleCrop()`

Cargar una imagen "a mano" requiere manejar varios problemas a la vez:

| Problema | Solución de Glide |
|----------|-------------------|
| Decodificar una imagen grande en el hilo principal | Carga en background automáticamente |
| El Fragment se destruye antes de que termine de cargar | Cancela la carga sola (`Glide.with(this)` se ata al ciclo de vida) |
| Cargar la misma imagen repetidas veces | Caché en memoria y disco |
| Recortar en círculo, bordes redondeados, etc. | Transformaciones integradas |

```java
private void displayImage(Uri uri) {
    Glide.with(this)   // vincula la carga al ciclo de vida del Fragment
         .load(uri)    // fuente: la Uri de la galería
         .circleCrop() // recorta la imagen en forma circular con anti-aliasing
         .into(ivProfile); // destino: el ImageView
}
```

Pasar `this` (el Fragment) en vez de `getApplicationContext()` es lo que le permite a Glide pausar/cancelar la carga si la pantalla se cierra antes de que termine.

### Dónde se persiste físicamente

```
/data/data/com.example.activitiesandviews/shared_prefs/profile_prefs.xml
```
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="image_uri">content://media/external/images/media/42</string>
</map>
```
La imagen en sí permanece en el almacenamiento compartido de la galería (`content://media/...`), fuera del sandbox de la app — solo la referencia (`Uri`) vive en `profile_prefs.xml`, en texto plano.

**Cómo observarlo en Android Studio:**
1. `View` → `Tool Windows` → `Device File Explorer`
2. Navegar a: `data/data/com.example.activitiesandviews/shared_prefs/`
3. Doble click en `profile_prefs.xml` — se ve la `Uri` guardada en texto plano

### Archivos modificados / creados

| Archivo | Cambio |
|---------|--------|
| `app/src/main/AndroidManifest.xml` | Permisos `READ_MEDIA_IMAGES` y `READ_EXTERNAL_STORAGE` |
| `ui/storage/ProfileImageFragment.java` | Nuevo — implementación de los 6 pasos |
| `res/layout/fragment_profile_image.xml` | Layout nuevo con `ImageView` y `Button` |
| `res/navigation/home_nav_graph.xml` | Destino y acción `action_storageMenu_to_profileImage` |
| `res/layout/fragment_storage_menu.xml` | Botón "Imagen de Perfil — Galería + Glide" agregado, y envuelto en `ScrollView` para que los 6 botones del menú entren en pantalla |
| `ui/storage/StorageMenuFragment.java` | Handler del nuevo botón |

---

## Resumen de persistencia introducida en este branch

| Método | Ruta en el dispositivo | Formato | Legible a simple vista |
|--------|------------------------|---------|------------------------|
| Token de sesión (ya existente) | `.../shared_prefs/auth_prefs.xml` | XML texto plano | Sí |
| Token cifrado + flag biométrico (nuevo) | `.../shared_prefs/encrypted_auth_prefs.xml` | XML cifrado | No |
| Imagen de perfil — referencia (nuevo) | `.../shared_prefs/profile_prefs.xml` | XML texto plano (solo la `Uri`) | Sí |

---

## Arquitectura

```
app/
├── di/
│   ├── NetworkModule.java          # Hilt: OkHttp (header Authorization + detección de 401), Retrofit
│   └── AuthEventBus.java           # Hilt @Singleton — LiveData<Boolean> sessionExpired
├── data/local/
│   └── TokenManager.java           # token plano (auth_prefs) + flag biométrico + token cifrado (encrypted_auth_prefs)
└── ui/
    ├── MainActivity.java              # observa AuthEventBus y navega a auth_nav_graph si la sesión expira
    ├── auth/
    │   └── LoginFragment.java         # formulario de credenciales o BiometricPrompt, según biometric_enabled
    ├── biometric/
    │   └── BiometricFragment.java     # práctica standalone de BiometricManager + BiometricPrompt
    ├── home/
    │   └── HomeFragment.java          # botón "Biometría" hacia BiometricFragment
    └── storage/
        ├── StorageMenuFragment.java   # botón "Imagen de Perfil" hacia ProfileImageFragment
        └── ProfileImageFragment.java  # selector de galería (Activity Result API) + Glide circleCrop
```
