package com.example.activitiesandviews.di;

import android.content.Context;

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
}
