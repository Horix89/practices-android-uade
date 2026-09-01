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
import com.example.activitiesandviews.data.local.db.Note;
import com.example.activitiesandviews.data.local.db.NoteDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RoomFragment extends Fragment {

    @Inject
    NoteDao noteDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etNota = view.findViewById(R.id.etNota);
        Button btnGuardar = view.findViewById(R.id.btnGuardar);
        Button btnVerNotas = view.findViewById(R.id.btnVerNotas);
        Button btnBorrarTodas = view.findViewById(R.id.btnBorrarTodas);
        TextView tvResultado = view.findViewById(R.id.tvResultado);

        btnGuardar.setOnClickListener(v -> {
            String content = etNota.getText().toString().trim();
            if (content.isEmpty()) {
                tvResultado.setText("Escribe una nota primero");
                return;
            }
            Note note = new Note();
            note.content = content;
            note.createdAt = System.currentTimeMillis();

            executor.execute(() -> {
                noteDao.insert(note);
                requireActivity().runOnUiThread(() ->
                    tvResultado.setText("Nota guardada: \"" + content + "\"")
                );
            });
        });

        btnVerNotas.setOnClickListener(v ->
            executor.execute(() -> {
                List<Note> notes = noteDao.getAll();
                StringBuilder sb = new StringBuilder();
                if (notes.isEmpty()) {
                    sb.append("No hay notas guardadas");
                } else {
                    for (Note n : notes) {
                        sb.append("• ").append(n.content).append("\n");
                    }
                }
                requireActivity().runOnUiThread(() ->
                    tvResultado.setText(sb.toString().trim())
                );
            })
        );

        btnBorrarTodas.setOnClickListener(v ->
            executor.execute(() -> {
                noteDao.deleteAll();
                requireActivity().runOnUiThread(() ->
                    tvResultado.setText("Todas las notas eliminadas")
                );
            })
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
