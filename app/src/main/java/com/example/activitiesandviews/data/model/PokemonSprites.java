package com.example.activitiesandviews.data.model;

import com.google.gson.annotations.SerializedName;

public class PokemonSprites {

    @SerializedName("other")
    private PokemonSpritesOther other;

    public PokemonSpritesOther getOther() { return other; }

    public static class PokemonSpritesOther {

        @SerializedName("official-artwork")
        private PokemonOfficialArtwork officialArtwork;

        public PokemonOfficialArtwork getOfficialArtwork() { return officialArtwork; }

        public static class PokemonOfficialArtwork {

            @SerializedName("front_default")
            private String frontDefault;

            public String getFrontDefault() { return frontDefault; }
        }
    }
}
