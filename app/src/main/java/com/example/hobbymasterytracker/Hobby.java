package com.example.hobbymasterytracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hobbies")
public class Hobby {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public double totalHours;
    public String iconName;

    public Hobby(String name, String iconName) {
        this.name = name;
        this.iconName = iconName;
        this.totalHours = 0.0;
    }

    // Room requires a no-arg constructor when there are other constructors
    public Hobby() {}
}