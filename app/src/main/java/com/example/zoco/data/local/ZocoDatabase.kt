package com.example.zoco.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        NotificationEntity::class,
        MediaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZocoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun notificationDao(): NotificationDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: ZocoDatabase? = null

        fun getInstance(context: Context): ZocoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZocoDatabase::class.java,
                    "zoco_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
