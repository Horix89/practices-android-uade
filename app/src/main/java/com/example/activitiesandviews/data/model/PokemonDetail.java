package com.example.activitiesandviews.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PokemonDetail {

    @SerializedName("name")
    private String name;

    @SerializedName("height")
    private int height;

    @SerializedName("weight")
    private int weight;

    @SerializedName("sprites")
    private PokemonSprites sprites;

    @SerializedName("types")
    private List<PokemonTypeSlot> types;

    public String getName() { return name; }
    public int getHeight() { return height; }
    public int getWeight() { return weight; }
    public PokemonSprites getSprites() { return sprites; }
    public List<PokemonTypeSlot> getTypes() { return types; }
}
