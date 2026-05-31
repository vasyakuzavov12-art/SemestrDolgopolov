package com.example.myapplication.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM favorite_games ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("SELECT * FROM favorite_games WHERE id = :id")
    suspend fun getById(id: Int): GameEntity?
}