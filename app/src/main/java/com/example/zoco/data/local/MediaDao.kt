package com.example.zoco.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE productId = :productId ORDER BY createdAt ASC")
    fun getMediaByProductId(productId: Long): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getMediaById(id: Long): MediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<MediaEntity>)

    @Update
    suspend fun updateMedia(media: MediaEntity): Int

    @Delete
    suspend fun deleteMedia(media: MediaEntity): Int

    @Query("DELETE FROM media WHERE productId = :productId")
    suspend fun deleteMediaByProductId(productId: Long): Int
}
