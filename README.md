# feature/storage — Métodos de Persistencia en Android

Proyecto de prácticas en Android (Java). Este branch agrega ejemplos funcionales de los métodos de persistencia más comunes, accesibles desde Home → **"Ejemplos de Storage"**.

---

## 1. SharedPreferences — 
 Se agregó un fragment llamado `demo_prefs`, con su propia pantalla (Home → Storage → **SharedPreferences**) donde se puede escribir, leer y borrar un valor libremente y ver el resultado al instante.


```java
// StorageModule.java
@Provides
@Singleton
public SharedPreferences providePlainSharedPreferences(@ApplicationContext Context context) {
    return context.getSharedPreferences("demo_prefs", Context.MODE_PRIVATE);
    // "demo_prefs" es un archivo XML distinto de "auth_prefs" — no se mezclan
    // los datos de la demo con el token real de autenticación
}
```

```java
// SharedPreferencesFragment.java
@Inject
SharedPreferences sharedPreferences; // Hilt inyecta el singleton de arriba, sin new ni Context manual
```

El resto es idéntico al patrón guardar/leer/borrar ya visto: `.edit().putString(...).apply()`, `.getString(key, null)`, `.edit().remove(key).apply()`. La lectura y escritura son **síncronas** (bloquean el hilo que las llama, aunque `.apply()` escribe a disco en background) — esta es la principal diferencia frente a DataStore, ver sección 5.

**Dónde se persiste:** `/data/data/com.example.activitiesandviews/shared_prefs/demo_prefs.xml` (mismo formato XML texto plano que `auth_prefs.xml`).

---

## 2. Room — `RoomFragment.java`, `Note.java`, `NoteDao.java`, `AppDatabase.java`

**Caso de uso:** Datos estructurados con queries SQL, relaciones o historial.
**Por qué sobre SQLite directo:** Abstracción type-safe, validación de queries en tiempo de compilación, sin boilerplate de cursores.

### Dependencia
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
annotationProcessor("androidx.room:room-compiler:2.6.1")
```

### Entidad — `Note.java`
```java
@Entity(tableName = "notes")       // define la tabla "notes" en SQLite
public class Note {

    @PrimaryKey(autoGenerate = true) // columna id, autoincremental
    public int id;

    @ColumnInfo(name = "content")    // columna "content"
    public String content;

    @ColumnInfo(name = "created_at") // columna "created_at" — timestamp en milisegundos
    public long createdAt;
}
```

### DAO — `NoteDao.java`
```java
@Dao
public interface NoteDao {

    @Insert
    void insert(Note note);  // Room genera el INSERT INTO notes (...) VALUES (...)

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    List<Note> getAll();     // Room valida este SQL en tiempo de compilación

    @Query("DELETE FROM notes")
    void deleteAll();
}
```

### Base de datos — `AppDatabase.java`
```java
@Database(entities = {Note.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao(); // Room genera la implementación de esta interfaz
}
```

### Cómo se crea la instancia — `StorageModule.java`
```java
@Provides @Singleton
public AppDatabase provideDatabase(@ApplicationContext Context context) {
    return Room.databaseBuilder(context, AppDatabase.class, "notes_db").build();
}

@Provides @Singleton
public NoteDao provideNoteDao(AppDatabase database) {
    return database.noteDao();
}
```

### Operaciones en background — `RoomFragment.java`
Room **no permite operaciones en el main thread** (lanzaría una excepción). Se usa `ExecutorService`:
```java
private final ExecutorService executor = Executors.newSingleThreadExecutor();
```

**Guardar:**
```java
executor.execute(() -> {
    noteDao.insert(note);                    // se ejecuta en el hilo de fondo
    requireActivity().runOnUiThread(() ->    // vuelve al main thread para actualizar la UI
        tvResultado.setText("Nota guardada: ...")
    );
});
```

**Leer:**
```java
executor.execute(() -> {
    List<Note> notes = noteDao.getAll();
    requireActivity().runOnUiThread(() ->
        tvResultado.setText(sb.toString())
    );
});
```

**Al destruirse la view:**
```java
@Override
public void onDestroyView() {
    super.onDestroyView();
    executor.shutdown(); // libera el hilo de fondo
}
```

### Dónde se persiste físicamente

```
/data/data/com.example.activitiesandviews/databases/notes_db
/data/data/com.example.activitiesandviews/databases/notes_db-shm   ← shared memory (WAL)
/data/data/com.example.activitiesandviews/databases/notes_db-wal   ← write-ahead log
```

**Cómo observarlo en Android Studio:**
1. `View` → `Tool Windows` → `Device File Explorer`
2. Navegar a: `data/data/com.example.activitiesandviews/databases/`
3. Click derecho sobre `notes_db` → `Save As...` para descargarlo localmente
4. Abrirlo con **DB Browser for SQLite** (app gratuita) — verás la tabla `notes` con sus filas

---

## 3. Files — `FilesFragment.java`

**Caso de uso:** Archivos de texto, logs, PDFs, imágenes descargadas.
**Por qué:** Acceso directo al filesystem privado de la app sin permisos extra.

No requiere dependencias adicionales — usa el API estándar de Java `java.io`.

### Directorio privado
```java
// getFilesDir() devuelve: /data/data/<package>/files/
File file = new File(requireContext().getFilesDir(), "mi_archivo.txt");
```

### Escribir
```java
try (FileOutputStream fos = new FileOutputStream(file)) { // try-with-resources: cierra el stream automáticamente
    fos.write(content.getBytes());
}
```

### Leer
```java
try (FileInputStream fis = new FileInputStream(file);
     BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
    // BufferedReader permite leer por líneas en lugar de byte a byte
    String line;
    while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
    }
}
```

### Borrar
```java
if (file.exists() && file.delete()) { ... }
```

### Dónde se persiste físicamente

```
/data/data/com.example.activitiesandviews/files/mi_archivo.txt
```

**Cómo observarlo en Android Studio:**
1. `View` → `Tool Windows` → `Device File Explorer`
2. Navegar a: `data/data/com.example.activitiesandviews/files/`
3. Doble click en `mi_archivo.txt` — se abre como texto plano directamente en Android Studio

---

## 4. EncryptedSharedPreferences — `EncryptedPrefsFragment.java`

**Caso de uso:** Datos sensibles — tokens JWT, API keys, credenciales.
**Por qué sobre SharedPreferences normal:** El archivo XML de SharedPreferences es texto plano legible. EncryptedSharedPreferences cifra tanto las claves como los valores en disco.

### Dependencia
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

### Construcción de la instancia
```java
private SharedPreferences buildEncryptedPrefs() throws Exception {

    // 1. Crea o recupera la MasterKey del Android Keystore del dispositivo
    //    La clave maestra nunca sale del hardware del dispositivo
    MasterKey masterKey = new MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build();

    // 2. Crea las SharedPreferences cifradas
    return EncryptedSharedPreferences.create(
            requireContext(),
            "secure_prefs",                                                  // nombre del archivo
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,  // cifrado de claves
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM // cifrado de valores
    );
}
```
- **AES256_SIV** para claves: cifrado determinístico, necesario para poder buscar por clave.
- **AES256_GCM** para valores: cifrado con autenticación, más seguro para datos sensibles.

### Guardar / Recuperar / Limpiar
```java
// La interfaz es idéntica a SharedPreferences normal — el cifrado es transparente
prefs.edit().putString(KEY_SECRET, value).apply();  // cifra al guardar
prefs.getString(KEY_SECRET, null);                  // descifra al leer
prefs.edit().remove(KEY_SECRET).apply();
```

### Dónde se persiste físicamente

```
/data/data/com.example.activitiesandviews/shared_prefs/secure_prefs.xml
```

**Cómo observarlo en Android Studio:**
1. `View` → `Tool Windows` → `Device File Explorer`
2. Navegar a: `data/data/com.example.activitiesandviews/shared_prefs/`
3. Doble click en `secure_prefs.xml` — verás el contenido cifrado:

```xml
<!-- EncryptedSharedPreferences — todo está cifrado, nada es legible -->
<map>
    <string name="ASJDF923ndf...">AQIDBAUGBwgJ...</string>
    <string name="__androidx_security_crypto_encrypted_prefs_key_keyset__">...</string>
</map>
```

Comparado con `auth_prefs.xml` (SharedPreferences normal) donde el token es texto plano visible.

---

## 5. DataStore (Preferences) — `DataStoreFragment.java`

**Caso de uso:** el mismo que SharedPreferences (pares clave-valor: flags, configuración, preferencias de usuario).
**Por qué existe si ya está SharedPreferences:** Google la presenta como su reemplazo moderno porque resuelve dos problemas de SharedPreferences:
1. **Es asíncrona por diseño** — nunca bloquea el hilo que la llama, ni siquiera para leer (SharedPreferences sí puede bloquear en su primera carga desde disco).
2. **Es transaccional y consistente** — cada actualización se aplica de forma atómica sobre el estado más reciente, evitando condiciones de carrera si dos partes de la app escriben al mismo tiempo (con SharedPreferences, dos `.edit()` concurrentes pueden pisarse).

### Dependencia

DataStore está escrito en Kotlin y expone su API "nativa" con `Flow` y funciones `suspend`. Como este proyecto es **Java puro** (sin coroutines), se usa el artefacto con wrapper de **RxJava3** que Google publica para justamente estos casos — expone `Flowable`/`Single` en vez de `Flow`/`suspend`:

```kotlin
implementation("androidx.datastore:datastore-preferences-rxjava3:1.1.7")
implementation("io.reactivex.rxjava3:rxjava:3.1.8")
```

### Antes de seguir: ¿qué son `Flowable`, `Single`, `subscribe()` y `Disposable`?

Si nunca viste RxJava, todo el código de esta sección puede parecer magia. No lo es — es el **mismo patrón de callback** que ya usaste en `PokemonListFragment` con Retrofit, solo que con nombres distintos y un poco más de vocabulario. Antes de leer el código de guardar/leer, conviene tener estas cuatro piezas claras:

**1. `Flowable<T>` / `Single<T>` — "una promesa de que en algún momento va a aparecer un valor"**

Son clases de RxJava que representan **algo que todavía no tenés, pero vas a recibir más adelante**, de forma parecida a un `Call<T>` de Retrofit:

| Ya lo conocés de Retrofit | Equivalente en RxJava | Emite... |
|---|---|---|
| `Call<PokemonListResponse>` | `Single<T>` | **un solo valor**, una sola vez, y termina (o falla) |
| — (no hay equivalente directo) | `Flowable<T>` | **cero, uno o muchos valores** a lo largo del tiempo, y puede no terminar nunca |

En este proyecto:
- `dataStore.updateDataAsync(...)` devuelve un `Single<Preferences>` → "en algún momento vas a tener el nuevo estado guardado" (una sola vez).
- `dataStore.data()` devuelve un `Flowable<Preferences>` → "vas a recibir el estado actual, y de nuevo cada vez que alguien lo cambie" (potencialmente muchas veces). Por eso, cuando en esta demo solo queremos leer **una vez**, hay que "cortar" el stream con `.firstOrError()` (ver sección "Leer" más abajo) — si no, quedaríamos escuchando cambios para siempre.

**2. `.subscribe(...)` — el equivalente exacto de `.enqueue(new Callback<>() {...})`**

En `PokemonListFragment` ya escribiste esto:
```java
call.enqueue(new Callback<PokemonListResponse>() {
    @Override
    public void onResponse(Call<...> call, Response<...> response) { /* éxito */ }

    @Override
    public void onFailure(Call<...> call, Throwable t) { /* error */ }
});
```

`.subscribe(...)` es lo mismo, pero en vez de una clase anónima con dos métodos, RxJava te deja pasar **dos lambdas**: la primera para el caso de éxito, la segunda para el error.

```java
dataStore.updateDataAsync(...)
        .subscribe(
                prefs -> { /* onResponse: acá "prefs" es el resultado */ },
                throwable -> { /* onFailure: acá "throwable" es el error */ }
        );
```

Ninguna de las dos lambdas corre en el hilo principal (el mismo motivo por el que Room usa `ExecutorService` + `runOnUiThread`) — por eso, adentro de ambas, se envuelve la actualización de la UI en `requireActivity().runOnUiThread(() -> ...)`.

**3. `Disposable` — el "ticket de cancelación" de una suscripción**

Cada vez que llamás a `.subscribe(...)`, RxJava te devuelve un `Disposable`: un objeto que representa esa operación en curso y que sirve para **cancelarla** si ya no te interesa el resultado (por ejemplo, porque el usuario navegó a otra pantalla). Es conceptualmente lo mismo que `call.cancel()` en Retrofit o `executor.shutdown()` en Room — una forma de decirle "pará, no hace falta que sigas".

**4. `CompositeDisposable` — una "bolsa" de disposables para tirar todos juntos**

Como en esta pantalla hay tres botones y por lo tanto hasta tres suscripciones activas al mismo tiempo, en vez de manejar cada `Disposable` por separado se los agrega todos a un `CompositeDisposable`, y con un solo `.clear()` en `onDestroyView()` se cancelan todas de una. Ver el detalle en "Evitar leaks" más abajo.

Con estas cuatro piezas, el código de las próximas secciones se lee igual que el de Retrofit: *"hacé esta operación (`updateDataAsync`/`data()`), y cuando tengas el resultado (`.subscribe`), hacé esto; si falla, hacé esto otro"*.

### Cómo se crea la instancia — `StorageModule.java`

```java
@Provides
@Singleton
public RxDataStore<Preferences> provideDataStore(@ApplicationContext Context context) {
    // "demo_datastore" define el nombre del archivo interno (no es un Context.MODE_PRIVATE,
    // DataStore no tiene modos — siempre es privado a la app)
    return new RxPreferenceDataStoreBuilder(context, "demo_datastore").build();
}
```

Al igual que con `SharedPreferences`, la instancia se inyecta directo con `@Inject`:
```java
@Inject
RxDataStore<Preferences> dataStore;
```

### Las claves son tipadas — `Preferences.Key`

```java
private static final Preferences.Key<String> KEY_VALOR = PreferencesKeys.stringKey("valor_demo");
// PreferencesKeys también ofrece intKey, booleanKey, floatKey, stringSetKey, etc.
// El tipo queda fijado en la clave: no hay forma de pedir un int con una key de String por error
```

### Guardar — transacción vía `updateDataAsync`

No existe un `.edit().put(...).apply()` como en SharedPreferences. En su lugar, cada escritura es una **transacción** que recibe el estado actual y devuelve el nuevo estado:

```java
dataStore.updateDataAsync(prefsIn -> {
            // prefsIn = snapshot inmutable más reciente; se convierte a mutable para modificarla
            MutablePreferences mutable = prefsIn.toMutablePreferences();
            mutable.set(KEY_VALOR, value);
            return Single.just(mutable); // se devuelve el nuevo estado envuelto en un Single
        })
        .subscribe(
                prefs -> requireActivity().runOnUiThread(() -> tvResultado.setText("Guardado:\n\"" + value + "\"")),
                throwable -> requireActivity().runOnUiThread(() -> tvResultado.setText("Error: " + throwable.getMessage()))
        );
```

`.subscribe()` recibe la operación en un hilo interno de DataStore — por eso hace falta `runOnUiThread(...)` para tocar las vistas, igual que con el `ExecutorService` de Room.

### Leer — se observa un stream, no se "hace una consulta"

`dataStore.data()` devuelve un `Flowable<Preferences>` que emite el valor actual **y cada vez que cambia**. Para leer "una sola vez" (como se hace en esta demo) se toma el primer valor y se completa:

```java
dataStore.data().firstOrError() // toma la primera emisión y listo, no queda escuchando cambios futuros
        .subscribe(
                prefs -> requireActivity().runOnUiThread(() -> {
                    String stored = prefs.get(KEY_VALOR); // null si la clave no existe todavía
                    tvResultado.setText(stored != null ? "Recuperado:\n\"" + stored + "\"" : "No hay datos guardados");
                }),
                throwable -> requireActivity().runOnUiThread(() -> tvResultado.setText("Error: " + throwable.getMessage()))
        );
```

Si en vez de `.firstOrError()` te suscribieras directo a `dataStore.data()`, la UI se actualizaría **sola** cada vez que el valor cambia (incluso desde otra pantalla o proceso) — esa es la diferencia de fondo con SharedPreferences, que solo se lee cuando el código explícitamente llama a `.getString(...)`.

### Borrar

Misma mecánica de transacción, pero removiendo la clave:
```java
dataStore.updateDataAsync(prefsIn -> {
    MutablePreferences mutable = prefsIn.toMutablePreferences();
    mutable.remove(KEY_VALOR);
    return Single.just(mutable);
})
```

### Evitar leaks — `CompositeDisposable`

Cada `.subscribe()` devuelve un `Disposable` que sigue "vivo" en un hilo de fondo aunque el usuario haya navegado a otra pantalla. Se agregan todos a un `CompositeDisposable` y se cancelan juntos al destruirse la vista (mismo rol que `executor.shutdown()` en Room):

```java
private final CompositeDisposable disposables = new CompositeDisposable();
// disposables.add(...) en cada botón

@Override
public void onDestroyView() {
    super.onDestroyView();
    disposables.clear(); // cancela cualquier operación de DataStore todavía pendiente
}
```

### Dónde se persiste físicamente

```
/data/data/com.example.activitiesandviews/files/datastore/demo_datastore.preferences_pb
```

A diferencia de SharedPreferences, el archivo `.preferences_pb` es **binario** (protocol buffers), no XML — abrirlo como texto en el Device File Explorer muestra caracteres ilegibles en vez de un `<map>` legible.

**Cómo observarlo en Android Studio:**
1. `View` → `Tool Windows` → `Device File Explorer`
2. Navegar a: `data/data/com.example.activitiesandviews/files/datastore/`
3. El archivo `demo_datastore.preferences_pb` no se puede leer a simple vista — para confirmar que el dato persiste, la forma práctica es usar la propia pantalla "Recuperar" luego de matar y reabrir la app.

---

## Resumen de persistencia física

| Método | Ruta en el dispositivo | Formato | Legible a simple vista |
|--------|------------------------|---------|------------------------|
| SharedPreferences (token) | `.../shared_prefs/auth_prefs.xml` | XML texto plano | Sí |
| SharedPreferences (demo) | `.../shared_prefs/demo_prefs.xml` | XML texto plano | Sí |
| Room | `.../databases/notes_db` | SQLite | No — usar DB Browser for SQLite |
| Files | `.../files/mi_archivo.txt` | Texto plano | Sí |
| EncryptedSharedPreferences | `.../shared_prefs/secure_prefs.xml` | XML cifrado | No |
| DataStore | `.../files/datastore/demo_datastore.preferences_pb` | Protocol Buffers binario | No |

---

## Arquitectura

```
app/
├── di/
│   └── StorageModule.java          # Hilt: AppDatabase, NoteDao, SharedPreferences, RxDataStore<Preferences>
├── data/local/
│   ├── db/
│   │   ├── Note.java               # @Entity — tabla "notes"
│   │   ├── NoteDao.java            # @Dao — insert / getAll / deleteAll
│   │   └── AppDatabase.java        # @Database — punto de entrada a Room
│   └── TokenManager.java           # SharedPreferences — token de sesión
└── ui/storage/
    ├── StorageMenuFragment.java       # menú con 5 botones
    ├── RoomFragment.java              # ejemplo Room + ExecutorService
    ├── FilesFragment.java             # ejemplo java.io filesystem
    ├── EncryptedPrefsFragment.java    # ejemplo EncryptedSharedPreferences
    ├── SharedPreferencesFragment.java # ejemplo SharedPreferences genérico (demo_prefs, vía Hilt)
    └── DataStoreFragment.java         # ejemplo DataStore (Preferences) + RxJava3
```
