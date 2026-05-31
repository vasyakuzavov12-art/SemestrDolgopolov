package com.example.myapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteGameEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FavoriteGameDatabase : RoomDatabase() {
    abstract fun favoriteGameDao(): FavoriteGameDao

    companion object {
        @Volatile
        private var INSTANCE: FavoriteGameDatabase? = null

        fun getInstance(context: Context): FavoriteGameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FavoriteGameDatabase::class.java,
                    "favorite_games_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}