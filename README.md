# Android Mobile Practices


---

## Estructura del Proyecto

```
app/
├── MyApp.java                          ← @HiltAndroidApp (punto de entrada de Hilt)
├── di/
│   └── NetworkModule.java              ← @Module con @Provides para Retrofit y ApiService
├── data/
│   ├── model/
│   │   ├── PokemonListResponse.java
│   │   ├── PokemonResult.java
│   │   ├── PokemonDetail.java
│   │   ├── PokemonSprites.java
│   │   └── PokemonTypeSlot.java
│   └── network/
│       ├── RetrofitClient.java         ← Singleton manual (reemplazado por Hilt)
│       └── PokemonApiService.java      ← Interface Retrofit
└── ui/
    ├── MainActivity.java               ← @AndroidEntryPoint
    ├── auth/
    │   └── LoginFragment.java
    ├── home/
    │   └── HomeFragment.java
    └── pokemon/
        ├── PokemonListFragment.java    ← @AndroidEntryPoint + @Inject
        ├── PokemonDetailFragment.java  ← @AndroidEntryPoint + @Inject
        └── PokemonAdapter.java
```

---

## Configuración de Hilt

### 1. Dependencias en `gradle/libs.versions.toml`

```toml
[versions]
hilt = "2.59.2"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 2. `build.gradle.kts` (proyecto)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
}
```

### 3. `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)   // Java usa annotationProcessor (no kapt)
}
```

> **Nota:** `kapt` es para proyectos Kotlin. En proyectos Java se usa `annotationProcessor`.

---

## Componentes Hilt

### `@HiltAndroidApp` — MyApp.java

#### ¿Qué es `Application` y en qué se diferencia de una Activity?

Android siempre crea un objeto `Application` al arrancar la app, **antes** que cualquier Activity o Fragment. Antes de Hilt ya existía uno — era el default y no lo veías.

```
// Sin Hilt
Sistema Android
    └── crea MainActivity  ← primer código tuyo que corría

// Con Hilt
Sistema Android
    └── crea MyApp (Application)  ← primero, antes que todo
            └── crea MainActivity
```

`@HiltAndroidApp` le dice a Hilt que use `MyApp` como punto de inicialización del grafo de dependencias. Hilt necesita este lugar porque es el único punto de entrada garantizado — si se inicializara en `MainActivity` y tuvieras varias Activities, ¿cuál arrancaría el grafo?

```java
@HiltAndroidApp
public class MyApp extends Application {
}
```

Registrado en `AndroidManifest.xml`:
```xml
<application android:name=".MyApp" ...>
```

#### ¿Qué cambió concretamente?

| | Antes | Ahora con Hilt |
|---|---|---|
| Primer código que corre | `MainActivity.onCreate()` | `MyApp` (antes que todo) |
| Retrofit se crea en | `RetrofitClient` (singleton manual) | `NetworkModule.provideRetrofit()` |
| El Fragment obtiene el servicio | `RetrofitClient.getInstance().create(...)` | campo `@Inject` |
| Quién controla el ciclo de vida | Vos (con el `if (instance == null)`) | Hilt |

> Para el usuario la app arranca igual. La diferencia es interna: Hilt tomó el control del grafo de objetos.

---

### `@AndroidEntryPoint` — Activities y Fragments

Habilita la inyección en el componente Android.

```java
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity { ... }

@AndroidEntryPoint
public class PokemonListFragment extends Fragment { ... }
```

> Regla: si un Fragment usa `@Inject`, su Activity padre también debe tener `@AndroidEntryPoint`.

---

### `@Module` + `@InstallIn` — NetworkModule.java

Define cómo construir las dependencias.

```java
@Module
@InstallIn(SingletonComponent.class)   // Viven durante toda la app
public class NetworkModule {

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public PokemonApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(PokemonApiService.class);
    }
}
```

- `@Singleton`: Hilt crea **una sola instancia** y la reutiliza (equivale al patrón Singleton manual anterior).
- `SingletonComponent`: las dependencias viven mientras vive la `Application`.

---

### `@Inject` — Uso en Fragments

```java
@AndroidEntryPoint
public class PokemonListFragment extends Fragment {

    @Inject
    PokemonApiService apiService;   // Hilt inyecta esto automáticamente

    @Override
    public void onViewCreated(...) {
        // apiService ya está disponible, no hace falta crearlo
        apiService.getPokemon(20).enqueue(...);
    }
}
```

---

## Flujo de Inyección

```
MyApp (@HiltAndroidApp)
    └── NetworkModule (@Module @InstallIn(SingletonComponent))
            ├── provideRetrofit()       → Retrofit (Singleton)
            └── provideApiService()     → PokemonApiService (Singleton)
                        ↓
            PokemonListFragment (@AndroidEntryPoint)
                @Inject PokemonApiService apiService ✓

            PokemonDetailFragment (@AndroidEntryPoint)
                @Inject PokemonApiService apiService ✓
```

---

## Flujo de Navegación

```
MainActivity (NavHost)
└── LoginFragment → HomeFragment → PokemonListFragment → PokemonDetailFragment
```

---

## API utilizada

**PokéAPI** — `https://pokeapi.co/api/v2/`

| Endpoint | Descripción |
|---|---|
| `GET /pokemon?limit=20` | Lista de Pokémon |
| `GET /pokemon/{name}` | Detalle de un Pokémon |

---

## Tecnologías

| Librería | Uso |
|---|---|
| Hilt 2.59.2 | Inyección de dependencias |
| Retrofit 2.9.0 | Cliente HTTP |
| Gson | Deserialización JSON |
| Glide 4.16.0 | Carga de imágenes |
| Navigation Component 2.8.9 | Navegación entre Fragments |
