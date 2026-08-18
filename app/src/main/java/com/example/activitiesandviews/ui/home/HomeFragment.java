package com.example.activitiesandviews.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.activitiesandviews.R;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String username = getArguments() != null
                ? getArguments().getString("username", "")
                : "";

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        Button btnPokemon = view.findViewById(R.id.btnPokemon);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        tvWelcome.setText("Bienvenido, " + username + "!");

        btnPokemon.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_pokemon));

        btnLogout.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.auth_nav_graph, null, navOptions);
        });
    }
}
