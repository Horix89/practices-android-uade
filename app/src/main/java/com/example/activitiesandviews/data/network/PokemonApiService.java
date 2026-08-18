package com.example.activitiesandviews.data.network;

import com.example.activitiesandviews.data.model.PokemonDetail;
import com.example.activitiesandviews.data.model.PokemonListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PokemonApiService {

    @GET("pokemon")
    Call<PokemonListResponse> getPokemon(@Query("limit") int limit);

    @GET("pokemon/{name}")
    Call<PokemonDetail> getPokemonDetail(@Path("name") String name);
}
