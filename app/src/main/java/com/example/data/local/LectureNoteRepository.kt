package com.example.data.local

import com.example.data.model.MedicalLectureNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LectureNoteRepository(private val dao: LectureNoteDao) {

    fun getAllNotes(): Flow<List<MedicalLectureNote>> =
        dao.getAllNotes().map { list -> list.map { it.toDomain() } }

    fun getBookmarkedNotes(): Flow<List<MedicalLectureNote>> =
        dao.getBookmarkedNotes().map { list -> list.map { it.toDomain() } }

    fun searchNotes(query: String): Flow<List<MedicalLectureNote>> =
        dao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    suspend fun getNoteById(id: Long): MedicalLectureNote? =
        dao.getNoteById(id)?.toDomain()

    suspend fun saveNote(note: MedicalLectureNote): Long =
        dao.insertNote(LectureNoteEntity.fromDomain(note))

    suspend fun toggleBookmark(id: Long, currentBookmarked: Boolean) =
        dao.setBookmark(id, !currentBookmarked)

    suspend fun deleteNote(id: Long) =
        dao.deleteNoteById(id)
}
