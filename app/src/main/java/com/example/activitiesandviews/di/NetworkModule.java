package com.example.activitiesandviews.di;

import android.util.Log;

import com.example.activitiesandviews.data.local.TokenManager;
import com.example.activitiesandviews.data.network.PokemonApiService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String BASE_URL = "https://pokeapi.co/api/v2/";
    private static final String TAG = "NetworkModule";

    @Provides
    @Singleton
    public OkHttpClient provideOkHttp(TokenManager tokenManager) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = tokenManager.getToken();
                    Request request = chain.request();
                    if (token != null) {
                        request = request.newBuilder()
                                .addHeader("Authorization", "Bearer " + token)
                                .build();
                        Log.d(TAG, "Authorization header agregado: Bearer " + token);
                    } else {
                        Log.d(TAG, "Sin token — request sin Authorization header");
                    }
                    return chain.proceed(request);
                })
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public PokemonApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(PokemonApiService.class);
    }
}
