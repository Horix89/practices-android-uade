package com.example.activitiesandviews.data.model;

import com.google.gson.annotations.SerializedName;

public class PokemonResult {

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    // La URL de la lista tiene la forma: https://pokeapi.co/api/v2/pokemon/1/
    // Extraemos el ID del último segmento para construir la URL del sprite
    // sin necesidad de hacer una segunda llamada a la API.
    public String getSpriteUrl() {
        String[] parts = url.split("/");
        String id = parts[parts.length - 1];
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + id + ".png";
    }
}
