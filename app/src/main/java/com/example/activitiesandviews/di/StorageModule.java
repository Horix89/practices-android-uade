package com.example.activitiesandviews.di;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.room.Room;

import com.example.activitiesandviews.data.local.db.AppDatabase;
import com.example.activitiesandviews.data.local.db.NoteDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class StorageModule {

    @Provides
    @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "notes_db")
                .build();
    }

    @Provides
    @Singleton
    public NoteDao provideNoteDao(AppDatabase database) {
        return database.noteDao();
    }

    @Provides
    @Singleton
    public SharedPreferences providePlainSharedPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences("demo_prefs", Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public RxDataStore<Preferences> provideDataStore(@ApplicationContext Context context) {
        return new RxPreferenceDataStoreBuilder(context, "demo_datastore").build();
    }
}
