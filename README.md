# feature/storage — Métodos de Persistencia en Android

Proyecto de prácticas en Android (Java). Este branch agrega ejemplos funcionales de los métodos de persistencia más comunes, accesibles desde Home → **"Ejemplos de Storage"**.

---

## 1. SharedPreferences — `TokenManager.java`

**Caso de uso:** Configuraciones simples y datos de sesión (flags, último usuario, token de autenticación).

SharedPreferences ya estaba implementado en el proyecto como parte del branch `feature/retrofit-interceptor-token`. Se reutiliza aquí como ejemplo del método más básico de persistencia.

### Cómo funciona

Persiste pares clave-valor en un archivo XML en el directorio privado de la app.

```java
private static final String PREF_NAME = "auth_prefs"; // nombre del archivo XML
private static final String KEY_TOKEN = "token";       // clave dentro del archivo

// La instancia se obtiene con getSharedPreferences()
this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
```

**Guardar:**
```java
prefs.edit().putString(KEY_TOKEN, token).apply();
// .edit()  → abre una transacción de escritura
// .apply() → escribe en disco de forma asíncrona (no bloquea el main thread)
```

**Leer:**
```java
return prefs.getString(KEY_TOKEN, null);
// segundo argumento = valor por defecto si la clave no existe
```

**Borrar:**
```java
prefs.edit().remove(KEY_TOKEN).apply();
```

### Dónde se persiste físicamente

```
/data/data/com.example.activitiesandviews/shared_prefs/auth_prefs.xml
```

Contenido del archivo en disco (texto plano legible):
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="token">fake-token-abc123</string>
</map>
```

**Cómo observarlo en Android Studio:**
1. `View` → `Tool Windows` → `Device File Explorer`
2. Navegar a: `data/data/com.example.activitiesandviews/shared_prefs/`
3. Doble click en `auth_prefs.xml` — se abre como texto plano directamente

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

## Resumen de persistencia física

| Método | Ruta en el dispositivo | Formato | Legible a simple vista |
|--------|------------------------|---------|------------------------|
| SharedPreferences | `.../shared_prefs/auth_prefs.xml` | XML texto plano | Sí |
| Room | `.../databases/notes_db` | SQLite | No — usar DB Browser for SQLite |
| Files | `.../files/mi_archivo.txt` | Texto plano | Sí |
| EncryptedSharedPreferences | `.../shared_prefs/secure_prefs.xml` | XML cifrado | No |

---

## Arquitectura

```
app/
├── di/
│   └── StorageModule.java          # Hilt: AppDatabase, NoteDao
├── data/local/
│   ├── db/
│   │   ├── Note.java               # @Entity — tabla "notes"
│   │   ├── NoteDao.java            # @Dao — insert / getAll / deleteAll
│   │   └── AppDatabase.java        # @Database — punto de entrada a Room
│   └── TokenManager.java           # SharedPreferences — token de sesión
└── ui/storage/
    ├── StorageMenuFragment.java    # menú con 3 botones
    ├── RoomFragment.java           # ejemplo Room + ExecutorService
    ├── FilesFragment.java          # ejemplo java.io filesystem
    └── EncryptedPrefsFragment.java # ejemplo EncryptedSharedPreferences
```
