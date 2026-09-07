package com.clipboardmemory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ClipboardEntry>>

    @Query("SELECT * FROM clipboard_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ClipboardEntry>

    @Query("SELECT * FROM clipboard_entries WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<ClipboardEntry>>

    @Insert
    suspend fun insert(entry: ClipboardEntry): Long

    @Query("DELETE FROM clipboard_entries")
    suspend fun clearAll()
}