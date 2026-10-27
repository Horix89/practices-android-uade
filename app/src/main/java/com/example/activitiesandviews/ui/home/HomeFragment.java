package com.example.activitiesandviews.ui.home;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.service.PollingService;

import com.example.activitiesandviews.R;

public class HomeFragment extends Fragment {

    // Launcher para solicitar POST_NOTIFICATIONS; debe registrarse antes de onStart()
    private ActivityResultLauncher<String> notifPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /*
         * WindowInsets: valores que Android expone para saber cuánto espacio
         * ocupan la barra de estado (arriba) y la barra de navegación (abajo).
         *
         * Se aplica padding a la vista raíz para que el contenido nunca quede
         * tapado por esas barras del sistema. Es la alternativa correcta a
         * android:fitsSystemWindows="true", que tiene comportamiento impredecible
         * en layouts complejos.
         *
         * En este caso la Toolbar ya absorbe el inset superior y el BottomNav
         * el inferior (ver MainActivity.applyWindowInsets), por lo que aquí
         * solo llegan los insets laterales (útiles en modo landscape o con
         * barras de gestos en los costados).
         */
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int dp24 = Math.round(24 * getResources().getDisplayMetrics().density);
            v.setPadding(
                dp24 + bars.left,
                dp24 + bars.top,
                dp24 + bars.right,
                dp24 + bars.bottom
            );
            return insets;
        });

        String username = getArguments() != null
                ? getArguments().getString("username", "")
                : "";

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        Button btnPokemon = view.findViewById(R.id.btnPokemon);
        Button btnStorage = view.findViewById(R.id.btnStorage);
        Button btnBiometric = view.findViewById(R.id.btnBiometric);
        Button btnMaterialDemo = view.findViewById(R.id.btnMaterialDemo);
        Button btnQr = view.findViewById(R.id.btnQr);
        Button btnPolling = view.findViewById(R.id.btnPolling);
        Button btnNotifEjemplo = view.findViewById(R.id.btnNotifEjemplo);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        tvWelcome.setText("Bienvenido, " + username + "!");

        btnPokemon.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_pokemon));

        btnStorage.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_storage));

        btnBiometric.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_biometric));

        btnMaterialDemo.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_material_demo));

        btnQr.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_qr));

        // startForegroundService: versión especial de startService para Foreground Services.
        // Requerida desde API 26; como minSdk=30, es la única forma válida de iniciarlo.
        btnPolling.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PollingService.class);
            requireContext().startForegroundService(intent);
        });

        // Botón de prueba: verifica permiso antes de mostrar la notificación
        btnNotifEjemplo.setOnClickListener(v -> pedirPermisoNotificaciones());

        btnLogout.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.auth_nav_graph, null, navOptions);
        });
    }

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

    /**
     * Muestra una notificación local inmediata sin necesitar el backend.
     * Útil para verificar que el canal de notificaciones funciona correctamente.
     * Usa el mismo canal ("novedades_channel") que el PollingService.
     */
    private void mostrarNotificacionEjemplo() {
        NotificationManager manager =
            (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear el canal (requerido en API 26+; si ya existe, esta llamada es no-op)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                "novedades_channel",
                "Novedades",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(canal);
        }

        Notification notif = new NotificationCompat.Builder(requireContext(), "novedades_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notificación de ejemplo")
            .setContentText("¡Funciona! Esto simula una novedad del backend.")
            .setAutoCancel(true)
            .build();

        manager.notify(999, notif);
    }
}
