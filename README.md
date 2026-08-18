# ActivitiesAndViews - Fragments y Navigation Component

## Branches del proyecto

| Branch | Contenido |
|--------|-----------|
| `feature/navigation-intents` | Navegación entre Activities con Intents |
| `feature/navigation-component` | **Fragments + Navigation Component (este branch)** |

---

## ¿Qué aprendemos en este branch?

Migramos de la navegación tradicional Activity-based a **Single Activity Architecture** usando el **Navigation Component** de Jetpack. El proyecto demuestra:

1. Qué es un Fragment y cómo difiere de una Activity
2. Ciclo de vida de un Fragment (`onCreateView` vs `onViewCreated`)
3. Navegación entre Fragments con `NavController`
4. Cómo pasar argumentos entre Fragments con `Bundle`
5. Control del back stack con `popUpTo` y `popUpToInclusive`
6. Organización del código en múltiples nav graphs

---

## Contexto: ¿por qué migrar de Activities a Fragments?

En el branch anterior (`feature/navigation-intents`) cada pantalla era una **Activity** independiente. Eso funciona, pero tiene limitaciones:

| Activities (branch anterior) | Fragments (este branch) |
|------------------------------|------------------------|
| Cada pantalla es una Activity | Una sola Activity, múltiples Fragments |
| Navegación via Intents | Navegación via NavController |
| Back stack manejado por el sistema | Back stack manejado por Navigation Component |
| Datos pasados con `putExtra` | Datos pasados con `Bundle` |
| Sin control fino del back stack | `popUpTo` para control preciso |

Google recomienda **Single Activity Architecture** desde 2019. Todos los proyectos oficiales (Now in Android, architecture-samples) y Jetpack Compose usan este patrón.

---

## Conceptos clave

### ¿Qué es un Fragment?

Un Fragment es una **porción reutilizable de interfaz de usuario** que vive dentro de una Activity. A diferencia de una Activity, un Fragment no puede existir solo — siempre necesita una Activity que lo contenga.

```
Activity (MainActivity)
    │
    └── FragmentContainerView  ← "el contenedor"
            │
            ├── LoginFragment     ← pantalla de login
            ├── HomeFragment      ← pantalla de bienvenida
            └── DetailFragment    ← pantalla de detalle
```

### Ciclo de vida de un Fragment

El ciclo de vida de un Fragment tiene más etapas que el de una Activity. Las dos más importantes para nosotros son:

```
onCreateView()   → inflás el layout, devolvés la View
                   NO accedas a las vistas aquí

onViewCreated()  → la View ya está creada y disponible
                   acá buscás vistas con findViewById y seteás listeners
```

```java
@Override
public View onCreateView(LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
    // Solo inflar — no buscar vistas
    return inflater.inflate(R.layout.fragment_login, container, false);
}

@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // Acá sí podés usar view.findViewById(...)
    Button btnIngresar = view.findViewById(R.id.btnIngresar);
}
```

### ¿Qué es el Navigation Component?

El Navigation Component es una librería de Jetpack que centraliza toda la navegación de la app en un solo archivo XML: el **nav graph**.

Tiene tres piezas principales:

| Pieza | Qué es | Dónde vive |
|-------|--------|------------|
| `NavGraph` | El mapa de destinos y acciones | `res/navigation/*.xml` |
| `NavHost` | El contenedor donde se muestran los Fragments | `activity_main.xml` |
| `NavController` | El que ejecuta la navegación | Se obtiene en el Fragment |

---

## Flujo de la app

```
MainActivity (NavHost — única Activity)
       │
       └── nav_graph.xml (raíz)
              ├── auth_nav_graph.xml
              │       └── LoginFragment (start)
              │               └──(action_auth_to_home + username)──►
              └── home_nav_graph.xml
                      ├── HomeFragment (start)
                      │       ├──(action_home_to_detail + username)──► DetailFragment
                      │       └──(logout: popUpTo nav_graph)──► LoginFragment
                      └── DetailFragment
```

---

## Estructura del proyecto

```
app/src/main/
├── java/com/example/activitiesandviews/
│   ├── ui/
│   │   ├── MainActivity.java          ← única Activity, solo configura el NavHost
│   │   ├── auth/
│   │   │   └── LoginFragment.java     ← pantalla de login
│   │   └── home/
│   │       ├── HomeFragment.java      ← pantalla de bienvenida
│   │       └── DetailFragment.java    ← pantalla de detalle
│   ├── data/
│   │   ├── repository/                ← (vacío, para futuras clases)
│   │   └── model/                     ← (vacío, para futuras clases)
│   ├── LoginActivity.java             ← referencia: branch anterior
│   └── HomeActivity.java              ← referencia: branch anterior
└── res/
    ├── layout/
    │   ├── activity_main.xml          ← solo contiene el FragmentContainerView
    │   ├── fragment_login.xml         ← diseño del login
    │   ├── fragment_home.xml          ← diseño del home
    │   └── fragment_detail.xml        ← diseño del detalle
    └── navigation/
        ├── nav_graph.xml              ← raíz: incluye los dos sub-graphs
        ├── auth_nav_graph.xml         ← graph de autenticación
        └── home_nav_graph.xml         ← graph del home
```

---

## Paso a paso: cómo se implementó

### Paso 1 — Agregar dependencias (`libs.versions.toml`)

```toml
[versions]
navigation = "2.8.9"

[libraries]
navigation-fragment = { group = "androidx.navigation", name = "navigation-fragment", version.ref = "navigation" }
navigation-ui      = { group = "androidx.navigation", name = "navigation-ui",      version.ref = "navigation" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
}
```

---

### Paso 2 — NavHost en `activity_main.xml`

`MainActivity` ya no tiene contenido propio. Solo declara el **contenedor** donde vivirán los Fragments:

```xml
<androidx.fragment.app.FragmentContainerView
    android:id="@+id/nav_host_fragment"
    android:name="androidx.navigation.fragment.NavHostFragment"
    app:defaultNavHost="true"
    app:navGraph="@navigation/nav_graph" />
```

| Atributo | Significado |
|----------|-------------|
| `android:name` | Indica que este contenedor es un NavHostFragment |
| `app:defaultNavHost="true"` | Intercepta el botón "atrás" del sistema |
| `app:navGraph` | El archivo XML que define los destinos |

---

### Paso 3 — Nav graphs

#### `nav_graph.xml` (raíz)

El grafo raíz no define destinos directamente. Solo **incluye** los sub-graphs y declara cuál es el inicio:

```xml
<navigation
    android:id="@+id/nav_graph"
    app:startDestination="@id/auth_nav_graph">

    <include app:graph="@navigation/auth_nav_graph" />
    <include app:graph="@navigation/home_nav_graph" />

</navigation>
```

#### `auth_nav_graph.xml`

```xml
<navigation
    android:id="@+id/auth_nav_graph"
    app:startDestination="@id/loginFragment">

    <fragment android:id="@+id/loginFragment"
              android:name="...ui.auth.LoginFragment">
        <action
            android:id="@+id/action_auth_to_home"
            app:destination="@id/home_nav_graph"
            app:popUpTo="@id/auth_nav_graph"
            app:popUpToInclusive="true" />
    </fragment>

</navigation>
```

> `popUpTo` + `popUpToInclusive="true"` sobre `auth_nav_graph`: al navegar al home, el login se elimina del back stack. El usuario no puede volver al login tocando "atrás".

#### `home_nav_graph.xml`

```xml
<navigation
    android:id="@+id/home_nav_graph"
    app:startDestination="@id/homeFragment">

    <argument name="username" app:argType="string" android:defaultValue="" />

    <fragment android:id="@+id/homeFragment" ...>
        <argument name="username" app:argType="string" android:defaultValue="" />
        <action android:id="@+id/action_home_to_detail"
                app:destination="@id/detailFragment" />
    </fragment>

    <fragment android:id="@+id/detailFragment" ...>
        <argument name="username" app:argType="string" android:defaultValue="" />
    </fragment>

</navigation>
```

---

### Paso 4 — Pasar argumentos entre Fragments

A diferencia de los Intents (que usaban `putExtra`/`getStringExtra`), entre Fragments se usa un **Bundle** para enviar y `getArguments()` para recibir.

**Envío (LoginFragment → HomeFragment):**
```java
Bundle args = new Bundle();
args.putString("username", username);

Navigation.findNavController(view)
        .navigate(R.id.action_auth_to_home, args);
```

**Recepción (HomeFragment):**
```java
String username = getArguments() != null
        ? getArguments().getString("username", "")
        : "";
```

> La clave `"username"` debe coincidir exactamente en el envío y la recepción, igual que con `putExtra`/`getStringExtra` en los Intents.

---

### Paso 5 — NavController

El `NavController` es el objeto que ejecuta las navegaciones. Se obtiene desde cualquier Fragment con:

```java
Navigation.findNavController(view).navigate(R.id.action_auth_to_home, args);
```

En `MainActivity` lo guardamos para soportar el botón "atrás" del sistema:

```java
NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
        .findFragmentById(R.id.nav_host_fragment);

navController = navHostFragment.getNavController();

// Permite que el botón "atrás" de la toolbar use el NavController
@Override
public boolean onSupportNavigateUp() {
    return navController.navigateUp() || super.onSupportNavigateUp();
}
```

---

### Paso 6 — Logout: navegación forzada con `popUpTo`

El botón "Cerrar sesión" en `HomeFragment` navega a `auth_nav_graph` limpiando **todo** el back stack:

```java
NavOptions navOptions = new NavOptions.Builder()
        .setPopUpTo(R.id.nav_graph, true)  // elimina todo hasta la raíz
        .build();

Navigation.findNavController(view)
        .navigate(R.id.auth_nav_graph, null, navOptions);
```

**`popUpTo(R.id.nav_graph, true)`** — saca todo hasta el grafo raíz inclusive. El resultado es que `LoginFragment` queda como único elemento del stack.

---

## Control del back stack: `popUpTo` y `popUpToInclusive`

Estos dos atributos controlan qué se elimina del back stack al navegar.

### `popUpTo`

Elimina todos los destinos del stack **hasta llegar** al destino indicado:

```
Stack: [ Login | Home | Detail ]
navigate con popUpTo="loginFragment"
Resultado: [ Login | NuevoDestino ]   ← Login quedó, Home y Detail fueron removidos
```

### `popUpToInclusive`

Extiende `popUpTo` para decidir si el destino indicado **también se elimina**:

```
popUpToInclusive="false"  →  elimina hasta Login, pero Login queda
popUpToInclusive="true"   →  elimina hasta Login, Login también se elimina
```

### Casos de uso en este proyecto

| Acción | popUpTo | inclusive | Resultado |
|--------|---------|-----------|-----------|
| Login exitoso | `auth_nav_graph` | `true` | Login removido, no se puede volver |
| Logout | `nav_graph` | `true` | Todo el stack limpio, solo queda Login |
| Navegar a Detail | — | — | Detail se apila, "atrás" vuelve a Home |

---

## Múltiples nav graphs: ¿cómo funciona el back stack?

Los nav graphs **no tienen back stacks independientes**. Todos comparten la misma pila del `NavController`. La separación es puramente organizativa.

```
Un solo back stack:
[ LoginFragment | HomeFragment | DetailFragment ]
                                      ↑ "atrás" → HomeFragment
                               ↑ "atrás" → LoginFragment (si no fue eliminado)
```

La separación en múltiples graphs aporta:
- **Organización**: cada feature tiene su propio archivo
- **Reusabilidad**: podés incluir el mismo sub-graph en distintos lugares
- **Encapsulamiento**: las acciones internas de un graph no se exponen al resto

Para navegar de un graph a otro usás el **ID del graph** como destino:
```java
Navigation.findNavController(view).navigate(R.id.home_nav_graph);
// → esto empuja el startDestination del home_nav_graph al stack
```

---

## Comparación: Intent vs Navigation Component

| | Intents (branch anterior) | Navigation Component (este branch) |
|--|--------------------------|-----------------------------------|
| Unidad de pantalla | Activity | Fragment |
| Navegación | `startActivity(intent)` | `navController.navigate(actionId)` |
| Paso de datos | `intent.putExtra("key", value)` | `bundle.putString("key", value)` |
| Recepción de datos | `getIntent().getStringExtra("key")` | `getArguments().getString("key")` |
| Limpiar back stack | `FLAG_ACTIVITY_CLEAR_TASK` | `popUpTo` + `popUpToInclusive` |
| Configuración | `AndroidManifest.xml` | `nav_graph.xml` |

---

## Resumen de conceptos

| Concepto | Para qué sirve |
|----------|---------------|
| `Fragment` | Porción de UI reutilizable que vive dentro de una Activity |
| `onCreateView` | Inflar el layout del Fragment |
| `onViewCreated` | Inicializar vistas y listeners |
| `NavHost` | Contenedor que muestra los Fragments (`FragmentContainerView`) |
| `NavGraph` | Mapa de destinos y acciones en XML |
| `NavController` | Ejecuta navegaciones entre Fragments |
| `Bundle` | Contenedor clave-valor para pasar argumentos |
| `getArguments()` | Leer los argumentos recibidos en un Fragment |
| `popUpTo` | Elimina destinos del back stack hasta el indicado |
| `popUpToInclusive` | Si `true`, elimina también el destino indicado |
| `<include>` | Incluye un sub-graph dentro del grafo raíz |
