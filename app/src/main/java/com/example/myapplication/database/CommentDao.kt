package com.example.myapplication.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE gameId = :gameId ORDER BY timestamp DESC")
    fun getCommentsForGame(gameId: Int): Flow<List<CommentEntity>>

    @Insert
    suspend fun addComment(comment: CommentEntity)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)
}