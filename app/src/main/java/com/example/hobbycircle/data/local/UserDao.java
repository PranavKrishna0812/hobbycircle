package com.example.hobbycircle.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserEntity getUserById(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserEntity user);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(String userId);
}
