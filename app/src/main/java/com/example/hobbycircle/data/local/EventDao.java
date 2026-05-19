package com.example.hobbycircle.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {

    @Query("SELECT * FROM events ORDER BY dateTime ASC")
    List<EventEntity> getAllEvents();

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    EventEntity getEventById(String eventId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(EventEntity event);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EventEntity> events);

    @Query("DELETE FROM events")
    void clearAll();

    @Query("DELETE FROM events WHERE id = :eventId")
    void deleteById(String eventId);
}