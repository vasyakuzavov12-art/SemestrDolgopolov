package com.example.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_games")
data class FavoriteGameEntity(
    @PrimaryKey
    val id: Int,
    val userId: Long,
    val name: String,
    val imageUrl: String,
    val price: String,
    val timestamp: Long = System.currentTimeMillis()
)