# Android Practices — QR + Notificaciones

## Branch: `feature/qr-and-notifications`

Agrega dos features pedagógicas independientes: **lectura de QR** con CameraX + ML Kit y **notificaciones vía long polling** con un Foreground Service.

---

### Qué se agregó

| Acción | Archivo / Clase |
|--------|-----------------|
| Creado | `ui/qr/QrScanFragment.java` — Fragment con la lógica de QR |
| Creado | `res/layout/fragment_qr_scan.xml` — Layout con PreviewView |
| Creado | `service/PollingService.java` — Foreground Service con long polling |
| Modificado | `AndroidManifest.xml` — permisos CAMERA, FOREGROUND_SERVICE, POST_NOTIFICATIONS + declaración del Service |
| Modificado | `gradle/libs.versions.toml` + `app/build.gradle.kts` — dependencias CameraX 1.4.1 y ML Kit barcode-scanning 17.3.0 |
| Modificado | `res/navigation/home_nav_graph.xml` — destino `qrScanFragment` + action |
| Modificado | `res/layout/fragment_home.xml` — tres botones nuevos |
| Modificado | `ui/home/HomeFragment.java` — handlers de los botones nuevos |

---

### Flujo completo: permisos + CameraX + ML Kit

#### 1. `pedirPermisoCamara()` — verificar o solicitar el permiso

`app/src/main/java/com/example/activitiesandviews/ui/qr/QrScanFragment.java`

```java
private void pedirPermisoCamara() {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
        // Permiso ya otorgado: ir directo a la cámara
        iniciarCamara();
    } else {
        // Solicitar al usuario; el resultado llega al launcher registrado en onCreate()
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }
}
```

El launcher se registra en `onCreate()` (obligatorio antes de `onStart()`):

`app/src/main/java/com/example/activitiesandviews/ui/qr/QrScanFragment.java`

```java
requestPermissionLauncher = registerForActivityResult(
    new ActivityResultContracts.RequestPermission(),
    granted -> {
        if (granted) {
            iniciarCamara();
        } else {
            Toast.makeText(requireContext(),
                "Se necesita el permiso de cámara para escanear QR",
                Toast.LENGTH_LONG).show();
        }
    }
);
```

#### 2. `iniciarCamara()` — configurar CameraX y enlazar el analyzer

`app/src/main/java/com/example/activitiesandviews/ui/qr/QrScanFragment.java`

```java
private void iniciarCamara() {
    ListenableFuture<ProcessCameraProvider> futuro =
        ProcessCameraProvider.getInstance(requireContext());

    futuro.addListener(() -> {
        ProcessCameraProvider cameraProvider = futuro.get();

        // Conecta el stream de la cámara al PreviewView del layout
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // STRATEGY_KEEP_ONLY_LATEST: descarta frames si el analyzer no terminó el anterior;
        // evita que se acumule una cola de imágenes pendientes
        ImageAnalysis analisis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        analisis.setAnalyzer(cameraExecutor, this::analizarFrame);

        cameraProvider.unbindAll();
        // bindToLifecycle: CameraX libera la cámara automáticamente en onPause/onDestroy
        cameraProvider.bindToLifecycle(
            getViewLifecycleOwner(),
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analisis
        );
    }, ContextCompat.getMainExecutor(requireContext()));
}
```

#### 3. Analyzer — procesar cada frame con ML Kit

`app/src/main/java/com/example/activitiesandviews/ui/qr/QrScanFragment.java`

```java
private void analizarFrame(@NonNull ImageProxy imageProxy) {
    InputImage imagen = InputImage.fromMediaImage(
        imageProxy.getImage(),
        imageProxy.getImageInfo().getRotationDegrees()
    );

    barcodeScanner.process(imagen)
        .addOnSuccessListener(codigos -> {
            for (Barcode codigo : codigos) {
                String valor = codigo.getRawValue();
                if (valor != null) procesarResultado(valor);
            }
        })
        .addOnFailureListener(e -> Log.w(TAG, "Error en ML Kit", e))
        .addOnCompleteListener(t -> {
            // imageProxy.close() SIEMPRE en addOnCompleteListener, no en onSuccess.
            // Si no se cierra, CameraX no puede entregar el siguiente frame
            // y el pipeline se traba indefinidamente.
            imageProxy.close();
        });
}
```

#### 4. Parsear el resultado como JSON

`app/src/main/java/com/example/activitiesandviews/ui/qr/QrScanFragment.java`

```java
private void procesarResultado(String rawValue) {
    String mensaje;
    try {
        // org.json.JSONObject está incluido en el SDK de Android, sin dependencia extra
        JSONObject json = new JSONObject(rawValue);
        mensaje = "JSON detectado:\n" + json.toString(2);
    } catch (JSONException e) {
        mensaje = "Texto: " + rawValue;
    }
    String mensajeFinal = mensaje;
    requireActivity().runOnUiThread(() -> {
        tvResultado.setText(mensajeFinal);
        Toast.makeText(requireContext(), mensajeFinal, Toast.LENGTH_SHORT).show();
    });
}
```

**QR de prueba — JSON para escanear:**

```json
{"nombre": "Estudiante", "legajo": 12345, "materia": "Desarrollo Mobile"}
```

---

### Polling normal vs Long Polling

| | Polling normal | Long Polling |
|-|----------------|--------------|
| Cómo funciona | El cliente pregunta cada N segundos: "¿hay algo nuevo?" | El cliente hace una request y el **servidor la retiene** hasta tener datos |
| Requests cuando no hay datos | Muchas (una por intervalo) | Cero (la conexión está abierta pero en espera) |
| Latencia de la respuesta | Hasta N segundos (depende del intervalo) | Casi inmediata (el servidor responde en cuanto hay datos) |
| Carga en el servidor | Alta (muchas requests vacías) | Baja (una sola request por novedad) |

**Polling normal** — el cliente introduce la demora artificialmente:

```java
while (true) {
    Response r = cliente.newCall(request).execute(); // responde de inmediato
    procesarSiHayDatos(r);
    Thread.sleep(5000); // esperar 5 segundos antes de volver a preguntar
}
```

**Long Polling** — el servidor introduce la demora; el cliente solo espera:

`app/src/main/java/com/example/activitiesandviews/service/PollingService.java`

```java
while (!Thread.currentThread().isInterrupted()) {
    // execute() bloquea hasta que el servidor responde (puede tardar segundos o minutos)
    Response r = cliente.newCall(request).execute();
    procesarNovedad(r); // el servidor ya garantizó que hay algo nuevo
    // sin sleep: lanzar la siguiente request de inmediato
}
```

La URL del backend se declara como constante visible al principio de la clase:

`app/src/main/java/com/example/activitiesandviews/service/PollingService.java`

```java
// ★ Apuntar al backend real antes de probar
private static final String BASE_URL = "http://10.0.2.2:8080";
```

> `10.0.2.2` es la IP del host desde el emulador de Android Studio.

---

### Por qué Foreground Service (y sus limitaciones)

Desde Android 8 (API 26), los Services en background son matados agresivamente por el sistema para ahorrar batería. Un `Service` común puede durar segundos o minutos antes de ser eliminado.

`startForeground()` muestra una notificación persistente que le indica al sistema que el usuario es consciente de que la app sigue corriendo:

`app/src/main/java/com/example/activitiesandviews/service/PollingService.java`

```java
@Override
public int onStartCommand(Intent intent, int flags, int startId) {

    // El canal debe existir antes de llamar a startForeground(); en API < 26 es no-op
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel canal = new NotificationChannel(
            CANAL_ID, CANAL_NOMBRE, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(canal);
    }

    Notification notifForeground = new NotificationCompat.Builder(this, CANAL_ID)
        .setContentTitle("Escuchando novedades...")
        .setContentText("El servicio está conectado al backend")
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setOngoing(true) // el usuario no puede descartarla manualmente
        .build();

    // DEBE llamarse dentro de los 5 segundos de onStartCommand()
    startForeground(NOTIF_ID_FOREGROUND, notifForeground);

    // NUNCA hacer red en el hilo principal: Android lanza NetworkOnMainThreadException
    pollingThread = new Thread(this::loopDePolling);
    pollingThread.start();

    // START_STICKY: si el sistema mata el Service, lo intenta reiniciar automáticamente
    return START_STICKY;
}
```

El Service se registra en el Manifest con el tipo de foreground declarado:

`app/src/main/AndroidManifest.xml`

```xml
<!-- foregroundServiceType="dataSync" es obligatorio en targetSdk >= 34 para servicios de red -->
<service
    android:name=".service.PollingService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />
```

Y se inicia desde `HomeFragment` con `startForegroundService()`:

`app/src/main/java/com/example/activitiesandviews/ui/home/HomeFragment.java`

```java
btnPolling.setOnClickListener(v -> {
    Intent intent = new Intent(requireContext(), PollingService.class);
    requireContext().startForegroundService(intent);
});
```

---

### Permiso POST_NOTIFICATIONS en tiempo de ejecución

En Android 13+ (API 33), mostrar notificaciones requiere que el usuario otorgue el permiso `POST_NOTIFICATIONS` explícitamente, igual que el permiso de cámara. En versiones anteriores se otorga automáticamente.

El flujo en `HomeFragment` sigue el mismo patrón que `QrScanFragment` usa para la cámara:

`app/src/main/java/com/example/activitiesandviews/ui/home/HomeFragment.java`

```java
// Se registra en onCreate(), obligatorio antes de onStart()
notifPermissionLauncher = registerForActivityResult(
    new ActivityResultContracts.RequestPermission(),
    granted -> {
        if (granted) {
            mostrarNotificacionEjemplo();
        } else {
            Toast.makeText(requireContext(),
                "Se necesita el permiso de notificaciones",
                Toast.LENGTH_LONG).show();
        }
    }
);
```

```java
private void pedirPermisoNotificaciones() {
    // POST_NOTIFICATIONS es runtime permission solo en API 33+; antes se otorga automáticamente
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            mostrarNotificacionEjemplo();
        } else {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    } else {
        mostrarNotificacionEjemplo();
    }
}
```

**Limitación real e importante:**  
El Foreground Service **NO sobrevive** a "Forzar detención" desde **Ajustes → Apps → [app] → Forzar detención**.  
En ese caso el sistema mata el proceso a nivel de SO sin ejecutar `onDestroy()`. No hay ningún mecanismo a nivel de app que pueda interceptar o evitar esto.

---

### Esto NO es push real

Lo implementado aquí es **long polling**: la app inicia y mantiene la conexión activamente. Si el proceso es matado, las notificaciones dejan de llegar.

Las notificaciones **push reales** usan canales propietarios del sistema operativo:

- **Android — FCM (Firebase Cloud Messaging):** Google mantiene una conexión persistente de bajo consumo con todos los dispositivos Android. El servidor envía un mensaje a FCM, que lo entrega al dispositivo aunque la app esté cerrada.
- **iOS — APNs (Apple Push Notification service):** equivalente de Apple.

```
Long Polling (este ejercicio)        Push real (FCM / APNs)
─────────────────────────────        ──────────────────────
App → abre conexión al backend       Backend → manda mensaje a FCM
Backend → retiene y responde         FCM → entrega al dispositivo
App muerta = sin notificaciones      App muerta = igual llegan
Funciona sin cuenta externa          Requiere proyecto Firebase/Apple
```

Estos canales no se implementan en este ejercicio porque requieren:
1. Una cuenta de Firebase / Apple Developer
2. Un backend con las SDKs de FCM/APNs
3. Configuración adicional en el `AndroidManifest.xml` (plugin de Firebase)
