package com.example.activitiesandviews.ui.material;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Demuestra el uso correcto de Material Components.
 *
 * La Toolbar ya NO se gestiona aquí. MainActivity la conecta con NavigationUI:
 *   - El título "Material UI" viene del android:label en home_nav_graph.xml
 *   - El botón back (si aplica) aparece/desaparece automáticamente
 *
 * Puntos clave para mostrar en clase:
 *   - Ningún color hardcodeado en el layout → todo viene del Theme
 *   - TextInputLayout.setError() integra validación con animación
 *   - Snackbar en lugar de Toast (respeta el FAB con CoordinatorLayout)
 *   - En modo oscuro, todo cambia sin tocar una sola línea de este Fragment
 */
public class MaterialDemoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_material_demo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputLayout tilEmail = view.findViewById(R.id.tilEmail);
        MaterialButton btnAction = view.findViewById(R.id.btnMaterialAction);
        MaterialButton btnGoToLegacy = view.findViewById(R.id.btnGoToLegacy);
        FloatingActionButton fab = view.findViewById(R.id.fab);

        btnAction.setOnClickListener(v -> {
            String email = tilEmail.getEditText() != null
                    ? tilEmail.getEditText().getText().toString().trim()
                    : "";

            if (email.isEmpty() || !email.contains("@")) {
                // setError(): integrado con el campo, animación y color automáticos
                tilEmail.setError("Ingresá un email válido");
            } else {
                tilEmail.setError(null);
                // Snackbar sube por encima del FAB gracias al CoordinatorLayout
                Snackbar.make(view, "Email válido: " + email, Snackbar.LENGTH_SHORT).show();
            }
        });

        btnGoToLegacy.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_material_to_legacy));

        fab.setOnClickListener(v ->
                Snackbar.make(view, "FAB: colorSecondary del Theme", Snackbar.LENGTH_SHORT).show());
    }
}
