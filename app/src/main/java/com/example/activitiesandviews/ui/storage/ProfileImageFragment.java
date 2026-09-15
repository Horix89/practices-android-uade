package com.example.activitiesandviews.ui.storage;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.activitiesandviews.R;

public class ProfileImageFragment extends Fragment {

    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_IMAGE_URI = "image_uri";

    private ImageView ivProfile;

    // -------------------------------------------------------------------------
    // ActivityResultLaunchers — deben registrarse antes de onStart()
    // -------------------------------------------------------------------------

    /**
     * Lanzador para solicitar un permiso en runtime.
     * Contrato: ActivityResultContracts.RequestPermission
     *   - Input:  String  — nombre del permiso a solicitar
     *   - Output: Boolean — true si fue concedido, false si fue denegado
     */
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            openGallery();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Permiso denegado. No se puede acceder a la galería.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    /**
     * Lanzador para abrir la galería y recibir la Uri de la imagen elegida.
     * Contrato: ActivityResultContracts.GetContent
     *   - Input:  String — MIME type del contenido a elegir ("image/*")
     *   - Output: Uri    — Uri de la imagen seleccionada, o null si se canceló
     */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            saveAndDisplay(uri);
                        }
                    });

    // -------------------------------------------------------------------------
    // Ciclo de vida del Fragment
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivProfile = view.findViewById(R.id.ivProfile);
        Button btnSelect = view.findViewById(R.id.btnSelectImage);

        // Si ya existe una Uri guardada, la mostramos al entrar al Fragment
        loadSavedImage();

        btnSelect.setOnClickListener(v -> checkPermissionAndOpenGallery());
    }

    // -------------------------------------------------------------------------
    // Paso 2: Pedir permiso en runtime
    // -------------------------------------------------------------------------

    /**
     * Verifica si ya tenemos el permiso. Si no, lo solicita a través del launcher.
     * En Android 13+ (API 33, TIRAMISU) se usa READ_MEDIA_IMAGES.
     * En versiones anteriores se usa READ_EXTERNAL_STORAGE.
     */
    private void checkPermissionAndOpenGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            // Permiso ya otorgado → abrimos la galería directamente
            openGallery();
        } else {
            // No tenemos permiso → lanzamos el diálogo del sistema
            permissionLauncher.launch(permission);
        }
    }

    // -------------------------------------------------------------------------
    // Paso 3: Abrir la galería
    // -------------------------------------------------------------------------

    /**
     * Lanza el selector de imágenes pasando el MIME type "image/*".
     * El sistema abre la galería y devuelve la Uri de la imagen elegida
     * directamente en el callback del galleryLauncher.
     */
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    // -------------------------------------------------------------------------
    // Pasos 4 y 5: Recibir Uri y persistirla
    // -------------------------------------------------------------------------

    /**
     * Persiste la Uri en SharedPreferences y muestra la imagen.
     * La Uri es una referencia directa a la imagen en la galería —
     * no es necesario copiarla ni convertirla.
     */
    private void saveAndDisplay(Uri uri) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IMAGE_URI, uri.toString()).apply();

        displayImage(uri);
    }

    /**
     * Lee la Uri guardada en SharedPreferences y carga la imagen si existe.
     */
    private void loadSavedImage() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_IMAGE_URI, null);
        if (uriString != null) {
            displayImage(Uri.parse(uriString));
        }
    }

    // -------------------------------------------------------------------------
    // Paso 6: Mostrar con Glide + circleCrop
    // -------------------------------------------------------------------------

    /**
     * Usa Glide para cargar la imagen desde la Uri y aplicar
     * la transformación circleCrop() que recorta la imagen en forma circular.
     */
    private void displayImage(Uri uri) {
        Glide.with(this)
                .load(uri)       // carga directamente desde la Uri
                .circleCrop()    // recorta en círculo
                .into(ivProfile); // destino: el ImageView
    }
}
