package com.example.hobbymasterytracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface HobbyDao {
    @Query("SELECT * FROM hobbies")
    LiveData<List<Hobby>> getAll();

    @Insert
    void insert(Hobby hobby);

    @Update
    void update(Hobby hobby);

    @Delete
    void delete(Hobby hobby);

    @Query("DELETE FROM hobbies")
    void deleteAll();
}