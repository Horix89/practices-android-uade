package com.example.activitiesandviews.ui.pokemon;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.activitiesandviews.R;
import com.example.activitiesandviews.data.model.PokemonResult;

import java.util.List;

public class PokemonAdapter extends RecyclerView.Adapter<PokemonAdapter.ViewHolder> {

    // Interfaz que el Fragment implementa para recibir el click
    public interface OnPokemonClickListener {
        void onPokemonClick(String pokemonName);
    }

    private final List<PokemonResult> items;
    private final OnPokemonClickListener listener;

    public PokemonAdapter(List<PokemonResult> items, OnPokemonClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pokemon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PokemonResult pokemon = items.get(position);

        holder.tvName.setText(pokemon.getName());

        Glide.with(holder.itemView.getContext())
                .load(pokemon.getSpriteUrl())
                .into(holder.ivPokemon);

        // Al hacer click en el ítem, notificamos al Fragment con el nombre del pokemon
        holder.itemView.setOnClickListener(v -> listener.onPokemonClick(pokemon.getName()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPokemon;
        TextView tvName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPokemon = itemView.findViewById(R.id.ivPokemon);
            tvName = itemView.findViewById(R.id.tvPokemonName);
        }
    }
}
