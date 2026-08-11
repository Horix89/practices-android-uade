# ActivitiesAndViews - Navegación entre Activities

## ¿Qué vamos a aprender?

En este proyecto aprendemos a navegar entre dos pantallas en Android usando **Activities** e **Intents**, y cómo pasar datos de una pantalla a la otra.

---

## Conceptos clave antes de arrancar

### ¿Qué es una Activity?

Una Activity es una **pantalla de tu app**. Cada vez que el usuario ve algo en pantalla, está mirando una Activity.

- `LoginActivity` → la pantalla donde el usuario ingresa su nombre
- `HomeActivity` → la pantalla de bienvenida a la que llega tras iniciar sesión

### ¿Qué es un Intent?

Un Intent es el **mecanismo de Android para comunicar componentes entre sí**. En este proyecto lo usamos para dos cosas:

1. **Navegar** de una Activity a otra
2. **Pasar datos** entre esas Activities

Podés pensarlo como un sobre: lo armás, le ponés adentro lo que querés mandar, y lo enviás.

### ¿Qué es el Back Stack?

Android mantiene una **pila de Activities** (llamada back stack). Cada vez que abrís una pantalla nueva, se apila encima de la anterior. Cuando tocás "atrás", se desapila.

```
Pantalla 1 → Pantalla 2 → Pantalla 3
                              ↑ estás acá

Tocás "atrás" → volvés a Pantalla 2
Tocás "atrás" → volvés a Pantalla 1
```

En un flujo de Login esto es un problema: si el usuario ya inició sesión, no debería poder volver al Login tocando "atrás". Más adelante explicamos cómo se resuelve.

---

## Flujo de la app

```
App inicia
    │
    ▼
LoginActivity         ← pantalla de inicio (launcher)
    │
    │  usuario ingresa su nombre y toca "Ingresar"
    │
    ▼
HomeActivity          ← muestra "Bienvenido, [nombre]!"
    │
    │  usuario toca "atrás"
    │
    ▼
App se cierra         ← NO vuelve al Login
```

---

## Estructura del proyecto

```
app/src/main/
├── java/com/example/activitiesandviews/
│   ├── LoginActivity.java     ← pantalla de login
│   ├── HomeActivity.java      ← pantalla principal
│   └── MainActivity.java      ← pantalla con lista de países (ejemplo previo)
└── res/
    └── layout/
        ├── activity_login.xml ← diseño de la pantalla de login
        ├── activity_home.xml  ← diseño de la pantalla principal
        └── activity_main.xml  ← diseño de la lista de países
```

---

## Paso a paso: cómo se implementó

### Paso 1 — Declarar las Activities en el Manifest

Antes de poder usar una Activity, **Android necesita saber que existe**. Esto se hace en `AndroidManifest.xml`.

```xml
<!-- LoginActivity es la pantalla que se abre al iniciar la app -->
<activity
    android:name=".LoginActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- HomeActivity solo se declara, no es launcher -->
<activity android:name=".HomeActivity" />
```

El bloque `<intent-filter>` con `MAIN` y `LAUNCHER` le dice al sistema que `LoginActivity` es **la primera pantalla que se abre** cuando el usuario toca el ícono de la app.

---

### Paso 2 — Layout de LoginActivity (`activity_login.xml`)

La pantalla de login tiene tres elementos:

```
┌─────────────────────────┐
│      Iniciar sesión     │  ← TextView (título)
│                         │
│  [ Nombre            ]  │  ← EditText (etNombre)
│  [ Email             ]  │  ← EditText (etEmail)
│  [ Contraseña        ]  │  ← EditText (etPassword)
│                         │
│       [ Ingresar ]      │  ← Button (btnIngresar)
└─────────────────────────┘
```

Cada elemento tiene un `android:id` para poder referenciarlo desde Java.

---

### Paso 3 — LoginActivity.java

```java
public class LoginActivity extends AppCompatActivity {

    private EditText etNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // conecta con el XML

        // findViewById busca una vista por su ID en el layout actual
        etNombre = findViewById(R.id.etNombre);

        Button btnIngresar = findViewById(R.id.btnIngresar);

        // setOnClickListener define qué pasa cuando el usuario toca el botón
        btnIngresar.setOnClickListener(v -> navigateToHome());
    }

    private void navigateToHome() {
        // 1. Leemos el texto que escribió el usuario
        String nombre = etNombre.getText().toString();

        // 2. Creamos el Intent: "quiero ir a HomeActivity"
        Intent intent = new Intent(this, HomeActivity.class);

        // 3. Adjuntamos el nombre como dato extra (clave - valor)
        intent.putExtra("NOMBRE_USUARIO", nombre);

        // 4. Configuramos los flags para limpiar el back stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 5. Lanzamos la Activity
        startActivity(intent);
    }
}
```

---

### Paso 4 — HomeActivity.java

```java
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // getIntent() devuelve el Intent que inició esta Activity
        // getStringExtra("clave") recupera el dato que mandamos desde LoginActivity
        String nombre = getIntent().getStringExtra("NOMBRE_USUARIO");

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Bienvenido, " + nombre + "!");
    }
}
```

La clave `"NOMBRE_USUARIO"` tiene que ser **exactamente la misma** en el `putExtra` y en el `getStringExtra`. Si no coincide, devuelve `null`.

---

## ¿Cómo se pasan datos entre Activities?

El Intent funciona como un diccionario de clave-valor:

**Lado emisor (LoginActivity):**
```java
intent.putExtra("NOMBRE_USUARIO", nombre);  // String
intent.putExtra("EDAD", 25);                // int
intent.putExtra("ACTIVO", true);            // boolean
```

**Lado receptor (HomeActivity):**
```java
String nombre  = getIntent().getStringExtra("NOMBRE_USUARIO");
int edad       = getIntent().getIntExtra("EDAD", 0);      // 0 = valor por defecto
boolean activo = getIntent().getBooleanExtra("ACTIVO", false);
```

---

## ¿Por qué el usuario no puede volver al Login?

Con `setFlags` le indicamos al sistema cómo manejar el back stack:

```java
intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
```

| Flag | Qué hace |
|------|----------|
| `FLAG_ACTIVITY_NEW_TASK` | Inicia la Activity en una nueva tarea |
| `FLAG_ACTIVITY_CLEAR_TASK` | Destruye todas las Activities anteriores de la pila |

**Sin flags:**
```
[ LoginActivity → HomeActivity ]
                      ↑ "atrás" te lleva de vuelta al Login ❌
```

**Con flags:**
```
[ HomeActivity ]
      ↑ "atrás" cierra la app ✅
```

---

## Resumen

| Concepto | Para qué sirve |
|----------|---------------|
| `Activity` | Representa una pantalla de la app |
| `AndroidManifest.xml` | Registra todas las Activities que existen |
| `Intent` | Permite navegar entre Activities y enviar datos |
| `putExtra` / `getStringExtra` | Enviar y recibir datos entre Activities |
| `setFlags` | Controlar el comportamiento del back stack |
| `findViewById` | Obtener una referencia a una vista del layout |
| `setOnClickListener` | Definir qué pasa cuando el usuario toca algo |
