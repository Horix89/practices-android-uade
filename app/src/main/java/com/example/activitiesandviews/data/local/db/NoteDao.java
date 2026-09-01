package com.example.activitiesandviews.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void insert(Note note);

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    List<Note> getAll();

    @Query("DELETE FROM notes")
    void deleteAll();
}
