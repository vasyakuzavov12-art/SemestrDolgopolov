package com.example.myapplication.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteGameDao {
    @Query("SELECT * FROM favorite_games ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(game: FavoriteGameEntity)

    @Delete
    suspend fun removeFromFavorites(game: FavoriteGameEntity)

    @Query("SELECT * FROM favorite_games WHERE id = :id")
    suspend fun isFavorite(id: Int): FavoriteGameEntity?
}