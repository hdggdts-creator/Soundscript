package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.GeminiMedicalService
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.data.local.LectureNoteRepository
import com.example.data.local.SoundScriptDatabase
import com.example.data.model.MedicalLectureNote
import com.example.data.sample.SampleLectures
import com.example.firebase.FirebaseManager
import com.example.firebase.UserProfile
import com.example.pdf.MedicalNotesPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class StudyMode {
    NOTES,
    FLASHCARDS,
    QUIZ
}

data class UiFlashcard(
    val title: String,
    val front: String,
    val back: String,
    val tag: String
)

class SoundScriptViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SoundScriptDatabase.getInstance(application)
    private val repository = LectureNoteRepository(database.lectureNoteDao())
    val geminiService = GeminiMedicalService(application)
    val pdfGenerator = MedicalNotesPdfGenerator(application)

    val recorderManager = AudioRecorderManager(application)
    val playerManager = AudioPlayerManager()
    val firebaseManager = FirebaseManager(application)

    val currentUser: StateFlow<UserProfile?> = firebaseManager.currentUser
    val isFirebaseConfigured: StateFlow<Boolean> = firebaseManager.isFirebaseConfigured
    val authLoading: StateFlow<Boolean> = firebaseManager.authLoading
    val authError: StateFlow<String?> = firebaseManager.authError

    // UI States
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _allNotes = MutableStateFlow<List<MedicalLectureNote>>(emptyList())
    val allNotes: StateFlow<List<MedicalLectureNote>> = _allNotes.asStateFlow()

    private val _currentNote = MutableStateFlow<MedicalLectureNote?>(null)
    val currentNote: StateFlow<MedicalLectureNote?> = _currentNote.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSpecialty = MutableStateFlow("All")
    val selectedSpecialty: StateFlow<String> = _selectedSpecialty.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingStatus = MutableStateFlow("")
    val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _studyMode = MutableStateFlow(StudyMode.NOTES)
    val studyMode: StateFlow<StudyMode> = _studyMode.asStateFlow()

    private val _currentFlashcardIndex = MutableStateFlow(0)
    val currentFlashcardIndex: StateFlow<Int> = _currentFlashcardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _recordedAudioFile = MutableStateFlow<File?>(null)
    val recordedAudioFile: StateFlow<File?> = _recordedAudioFile.asStateFlow()

    private val _pickedAudioUri = MutableStateFlow<Uri?>(null)
    val pickedAudioUri: StateFlow<Uri?> = _pickedAudioUri.asStateFlow()

    private val _pickedAudioFileName = MutableStateFlow<String?>(null)
    val pickedAudioFileName: StateFlow<String?> = _pickedAudioFileName.asStateFlow()

    init {
        observeNotes()
        seedSampleDataIfEmpty()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            repository.getAllNotes().collectLatest { notes ->
                _allNotes.value = notes
                if (_currentNote.value == null && notes.isNotEmpty()) {
                    _currentNote.value = notes.first()
                }
            }
        }
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.lectureNoteDao().getAllNotes()
            // We insert initial sample lectures so students can experience high-yield notes right away
            for (sample in SampleLectures.sampleLecturesList) {
                val found = database.lectureNoteDao().getNoteById(sample.id)
                if (found == null) {
                    repository.saveNote(sample)
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun selectNote(note: MedicalLectureNote) {
        _currentNote.value = note
        _studyMode.value = StudyMode.NOTES
        _currentFlashcardIndex.value = 0
        _isCardFlipped.value = false
        _selectedTab.value = 1 // Switch to details tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSpecialty(specialty: String) {
        _selectedSpecialty.value = specialty
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun setStudyMode(mode: StudyMode) {
        _studyMode.value = mode
        _currentFlashcardIndex.value = 0
        _isCardFlipped.value = false
    }

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun nextCard(total: Int) {
        if (total > 0) {
            _currentFlashcardIndex.value = (_currentFlashcardIndex.value + 1) % total
            _isCardFlipped.value = false
        }
    }

    fun prevCard(total: Int) {
        if (total > 0) {
            _currentFlashcardIndex.value = if (_currentFlashcardIndex.value - 1 < 0) total - 1 else _currentFlashcardIndex.value - 1
            _isCardFlipped.value = false
        }
    }

    // Audio recording handlers
    fun startRecording() {
        val file = recorderManager.startRecording()
        _recordedAudioFile.value = file
        _pickedAudioUri.value = null
        _pickedAudioFileName.value = null
    }

    fun pauseRecording() {
        recorderManager.pauseRecording()
    }

    fun resumeRecording() {
        recorderManager.resumeRecording()
    }

    fun stopRecording() {
        val file = recorderManager.stopRecording()
        if (file != null && file.exists()) {
            _recordedAudioFile.value = file
        }
    }

    fun clearAudioSelection() {
        _recordedAudioFile.value = null
        _pickedAudioUri.value = null
        _pickedAudioFileName.value = null
    }

    fun setPickedAudio(uri: Uri, fileName: String) {
        _pickedAudioUri.value = uri
        _pickedAudioFileName.value = fileName
        _recordedAudioFile.value = null
    }

    fun generateFromCurrentAudio(
        titleHint: String,
        specialtyHint: String
    ) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val audioFile = _recordedAudioFile.value ?: copyUriToCacheFile(_pickedAudioUri.value)
            if (audioFile == null || !audioFile.exists()) {
                _errorMessage.value = app.getString(R.string.msg_no_audio_file)
                return@launch
            }

            _isProcessing.value = true
            _processingStatus.value = app.getString(R.string.msg_status_uploading)

            val result = geminiService.generateNotesFromAudio(audioFile, titleHint, specialtyHint)

            result.fold(
                onSuccess = { generatedNote ->
                    _processingStatus.value = app.getString(R.string.msg_status_saving_clinical)
                    val noteWithAudio = generatedNote.copy(
                        audioFilePath = audioFile.absolutePath,
                        audioDurationSeconds = if (generatedNote.audioDurationSeconds > 0) generatedNote.audioDurationSeconds else 300
                    )
                    val id = repository.saveNote(noteWithAudio)
                    val saved = noteWithAudio.copy(id = id)
                    _currentNote.value = saved
                    _successMessage.value = app.getString(R.string.msg_lecture_transcribed_success)
                    _isProcessing.value = false
                    _selectedTab.value = 1
                    
                    // Firestore cloud persistence
                    viewModelScope.launch(Dispatchers.IO) {
                        firebaseManager.syncNoteToFirestore(saved)
                    }
                },
                onFailure = { error ->
                    Log.e("SoundScriptViewModel", "Audio processing failed", error)
                    _isProcessing.value = false
                    _errorMessage.value = error.message ?: app.getString(R.string.msg_failed_generate)
                }
            )
        }
    }

    fun generateFromText(
        transcriptOrNotes: String,
        titleHint: String,
        specialtyHint: String
    ) {
        val app = getApplication<Application>()
        if (transcriptOrNotes.isBlank()) {
            _errorMessage.value = app.getString(R.string.msg_enter_lecture_text)
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _processingStatus.value = app.getString(R.string.msg_status_analyzing_text)

            val result = geminiService.generateNotesFromText(transcriptOrNotes, titleHint, specialtyHint)

            result.fold(
                onSuccess = { note ->
                    _processingStatus.value = app.getString(R.string.msg_status_saving_text)
                    val id = repository.saveNote(note)
                    val saved = note.copy(id = id)
                    _currentNote.value = saved
                    _successMessage.value = app.getString(R.string.msg_notes_saved_success)
                    _isProcessing.value = false
                    _selectedTab.value = 1
                    
                    // Firestore cloud persistence
                    viewModelScope.launch(Dispatchers.IO) {
                        firebaseManager.syncNoteToFirestore(saved)
                    }
                },
                onFailure = { error ->
                    Log.e("SoundScriptViewModel", "Text notes generation failed", error)
                    _isProcessing.value = false
                    _errorMessage.value = error.message ?: app.getString(R.string.msg_failed_generate)
                }
            )
        }
    }

    private suspend fun copyUriToCacheFile(uri: Uri?): File? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val cacheDir = File(getApplication<Application>().cacheDir, "imported_audio")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val tempFile = File(cacheDir, "imported_${System.currentTimeMillis()}.m4a")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("SoundScriptViewModel", "Error copying uri to file", e)
            null
        }
    }

    fun toggleBookmark(note: MedicalLectureNote) {
        viewModelScope.launch {
            repository.toggleBookmark(note.id, note.isBookmarked)
            val updated = note.copy(isBookmarked = !note.isBookmarked)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = updated
            }
            // Sync updated bookmark to Firestore
            viewModelScope.launch(Dispatchers.IO) {
                firebaseManager.syncNoteToFirestore(updated)
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            if (_currentNote.value?.id == noteId) {
                _currentNote.value = _allNotes.value.firstOrNull { it.id != noteId }
            }
            // Delete from Firestore
            viewModelScope.launch(Dispatchers.IO) {
                firebaseManager.deleteNoteFromFirestore(noteId)
            }
            _successMessage.value = getApplication<Application>().getString(R.string.msg_note_removed)
        }
    }

    fun signInWithGoogle(activityContext: Context, serverClientId: String? = null) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val result = firebaseManager.signInWithGoogle(activityContext, serverClientId)
            result.fold(
                onSuccess = { profile ->
                    _successMessage.value = app.getString(R.string.msg_signed_in_as, profile.displayName ?: profile.email)
                    syncWithFirestore()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: app.getString(R.string.msg_google_signin_failed)
                }
            )
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        val app = getApplication<Application>()
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = app.getString(R.string.msg_email_pass_empty)
            return
        }
        viewModelScope.launch {
            val result = firebaseManager.signInWithEmail(email, pass)
            result.fold(
                onSuccess = { profile ->
                    _successMessage.value = app.getString(R.string.msg_signed_in_as, profile.email)
                    syncWithFirestore()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: app.getString(R.string.msg_auth_failed)
                }
            )
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val result = firebaseManager.signInAnonymously()
            result.fold(
                onSuccess = {
                    _successMessage.value = app.getString(R.string.msg_signed_in_guest)
                    syncWithFirestore()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: app.getString(R.string.msg_guest_signin_failed)
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseManager.signOut()
            _successMessage.value = getApplication<Application>().getString(R.string.msg_signed_out)
        }
    }

    fun syncWithFirestore() {
        viewModelScope.launch(Dispatchers.IO) {
            // Push local notes to Firestore
            for (note in _allNotes.value) {
                firebaseManager.syncNoteToFirestore(note)
            }
            withContext(Dispatchers.Main) {
                _successMessage.value = getApplication<Application>().getString(R.string.msg_synced_firestore)
            }
        }
    }

    fun exportAndSharePdf(note: MedicalLectureNote) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            try {
                val pdfFile = pdfGenerator.generatePdf(note)
                withContext(Dispatchers.Main) {
                    pdfGenerator.sharePdf(pdfFile)
                    _successMessage.value = app.getString(R.string.msg_pdf_exported, pdfFile.name)
                }
            } catch (e: Exception) {
                Log.e("SoundScriptViewModel", "Failed to export PDF", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = app.getString(R.string.msg_pdf_export_failed, e.message ?: "")
                }
            }
        }
    }

    fun saveCustomApiKey(key: String) {
        val prefs = getApplication<Application>().getSharedPreferences("soundscript_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        _successMessage.value = getApplication<Application>().getString(R.string.msg_api_key_updated)
    }

    fun getCustomApiKey(): String {
        val prefs = getApplication<Application>().getSharedPreferences("soundscript_settings", Context.MODE_PRIVATE)
        return prefs.getString("custom_gemini_api_key", "") ?: ""
    }

    /**
     * Converts current note into flashcards for active recall study
     */
    fun getFlashcardsForCurrentNote(): List<UiFlashcard> {
        val note = _currentNote.value ?: return emptyList()
        val cards = mutableListOf<UiFlashcard>()

        // 1. From Question-Answer pairs
        for (qa in note.questionAnswers) {
            cards.add(
                UiFlashcard(
                    title = "Lecturer Q&A",
                    front = qa.question,
                    back = "${qa.answer}${if (qa.lecturerNote.isNotEmpty()) "\n\n💡 Prof Note: ${qa.lecturerNote}" else ""}",
                    tag = "Q&A"
                )
            )
        }

        // 2. From Numbers & Doses
        for (nd in note.numbersAndDoses) {
            cards.add(
                UiFlashcard(
                    title = "Numbers & Dosages",
                    front = "What is the exact value/dose for:\n${nd.item}?",
                    back = "${nd.exactValue}\n\nClinical Context: ${nd.context}${if (nd.isUnclear) "\n⚠️ Note: Lecturer indicated this is [unclear]!" else ""}",
                    tag = "Dose/Cutoff"
                )
            )
        }

        // 3. From Exam Emphasis / Buzzwords
        for (ee in note.examEmphasis) {
            cards.add(
                UiFlashcard(
                    title = "Board Exam Buzzword",
                    front = "What clinical condition or mechanism does this buzzword signify:\n'${ee.buzzword}'?",
                    back = "Topic: ${ee.topic}\n\nHigh-Yield Pearl:\n${ee.pearl}\n\n⚠️ Exam Trap:\n${ee.trapOrWarning}",
                    tag = "Board Buzzword"
                )
            )
        }

        // 4. From Core Concepts
        for (cc in note.coreConcepts) {
            val parts = cc.split(":", limit = 2)
            if (parts.size == 2) {
                cards.add(
                    UiFlashcard(
                        title = "Pathophysiology & Mechanism",
                        front = "Explain the mechanism of:\n${parts[0].trim()}",
                        back = parts[1].trim(),
                        tag = "Concept"
                    )
                )
            }
        }

        return cards
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording()
        playerManager.stop()
    }
}
