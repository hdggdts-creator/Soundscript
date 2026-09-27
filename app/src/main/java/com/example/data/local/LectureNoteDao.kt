package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureNoteDao {
    @Query("SELECT * FROM lecture_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<LectureNoteEntity>>

    @Query("SELECT * FROM lecture_notes WHERE isBookmarked = 1 ORDER BY createdAt DESC")
    fun getBookmarkedNotes(): Flow<List<LectureNoteEntity>>

    @Query("SELECT * FROM lecture_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): LectureNoteEntity?

    @Query("""
        SELECT * FROM lecture_notes 
        WHERE title LIKE '%' || :query || '%' 
           OR specialty LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<LectureNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(entity: LectureNoteEntity): Long

    @Update
    suspend fun updateNote(entity: LectureNoteEntity)

    @Query("UPDATE lecture_notes SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmark(id: Long, isBookmarked: Boolean)

    @Query("DELETE FROM lecture_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM lecture_notes")
    suspend fun clearAll()
}
