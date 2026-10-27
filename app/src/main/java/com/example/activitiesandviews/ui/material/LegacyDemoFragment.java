package com.example.activitiesandviews.ui.material;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.R;

/**
 * Demuestra los problemas de no usar Material Components.
 *
 * La Toolbar ya NO se gestiona aquí. MainActivity la conecta con NavigationUI:
 *   - El título "Legacy UI" viene del android:label en home_nav_graph.xml
 *   - El botón back aparece automáticamente porque este destino NO es top-level
 *     en AppBarConfiguration → no se escribió ni una línea para eso
 *
 * Puntos clave para mostrar en clase:
 *   - Los colores del layout están hardcodeados: cambiar el Theme no tiene efecto
 *   - La validación requiere manejo manual: TextView de error, visibilidad, texto
 *   - Toast en lugar de Snackbar (no es un componente Material)
 *   - En modo oscuro, el fondo blanco y el texto negro se quedan igual → ilegible
 */
public class LegacyDemoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_legacy_demo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etEmail = view.findViewById(R.id.etLegacyEmail);
        TextView tvEmailError = view.findViewById(R.id.tvEmailError);
        Button btnAction = view.findViewById(R.id.btnLegacyAction);
        Button btnBackToMaterial = view.findViewById(R.id.btnBackToMaterial);

        btnAction.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !email.contains("@")) {
                // Validación manual: sin animación, sin integración con el campo
                tvEmailError.setVisibility(View.VISIBLE);
                tvEmailError.setText("Ingresá un email válido");
            } else {
                tvEmailError.setVisibility(View.GONE);
                // Toast clásico: no es un componente Material
                Toast.makeText(requireContext(), "Email: " + email, Toast.LENGTH_SHORT).show();
            }
        });

        // Este botón es redundante (la flecha de la Toolbar ya hace lo mismo)
        // pero se deja para comparar: en el enfoque legacy, el back era manual
        btnBackToMaterial.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
    }
}
