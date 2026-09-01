package com.example.activitiesandviews.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.R;
import com.example.activitiesandviews.data.local.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Inject
    TokenManager tokenManager;

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

        EditText etNombre = view.findViewById(R.id.etNombre);
        Button btnIngresar = view.findViewById(R.id.btnIngresar);

        btnIngresar.setOnClickListener(v -> {
            String username = etNombre.getText().toString().trim();

            // Simula el login guardando un token hardcodeado
            tokenManager.saveToken("fake-token-abc123");

            Bundle args = new Bundle();
            args.putString("username", username);

            Navigation.findNavController(view)
                    .navigate(R.id.action_auth_to_home, args);
        });
    }
}
