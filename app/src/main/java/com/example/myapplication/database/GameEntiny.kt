package com.example.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_games")
data class GameEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val rating: Double,
    val released: String?,
    val timestamp: Long = System.currentTimeMillis()
)