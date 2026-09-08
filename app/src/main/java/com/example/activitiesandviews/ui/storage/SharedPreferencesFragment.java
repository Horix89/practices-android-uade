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

import com.example.activitiesandviews.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SharedPreferencesFragment extends Fragment {

    private static final String KEY_VALOR = "valor_demo";

    @Inject
    SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shared_preferences, container, false);
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
            sharedPreferences.edit().putString(KEY_VALOR, value).apply();
            tvResultado.setText("Guardado:\n\"" + value + "\"");
        });

        btnRecuperar.setOnClickListener(v -> {
            String stored = sharedPreferences.getString(KEY_VALOR, null);
            if (stored != null) {
                tvResultado.setText("Recuperado:\n\"" + stored + "\"");
            } else {
                tvResultado.setText("No hay datos guardados");
            }
        });

        btnLimpiar.setOnClickListener(v -> {
            sharedPreferences.edit().remove(KEY_VALOR).apply();
            tvResultado.setText("Dato eliminado");
        });
    }
}
