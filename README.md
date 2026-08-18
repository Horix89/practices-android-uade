# Clase 3 — API REST & Retrofit

## ¿Qué agregamos en esta clase?

Conectamos la app a una API pública real: **PokeAPI** (`https://pokeapi.co/api/v2/`).

El flujo nuevo es:

```
Login → Home → "Ver Pokemones" → Lista de pokemones → Tap en uno → Detalle
```

Sin DI. Retrofit se instancia y se usa directamente desde los Fragments.

---

## Dependencias nuevas

```toml
# gradle/libs.versions.toml
[versions]
retrofit = "2.9.0"
glide    = "4.16.0"

[libraries]
retrofit      = { group = "com.squareup.retrofit2", name = "retrofit",        version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson",  version.ref = "retrofit" }
glide         = { group = "com.github.bumptech.glide", name = "glide",        version.ref = "glide" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.retrofit)
implementation(libs.retrofit.gson)
implementation(libs.glide)
```

También se agregó el permiso de internet en `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Estructura de la capa de red

```
data/
├── model/
│   ├── PokemonResult.java         ← { name, url } — un ítem de la lista
│   ├── PokemonListResponse.java   ← { count, results: List<PokemonResult> }
│   ├── PokemonDetail.java         ← { name, height, weight, sprites, types }
│   ├── PokemonSprites.java        ← sprites.other.official-artwork.front_default
│   └── PokemonTypeSlot.java       ← types[].type.name
└── network/
    ├── PokemonApiService.java     ← interfaz con @GET endpoints
    └── RetrofitClient.java        ← singleton que construye la instancia de Retrofit
```

---

## Paso 1 — RetrofitClient (Singleton)

No usamos DI. `RetrofitClient` es una clase con un método estático `getInstance()` que crea la instancia una sola vez y la reutiliza:

```java
public class RetrofitClient {

    private static final String BASE_URL = "https://pokeapi.co/api/v2/";
    private static Retrofit instance;

    public static Retrofit getInstance() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }
}
```

> **¿Por qué singleton?** Retrofit y OkHttp son objetos pesados — tienen thread pools, caché de conexiones, etc. Crear uno por request sería un desperdicio. El singleton garantiza que se crea una sola vez y se reutiliza en toda la app.

---

## Paso 2 — PokemonApiService (Interfaz)

Retrofit convierte una interfaz Java con anotaciones en llamadas HTTP reales:

```java
public interface PokemonApiService {

    @GET("pokemon")
    Call<PokemonListResponse> getPokemon(@Query("limit") int limit);
    // → GET https://pokeapi.co/api/v2/pokemon?limit=20

    @GET("pokemon/{name}")
    Call<PokemonDetail> getPokemonDetail(@Path("name") String name);
    // → GET https://pokeapi.co/api/v2/pokemon/bulbasaur
}
```

| Anotación | Qué hace |
|---|---|
| `@GET("pokemon")` | Define el path relativo a la base URL |
| `@Query("limit")` | Agrega un query param: `?limit=20` |
| `@Path("name")` | Reemplaza `{name}` en la URL con el valor del parámetro |
| `Call<T>` | Representa la llamada HTTP. `T` es el tipo que Gson va a parsear |

---

## Paso 3 — Hacer una petición HTTP: `enqueue()`

### ¿Cómo funciona `enqueue` a nivel Java?

Java tiene un concepto llamado **threads** (hilos). Por defecto, todo el código de una app Android corre en un único hilo: el **Main Thread** (también llamado UI Thread). Este hilo es el responsable de dibujar la pantalla y responder al usuario.

Si hacés una llamada de red en el Main Thread, ese hilo queda **bloqueado esperando la respuesta**. Mientras espera, no puede redibujar la pantalla ni responder a ningún toque. El sistema detecta esto y lanza la excepción:

```
NetworkOnMainThreadException
```

`enqueue()` resuelve el problema así:

```
Main Thread                         Hilo de red (OkHttp thread pool)
     │                                          │
     │  call.enqueue(callback)                  │
     │─────────────────────────────────────────►│  hace la request HTTP
     │                                          │  espera la respuesta...
     │  (sigue dibujando la UI, sin bloqueos)   │  recibe la respuesta
     │                                          │  parsea el JSON con Gson
     │◄─────────────────────────────────────────│  llama a onResponse()
     │                                          │
     │  actualiza la UI con los datos           │
```

1. `enqueue()` **encola** la petición en un hilo separado (OkHttp maneja el pool de hilos)
2. El Main Thread queda libre — la UI sigue respondiendo
3. Cuando llega la respuesta, Retrofit la parsea y **vuelve al Main Thread** para llamar a `onResponse()` o `onFailure()`
4. Como `onResponse()` corre en el Main Thread, podés tocar la UI directamente sin problemas

```java
// En el Fragment — onViewCreated()
PokemonApiService apiService = RetrofitClient.getInstance()
        .create(PokemonApiService.class);

Call<PokemonListResponse> call = apiService.getPokemon(20);

call.enqueue(new Callback<PokemonListResponse>() {

    @Override
    public void onResponse(Call<PokemonListResponse> call,
                           Response<PokemonListResponse> response) {
        // Corre en el Main Thread — podés tocar la UI
        if (response.isSuccessful()) {
            List<PokemonResult> lista = response.body().getResults();
            // actualizar RecyclerView...
        } else {
            // response.code() → 404, 500, etc.
        }
    }

    @Override
    public void onFailure(Call<PokemonListResponse> call, Throwable t) {
        // Error de red (sin internet, timeout, etc.)
        // También corre en el Main Thread
        Log.e("TAG", t.getMessage());
    }
});
```

> **Regla de oro:** siempre usá `enqueue()`, nunca `execute()`. `execute()` es la versión sincrónica — bloquea el Main Thread y lanza `NetworkOnMainThreadException`.

---

## Paso 4 — Modelos: cómo Gson parsea el JSON

La PokeAPI devuelve este JSON para el listado:

```json
{
  "count": 1350,
  "results": [
    { "name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/" },
    { "name": "ivysaur",   "url": "https://pokeapi.co/api/v2/pokemon/2/" }
  ]
}
```

Creamos una clase Java que "espeja" esa estructura. Gson mapea cada campo del JSON al campo Java correspondiente usando `@SerializedName`:

```java
public class PokemonListResponse {
    @SerializedName("count")
    private int count;

    @SerializedName("results")
    private List<PokemonResult> results;
    // getters...
}

public class PokemonResult {
    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;
    // getters...
}
```

### Truco: URL del sprite sin una segunda llamada

La URL de cada pokemon en la lista tiene la forma:
```
https://pokeapi.co/api/v2/pokemon/1/
```

El número al final es el ID del pokemon. Las imágenes de la PokeAPI siguen el patrón:
```
https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/{ID}.png
```

Podemos extraer el ID de la URL y construir la del sprite directamente, sin hacer una segunda llamada a la API:

```java
public String getSpriteUrl() {
    String[] parts = url.split("/");
    String id = parts[parts.length - 1]; // "1", "2", etc.
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + id + ".png";
}
```

---

## Paso 5 — Glide: cargar imágenes desde una URL

Glide es una librería para cargar imágenes desde internet. Maneja automáticamente el hilo de descarga, el caché y el redimensionado:

```java
Glide.with(context)
     .load(pokemon.getSpriteUrl())  // URL de la imagen
     .into(ivPokemon);              // ImageView destino
```

| | **Retrofit** | **Glide** |
|---|---|---|
| ¿Qué descarga? | JSON de una API | Imágenes desde una URL |
| ¿En qué hilo? | Hilo separado (enqueue) | Hilo separado (automático) |
| ¿Qué devuelve? | Objeto Java (via Gson) | Imagen en un ImageView |
| Caché | No por defecto | Sí, automático |

---

## Paso 6 — RecyclerView y click listener

Ver **[RecyclerView.md](./RecyclerView.md)** para la explicación completa de cómo funciona.

Para el click en cada ítem, el Adapter expone una interfaz que el Fragment implementa. Así el Adapter no sabe nada de navegación:

```java
// Interfaz definida en el Adapter
public interface OnPokemonClickListener {
    void onPokemonClick(String pokemonName);
}

// El Fragment la implementa con una lambda al crear el Adapter
new PokemonAdapter(lista, pokemonName -> {
    Bundle args = new Bundle();
    args.putString("pokemonName", pokemonName);
    Navigation.findNavController(view)
            .navigate(R.id.action_pokemonList_to_detail, args);
});
```

---

## Flujo completo de navegación

```
MainActivity (NavHost)
└── nav_graph.xml
    ├── auth_nav_graph.xml
    │   └── LoginFragment
    │         └──(action_auth_to_home)──► home_nav_graph
    └── home_nav_graph.xml
        ├── HomeFragment
        │     └──(action_home_to_pokemon)──► PokemonListFragment
        ├── PokemonListFragment
        │     └──(action_pokemonList_to_detail + pokemonName)──► PokemonDetailFragment
        └── PokemonDetailFragment
              └── GET /pokemon/{name} → imagen + nombre + tipos + altura + peso
```

---

## Estructura del proyecto

```
app/src/main/
├── java/com/example/activitiesandviews/
│   ├── data/
│   │   ├── model/
│   │   │   ├── PokemonResult.java         ← ítem de la lista { name, url, getSpriteUrl() }
│   │   │   ├── PokemonListResponse.java   ← respuesta del GET /pokemon
│   │   │   ├── PokemonDetail.java         ← respuesta del GET /pokemon/{name}
│   │   │   ├── PokemonSprites.java        ← sprites → other → official-artwork
│   │   │   └── PokemonTypeSlot.java       ← types[].type.name
│   │   └── network/
│   │       ├── RetrofitClient.java        ← singleton, construye la instancia de Retrofit
│   │       └── PokemonApiService.java     ← interfaz con @GET, @Query, @Path
│   └── ui/
│       ├── MainActivity.java
│       ├── auth/
│       │   └── LoginFragment.java
│       ├── home/
│       │   └── HomeFragment.java
│       └── pokemon/
│           ├── PokemonListFragment.java   ← llama GET /pokemon, muestra RecyclerView
│           ├── PokemonDetailFragment.java ← llama GET /pokemon/{name}, muestra detalle
│           └── PokemonAdapter.java        ← adapter del RecyclerView + click listener
└── res/
    ├── layout/
    │   ├── fragment_pokemon_list.xml      ← RecyclerView + ProgressBar + tvError
    │   ├── fragment_pokemon_detail.xml    ← imagen + nombre + tipos + altura + peso
    │   └── item_pokemon.xml               ← card con imagen y nombre (una fila)
    └── navigation/
        └── home_nav_graph.xml             ← pokemonListFragment y pokemonDetailFragment
```

---

## Resumen de conceptos

| Concepto | Para qué sirve |
|---|---|
| `Retrofit` | Cliente HTTP que convierte interfaces Java en llamadas HTTP |
| `@GET / @POST` | Anotaciones que definen el verbo y path del endpoint |
| `@Query` | Agrega query params a la URL (`?limit=20`) |
| `@Path` | Reemplaza un segmento de la URL (`/pokemon/{name}`) |
| `@SerializedName` | Mapea un campo JSON a un campo Java con distinto nombre |
| `Call<T>` | Representa una llamada HTTP pendiente de ejecutar |
| `enqueue()` | Ejecuta la llamada en un hilo separado, sin bloquear la UI |
| `onResponse()` | Callback cuando el servidor respondió (2xx, 4xx, 5xx) |
| `onFailure()` | Callback cuando hubo error de red (sin internet, timeout) |
| `isSuccessful()` | `true` solo si el código HTTP es 2xx |
| `response.code()` | Devuelve el código HTTP (200, 404, 500, etc.) |
| `GsonConverterFactory` | Convierte automáticamente JSON → objetos Java |
| `Glide` | Carga imágenes desde una URL en un `ImageView` |
| `RecyclerView` | Lista que reutiliza vistas para alta performance |
| `Adapter` | Puente entre los datos y las vistas del RecyclerView |
| `ViewHolder` | Guarda referencias a las vistas de una fila para no llamar `findViewById` cada vez |
