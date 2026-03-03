package com.example.rovi2.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoteiroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoteiro(roteiro: Roteiro)

    @Update
    suspend fun updateRoteiro(roteiro: Roteiro)

    @Delete
    suspend fun deleteRoteiro(roteiro: Roteiro)

    @Query("SELECT * FROM roteiros ORDER BY nome ASC")
    fun getAllRoteiros(): Flow<List<Roteiro>>

    @Query("SELECT * FROM roteiros WHERE id = :id")
    fun getRoteiroById(id: Int): Flow<Roteiro>
}