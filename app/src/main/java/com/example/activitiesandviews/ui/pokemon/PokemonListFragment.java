package com.example.activitiesandviews.ui.pokemon;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.activitiesandviews.R;
import com.example.activitiesandviews.data.model.PokemonListResponse;
import com.example.activitiesandviews.data.network.PokemonApiService;
import com.example.activitiesandviews.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PokemonListFragment extends Fragment {

    private static final String TAG = "PokemonListFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pokemon_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView tvError = view.findViewById(R.id.tvError);
        RecyclerView rvPokemon = view.findViewById(R.id.rvPokemon);

        rvPokemon.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Mostrar el loading
        progressBar.setVisibility(View.VISIBLE);

        // Crear el servicio directamente (sin DI)
        PokemonApiService apiService = RetrofitClient.getInstance()
                .create(PokemonApiService.class);

        Call<PokemonListResponse> call = apiService.getPokemon(20);

        call.enqueue(new Callback<PokemonListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PokemonListResponse> call,
                                   @NonNull Response<PokemonListResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    PokemonAdapter adapter = new PokemonAdapter(
                            response.body().getResults(),
                            pokemonName -> {
                                // Al hacer click, pasamos el nombre como argumento y navegamos al detalle
                                Bundle args = new Bundle();
                                args.putString("pokemonName", pokemonName);
                                Navigation.findNavController(view)
                                        .navigate(R.id.action_pokemonList_to_detail, args);
                            }
                    );
                    rvPokemon.setAdapter(adapter);
                    rvPokemon.setVisibility(View.VISIBLE);
                } else {
                    tvError.setText("Error " + response.code() + ": no se pudo cargar la lista.");
                    tvError.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error HTTP: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PokemonListResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvError.setText("Error de red: " + t.getMessage());
                tvError.setVisibility(View.VISIBLE);
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }
}
