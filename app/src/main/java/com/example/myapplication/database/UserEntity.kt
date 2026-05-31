package com.example.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,  // Для простоты - в реальном проекте хэшируй
    val steamId: String? = null,  // Привязанный Steam ID
    val steamName: String? = null,
    val isLoggedIn: Boolean = false
)