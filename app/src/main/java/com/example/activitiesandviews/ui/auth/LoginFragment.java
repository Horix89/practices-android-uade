package com.example.activitiesandviews.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.R;
import com.example.activitiesandviews.data.local.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final int ALLOWED_AUTHENTICATORS =
            Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;

    private static final String FAKE_TOKEN = "fake-token-abc123";

    @Inject
    TokenManager tokenManager;

    private View rootView;
    private LinearLayout layoutCredentials;
    private LinearLayout layoutBiometric;
    private TextView tvBiometricStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootView = view;
        layoutCredentials = view.findViewById(R.id.layoutCredentials);
        layoutBiometric = view.findViewById(R.id.layoutBiometric);
        tvBiometricStatus = view.findViewById(R.id.tvBiometricStatus);

        EditText etNombre = view.findViewById(R.id.etNombre);
        Button btnIngresar = view.findViewById(R.id.btnIngresar);
        Button btnUsarCredenciales = view.findViewById(R.id.btnUsarCredenciales);

        btnUsarCredenciales.setOnClickListener(v -> showCredentialForm());

        btnIngresar.setOnClickListener(v -> {
            String username = etNombre.getText().toString().trim();
            tokenManager.saveToken(FAKE_TOKEN);
            ofrecerBiometria(username);
        });

        if (tokenManager.isBiometricEnabled()) {
            showBiometricPrompt();
        } else {
            showCredentialForm();
        }
    }

    private void showCredentialForm() {
        layoutCredentials.setVisibility(View.VISIBLE);
        layoutBiometric.setVisibility(View.GONE);
    }

    private void showBiometricPrompt() {
        layoutCredentials.setVisibility(View.GONE);
        layoutBiometric.setVisibility(View.VISIBLE);
        tvBiometricStatus.setText("Verificando identidad...");

        BiometricManager manager = BiometricManager.from(requireContext());
        if (manager.canAuthenticate(ALLOWED_AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
            // Hardware no disponible o no configurado: caer al formulario
            showCredentialForm();
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Bienvenido de nuevo")
                .setSubtitle("Confirmá tu identidad para ingresar")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        String token = tokenManager.getEncryptedToken();
                        tokenManager.saveToken(token != null ? token : FAKE_TOKEN);
                        goToHome("Usuario");
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // El sistema ya muestra feedback; no se hace nada aquí
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // El usuario canceló o hubo un error terminal: ofrecer credenciales
                        showCredentialForm();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private void ofrecerBiometria(String username) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Activar acceso biométrico")
                .setMessage("¿Querés usar tu huella digital la próxima vez que inicies sesión?")
                .setPositiveButton("Sí, activar", (dialog, which) -> {
                    tokenManager.setBiometricEnabled(true);
                    tokenManager.saveEncryptedToken(FAKE_TOKEN);
                    goToHome(username);
                })
                .setNegativeButton("No, gracias", (dialog, which) -> goToHome(username))
                .setCancelable(false)
                .show();
    }

    private void goToHome(String username) {
        Bundle args = new Bundle();
        args.putString("username", username);
        Navigation.findNavController(rootView)
                .navigate(R.id.action_auth_to_home, args);
    }
}
