package com.example.activitiesandviews.data.model;

import com.google.gson.annotations.SerializedName;

public class PokemonTypeSlot {

    @SerializedName("type")
    private PokemonTypeInfo type;

    public PokemonTypeInfo getType() { return type; }

    public static class PokemonTypeInfo {

        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }
}
