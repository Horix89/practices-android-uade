package com.example.activitiesandviews.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PokemonListResponse {

    @SerializedName("count")
    private int count;

    @SerializedName("results")
    private List<PokemonResult> results;

    public int getCount() {
        return count;
    }

    public List<PokemonResult> getResults() {
        return results;
    }
}
