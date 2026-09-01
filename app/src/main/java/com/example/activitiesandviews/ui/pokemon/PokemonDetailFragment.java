package com.example.activitiesandviews.ui.pokemon;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.activitiesandviews.R;
import com.example.activitiesandviews.data.model.PokemonDetail;
import com.example.activitiesandviews.data.model.PokemonTypeSlot;
import com.example.activitiesandviews.data.network.PokemonApiService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class PokemonDetailFragment extends Fragment {

    private static final String TAG = "PokemonDetailFragment";

    @Inject
    PokemonApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pokemon_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recuperar el nombre pasado como argumento desde PokemonListFragment
        String pokemonName = getArguments() != null
                ? getArguments().getString("pokemonName", "")
                : "";

        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView tvError = view.findViewById(R.id.tvError);
        ImageView ivPokemon = view.findViewById(R.id.ivPokemon);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvTypes = view.findViewById(R.id.tvTypes);
        TextView tvHeight = view.findViewById(R.id.tvHeight);
        TextView tvWeight = view.findViewById(R.id.tvWeight);

        progressBar.setVisibility(View.VISIBLE);

        apiService.getPokemonDetail(pokemonName).enqueue(new Callback<PokemonDetail>() {
            @Override
            public void onResponse(@NonNull Call<PokemonDetail> call,
                                   @NonNull Response<PokemonDetail> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    PokemonDetail pokemon = response.body();

                    // Imagen
                    String imageUrl = pokemon.getSprites()
                            .getOther()
                            .getOfficialArtwork()
                            .getFrontDefault();

                    Glide.with(requireContext())
                            .load(imageUrl)
                            .into(ivPokemon);

                    // Tipos: ["grass", "poison"] → "Tipos: grass / poison"
                    List<String> typeNames = new ArrayList<>();
                    for (PokemonTypeSlot slot : pokemon.getTypes()) {
                        typeNames.add(slot.getType().getName());
                    }

                    // Altura en decímetros → metros  |  Peso en hectogramos → kg
                    tvName.setText(pokemon.getName());
                    tvTypes.setText("Tipos: " + String.join(" / ", typeNames));
                    tvHeight.setText("Altura: " + (pokemon.getHeight() / 10.0) + " m");
                    tvWeight.setText("Peso: " + (pokemon.getWeight() / 10.0) + " kg");

                    ivPokemon.setVisibility(View.VISIBLE);
                    tvName.setVisibility(View.VISIBLE);
                    tvTypes.setVisibility(View.VISIBLE);
                    tvHeight.setVisibility(View.VISIBLE);
                    tvWeight.setVisibility(View.VISIBLE);

                } else {
                    tvError.setText("Error " + response.code() + ": no se pudo cargar el detalle.");
                    tvError.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error HTTP: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PokemonDetail> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvError.setText("Error de red: " + t.getMessage());
                tvError.setVisibility(View.VISIBLE);
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }
}
