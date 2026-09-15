package com.example.activitiesandviews.ui.biometric;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.activitiesandviews.R;

import java.util.concurrent.Executor;

public class BiometricFragment extends Fragment {

    // Combinación de autenticadores permitidos: biometría fuerte O credencial del dispositivo (PIN/patrón/contraseña)
    private static final int ALLOWED_AUTHENTICATORS =
            Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;

    private TextView tvStatus;
    private Button btnAuthenticate;
    private Button btnGoToSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_biometric, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatus = view.findViewById(R.id.tvStatus);
        btnAuthenticate = view.findViewById(R.id.btnAuthenticate);
        btnGoToSettings = view.findViewById(R.id.btnGoToSettings);

        // PASO 1: Verificar disponibilidad con BiometricManager
        checkBiometricAvailability();

        // PASO 2: Configurar click del botón principal
        btnAuthenticate.setOnClickListener(v -> launchBiometricPrompt());

        // PASO 3 (opcional): Si no hay enrolamiento, ofrecer ir a Ajustes
        btnGoToSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
            startActivity(intent);
        });
    }

    /**
     * PASO 1 — BiometricManager: preguntar si la biometría está disponible.
     * Siempre hay que hacer esta verificación antes de mostrar el prompt.
     */
    private void checkBiometricAvailability() {
        BiometricManager manager = BiometricManager.from(requireContext());
        int result = manager.canAuthenticate(ALLOWED_AUTHENTICATORS);

        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Todo OK: hay hardware, está disponible y hay credenciales enroladas
                tvStatus.setText("Listo para autenticar. Presiona el boton.");
                btnAuthenticate.setEnabled(true);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // El dispositivo no tiene sensor biométrico
                tvStatus.setText("Este dispositivo no tiene sensor biometrico.");
                btnAuthenticate.setEnabled(false);
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // El hardware existe pero no está disponible en este momento
                tvStatus.setText("El sensor biometrico no esta disponible ahora.");
                btnAuthenticate.setEnabled(false);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // No hay biometría ni PIN/patrón enrolados
                tvStatus.setText("No hay biometria enrolada en el dispositivo.");
                btnAuthenticate.setEnabled(false);
                btnGoToSettings.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * PASO 2 — PromptInfo: configurar cómo se ve el diálogo del sistema.
     * PASO 3 — BiometricPrompt: disparar el prompt y escuchar los tres callbacks.
     */
    private void launchBiometricPrompt() {
        // PromptInfo define el texto del diálogo
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Práctica Biometría")
                .setSubtitle("Confirmá tu identidad")
                .setDescription("Usá tu huella digital o el PIN del dispositivo")
                // Con DEVICE_CREDENTIAL no se puede usar setNegativeButtonText()
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build();

        // El Executor determina en qué hilo corren los callbacks (main thread)
        Executor executor = ContextCompat.getMainExecutor(requireContext());

        // BiometricPrompt recibe el Fragment, el Executor y los callbacks
        BiometricPrompt biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        // Callback 1: la biometría fue validada correctamente
                        tvStatus.setText("Autenticacion exitosa!");
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Callback 2: el intento no coincide, pero el prompt sigue abierto
                        tvStatus.setText("Intento fallido. Reintenta.");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        // Callback 3: error del sistema o el usuario canceló
                        tvStatus.setText("Error: " + errString);
                    }
                }
        );

        // Lanzar el prompt pasándole el PromptInfo configurado
        biometricPrompt.authenticate(promptInfo);
    }
}
