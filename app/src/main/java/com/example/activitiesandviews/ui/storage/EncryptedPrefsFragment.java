package com.example.activitiesandviews.ui.storage;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.activitiesandviews.R;

public class EncryptedPrefsFragment extends Fragment {

    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_SECRET = "api_key";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_encrypted_prefs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etValor = view.findViewById(R.id.etValor);
        Button btnGuardar = view.findViewById(R.id.btnGuardar);
        Button btnRecuperar = view.findViewById(R.id.btnRecuperar);
        Button btnLimpiar = view.findViewById(R.id.btnLimpiar);
        TextView tvResultado = view.findViewById(R.id.tvResultado);

        btnGuardar.setOnClickListener(v -> {
            String value = etValor.getText().toString().trim();
            if (value.isEmpty()) {
                tvResultado.setText("Ingresa un valor primero");
                return;
            }
            try {
                SharedPreferences prefs = buildEncryptedPrefs();
                prefs.edit().putString(KEY_SECRET, value).apply();
                tvResultado.setText("Guardado de forma cifrada:\n\"" + value + "\"");
            } catch (Exception e) {
                tvResultado.setText("Error: " + e.getMessage());
            }
        });

        btnRecuperar.setOnClickListener(v -> {
            try {
                SharedPreferences prefs = buildEncryptedPrefs();
                String stored = prefs.getString(KEY_SECRET, null);
                if (stored != null) {
                    tvResultado.setText("Recuperado y descifrado:\n\"" + stored + "\"");
                } else {
                    tvResultado.setText("No hay datos guardados");
                }
            } catch (Exception e) {
                tvResultado.setText("Error: " + e.getMessage());
            }
        });

        btnLimpiar.setOnClickListener(v -> {
            try {
                SharedPreferences prefs = buildEncryptedPrefs();
                prefs.edit().remove(KEY_SECRET).apply();
                tvResultado.setText("Dato eliminado");
            } catch (Exception e) {
                tvResultado.setText("Error: " + e.getMessage());
            }
        });
    }

    private SharedPreferences buildEncryptedPrefs() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(requireContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                requireContext(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
