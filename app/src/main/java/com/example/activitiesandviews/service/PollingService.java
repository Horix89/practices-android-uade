package com.example.activitiesandviews.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Servicio en primer plano que implementa long polling contra un backend.
 *
 * Long polling: el cliente hace una request HTTP y el servidor la retiene abierta
 * hasta que haya datos nuevos para enviar (o hasta que se agote el timeout).
 * Esto evita el "polling corto" (preguntar cada N segundos) y reduce el tráfico
 * innecesario cuando no hay novedades.
 *
 * Por qué Foreground Service y no Service común:
 *   - Desde Android 8 (API 26), los Services en background son matados agresivamente
 *     por el sistema para ahorrar batería.
 *   - startForeground() muestra una notificación persistente que le indica al sistema
 *     que el usuario es consciente de que la app sigue corriendo: el SO no la mata.
 *   - LIMITACIÓN IMPORTANTE: esto NO sobrevive a "Forzar detención" desde
 *     Ajustes → Apps → [app] → Forzar detención. En ese caso el sistema mata el proceso
 *     sin posibilidad de interceptarlo. No hay solución a nivel app para este caso.
 */
public class PollingService extends Service {

    private static final String TAG = "PollingService";

    // ★ CAMBIAR esta URL al backend real antes de probar en un dispositivo o emulador.
    // 10.0.2.2 es la IP del host desde el emulador de Android Studio.
    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static final String ENDPOINT_NOVEDADES = BASE_URL + "/novedades";

    // IDs de notificación: el foreground necesita un ID fijo; las novedades usan otro
    private static final int NOTIF_ID_FOREGROUND = 1;
    private static final int NOTIF_ID_NOVEDAD = 2;

    private static final String CANAL_ID = "novedades_channel";
    private static final String CANAL_NOMBRE = "Novedades";

    private Thread pollingThread;

    // ──────────────────────────────────────────────────────────────────────────
    // onStartCommand: punto de entrada cuando se llama startForegroundService()
    // ──────────────────────────────────────────────────────────────────────────

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

        /*
         * startForeground DEBE llamarse dentro de los 5 segundos de onStartCommand,
         * de lo contrario Android lanza un ANR (Application Not Responding) y mata el Service.
         * La notificación fija le comunica al usuario que la app sigue activa en segundo plano.
         */
        Notification notifForeground = new NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("Escuchando novedades...")
            .setContentText("El servicio está conectado al backend")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            // FOREGROUND_SERVICE: no puede ser descartada por el usuario mientras el Service corre
            .setOngoing(true)
            .build();

        startForeground(NOTIF_ID_FOREGROUND, notifForeground);

        // Iniciar el loop de polling en un hilo separado.
        // NUNCA hacer red en el hilo principal: Android lanza NetworkOnMainThreadException.
        pollingThread = new Thread(this::loopDePolling);
        pollingThread.start();

        /*
         * START_STICKY: si el sistema mata el Service por memoria, lo intenta reiniciar
         * automáticamente con intent=null. Es el comportamiento adecuado para servicios
         * de larga duración que no necesitan recuperar el Intent original.
         */
        return START_STICKY;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // loopDePolling: corre en un hilo separado, nunca en el hilo principal
    // ──────────────────────────────────────────────────────────────────────────

    private void loopDePolling() {

        //Do Something
        /*
         * El hilo corre hasta que sea interrumpido (onDestroy llama a interrupt()).
         * isInterrupted() chequea la flag sin limpiarla, a diferencia de interrupted().
         */
        while (!Thread.currentThread().isInterrupted()) {

                Log.d(TAG, "Conectando a " + ENDPOINT_NOVEDADES);

                // execute() bloquea el hilo hasta que el servidor responde.
                // En long polling, el servidor retiene la conexión hasta tener datos.

                //Do Something

                // Sin sleep entre iteraciones exitosas: el propio servidor introduce la demora.
                // Si el servidor responde de inmediato sin datos, hacemos una pausa breve
                // para no saturarlo con requests consecutivas (solo llega acá si respondió vacío).


        }

        Log.d(TAG, "Loop de polling finalizado");
    }

    private void mostrarNotificacion(String mensaje) {
        // Do something
    }


    // ──────────────────────────────────────────────────────────────────────────
    // Ciclo de vida del Service
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Interrumpir el hilo de polling para que salga del loop limpiamente
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // No se usa binding; el Service se inicia con startForegroundService()
        return null;
    }
}
