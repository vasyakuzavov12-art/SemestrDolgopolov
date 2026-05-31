package com.example.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Int,
    val userId: Long,
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)