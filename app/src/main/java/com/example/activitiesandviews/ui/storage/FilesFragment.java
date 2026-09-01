package com.example.activitiesandviews.ui.storage;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class FilesFragment extends Fragment {

    private static final String FILE_NAME = "mi_archivo.txt";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etContenido = view.findViewById(R.id.etContenido);
        Button btnEscribir = view.findViewById(R.id.btnEscribir);
        Button btnLeer = view.findViewById(R.id.btnLeer);
        Button btnBorrar = view.findViewById(R.id.btnBorrar);
        TextView tvResultado = view.findViewById(R.id.tvResultado);

        btnEscribir.setOnClickListener(v -> {
            String content = etContenido.getText().toString().trim();
            if (content.isEmpty()) {
                tvResultado.setText("Escribe algo primero");
                return;
            }
            try {
                File file = new File(requireContext().getFilesDir(), FILE_NAME);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content.getBytes());
                }
                tvResultado.setText("Archivo escrito en:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                tvResultado.setText("Error al escribir: " + e.getMessage());
            }
        });

        btnLeer.setOnClickListener(v -> {
            try {
                File file = new File(requireContext().getFilesDir(), FILE_NAME);
                if (!file.exists()) {
                    tvResultado.setText("El archivo no existe todavía");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                tvResultado.setText("Contenido leído:\n" + sb.toString().trim());
            } catch (Exception e) {
                tvResultado.setText("Error al leer: " + e.getMessage());
            }
        });

        btnBorrar.setOnClickListener(v -> {
            File file = new File(requireContext().getFilesDir(), FILE_NAME);
            if (file.exists() && file.delete()) {
                tvResultado.setText("Archivo eliminado");
            } else {
                tvResultado.setText("No hay archivo para eliminar");
            }
        });
    }
}
