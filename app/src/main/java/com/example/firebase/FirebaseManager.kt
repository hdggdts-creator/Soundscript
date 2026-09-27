package com.example.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.ClassificationItem
import com.example.data.model.ClinicalPoint
import com.example.data.model.ExamEmphasisItem
import com.example.data.model.ManagementItem
import com.example.data.model.MedicalLectureNote
import com.example.data.model.NumberDoseItem
import com.example.data.model.QuestionAnswerItem
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean
)

class FirebaseManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    private val _isFirebaseConfigured = MutableStateFlow(false)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        checkAndInitFirebase()
    }

    fun checkAndInitFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                // Try initializing with fallback options if google-services.json was not yet dropped in
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.soundscript.medapp")
                    .setApiKey("AIzaSySoundScriptAppPlaceholderKey12345")
                    .setProjectId("soundscript-med")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            _isFirebaseConfigured.value = true
            setupAuthStateListener()
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Firebase initialization notice: ${e.message}")
            _isFirebaseConfigured.value = false
        }
    }

    private fun setupAuthStateListener() {
        try {
            val auth = FirebaseAuth.getInstance()
            updateUser(auth.currentUser)

            authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                updateUser(firebaseAuth.currentUser)
            }
            auth.addAuthStateListener(authListener!!)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error setting auth state listener", e)
        }
    }

    private fun updateUser(user: FirebaseUser?) {
        if (user != null) {
            _currentUser.value = UserProfile(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName ?: user.email?.substringBefore('@') ?: "Medical Student",
                photoUrl = user.photoUrl?.toString(),
                isAnonymous = user.isAnonymous
            )
        } else {
            _currentUser.value = null
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    /**
     * Google Sign-in with Android Credential Manager
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        serverClientId: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authLoading.value = true
        _authError.value = null

        try {
            val clientId = serverClientId ?: "108347895281-dummywebclientid.apps.googleusercontent.com"
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val user = authResult.user

                if (user != null) {
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email,
                        displayName = user.displayName ?: googleIdTokenCredential.displayName ?: "Medical Student",
                        photoUrl = user.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                        isAnonymous = false
                    )
                    _currentUser.value = profile
                    _authLoading.value = false
                    return@withContext Result.success(profile)
                }
            }
            _authLoading.value = false
            val err = "Google ID Token could not be parsed."
            _authError.value = err
            Result.failure(Exception(err))
        } catch (e: GetCredentialException) {
            _authLoading.value = false
            Log.e("FirebaseManager", "Google sign-in credential exception", e)
            val message = e.message ?: "Google sign-in was cancelled or failed."
            _authError.value = message
            Result.failure(e)
        } catch (e: Exception) {
            _authLoading.value = false
            Log.e("FirebaseManager", "Google sign-in failed", e)
            val message = e.message ?: "Authentication failed."
            _authError.value = message
            Result.failure(e)
        }
    }

    /**
     * Sign in or Register with Email & Password
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authLoading.value = true
        _authError.value = null
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = try {
                auth.signInWithEmailAndPassword(email.trim(), pass).await()
            } catch (signInEx: Exception) {
                // If user not found, attempt creation
                auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            }

            val user = authResult.user
            if (user != null) {
                val profile = UserProfile(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName ?: email.substringBefore('@'),
                    photoUrl = null,
                    isAnonymous = false
                )
                _currentUser.value = profile
                _authLoading.value = false
                Result.success(profile)
            } else {
                _authLoading.value = false
                Result.failure(Exception("Failed to obtain signed-in user."))
            }
        } catch (e: Exception) {
            _authLoading.value = false
            val msg = e.message ?: "Email authentication failed."
            _authError.value = msg
            Result.failure(e)
        }
    }

    /**
     * Anonymous / Guest sign-in for quick offline/guest syncing
     */
    suspend fun signInAnonymously(): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authLoading.value = true
        _authError.value = null
        try {
            val auth = FirebaseAuth.getInstance()
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                val profile = UserProfile(
                    uid = user.uid,
                    email = null,
                    displayName = "Guest Clinician",
                    photoUrl = null,
                    isAnonymous = true
                )
                _currentUser.value = profile
                _authLoading.value = false
                Result.success(profile)
            } else {
                _authLoading.value = false
                Result.failure(Exception("Anonymous login failed."))
            }
        } catch (e: Exception) {
            _authLoading.value = false
            val msg = e.message ?: "Anonymous sign-in failed."
            _authError.value = msg
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            FirebaseAuth.getInstance().signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error signing out", e)
        }
    }

    // =========================================================================
    // CLOUD FIRESTORE DATA PERSISTENCE
    // =========================================================================

    /**
     * Sync/save a medical lecture note to Firestore under the user's collection:
     * users/{uid}/lecture_notes/{noteId}
     */
    suspend fun syncNoteToFirestore(note: MedicalLectureNote): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        val uid = user?.uid ?: "local_guest"

        try {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("lecture_notes")
                .document(note.id.toString())

            val map = noteToFirestoreMap(note)
            docRef.set(map, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to sync note ${note.id} to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a note from Firestore
     */
    suspend fun deleteNoteFromFirestore(noteId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.success(Unit)
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(user.uid)
                .collection("lecture_notes")
                .document(noteId.toString())
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to delete note from Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Listen to real-time changes in Firestore for the current user's lecture notes
     */
    fun observeUserNotesFromFirestore(): Flow<List<MedicalLectureNote>> = callbackFlow {
        val user = _currentUser.value
        if (user == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore.collection("users")
            .document(user.uid)
            .collection("lecture_notes")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseManager", "Firestore listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notes = snapshot.documents.mapNotNull { doc ->
                        docToMedicalNote(doc.data, doc.id)
                    }
                    trySend(notes)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    // --- Helper mappers for Firestore ---

    private fun noteToFirestoreMap(note: MedicalLectureNote): Map<String, Any?> {
        return mapOf(
            "id" to note.id,
            "title" to note.title,
            "specialty" to note.specialty,
            "createdAt" to note.createdAt,
            "audioDurationSeconds" to note.audioDurationSeconds,
            "audioFilePath" to note.audioFilePath,
            "summary" to note.summary,
            "coreConcepts" to note.coreConcepts,
            "importantPoints" to note.importantPoints,
            "highestYieldPoints" to note.highestYieldPoints,
            "isBookmarked" to note.isBookmarked,
            "classifications" to note.classifications.map {
                mapOf("title" to it.title, "category" to it.category, "criteria" to it.criteria)
            },
            "clinicalPoints" to note.clinicalPoints.map {
                mapOf("category" to it.category, "description" to it.description, "isHighYield" to it.isHighYield)
            },
            "management" to note.management.map {
                mapOf("line" to it.line, "intervention" to it.intervention, "rationale" to it.rationale)
            },
            "numbersAndDoses" to note.numbersAndDoses.map {
                mapOf("item" to it.item, "exactValue" to it.exactValue, "context" to it.context, "isUnclear" to it.isUnclear)
            },
            "examEmphasis" to note.examEmphasis.map {
                mapOf("topic" to it.topic, "buzzword" to it.buzzword, "pearl" to it.pearl, "trapOrWarning" to it.trapOrWarning)
            },
            "questionAnswers" to note.questionAnswers.map {
                mapOf("question" to it.question, "answer" to it.answer, "lecturerNote" to it.lecturerNote)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun docToMedicalNote(data: Map<String, Any>?, docId: String): MedicalLectureNote? {
        if (data == null) return null
        return try {
            val id = (data["id"] as? Number)?.toLong() ?: docId.toLongOrNull() ?: 0L
            val title = data["title"] as? String ?: ""
            val specialty = data["specialty"] as? String ?: "General Medicine"
            val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val audioDurationSeconds = (data["audioDurationSeconds"] as? Number)?.toInt() ?: 0
            val audioFilePath = data["audioFilePath"] as? String
            val summary = data["summary"] as? String ?: ""
            val coreConcepts = (data["coreConcepts"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val importantPoints = (data["importantPoints"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val highestYieldPoints = (data["highestYieldPoints"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val isBookmarked = data["isBookmarked"] as? Boolean ?: false

            val classifications = (data["classifications"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<String, Any>)?.let {
                    ClassificationItem(
                        title = it["title"] as? String ?: "",
                        category = it["category"] as? String ?: "",
                        criteria = (it["criteria"] as? List<*>)?.mapNotNull { c -> c?.toString() } ?: emptyList()
                    )
                }
            } ?: emptyList()

            val clinicalPoints = (data["clinicalPoints"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<String, Any>)?.let {
                    ClinicalPoint(
                        category = it["category"] as? String ?: "",
                        description = it["description"] as? String ?: "",
                        isHighYield = it["isHighYield"] as? Boolean ?: false
                    )
                }
            } ?: emptyList()

            val management = (data["management"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<String, Any>)?.let {
                    ManagementItem(
                        line = it["line"] as? String ?: "",
                        intervention = it["intervention"] as? String ?: "",
                        rationale = it["rationale"] as? String ?: ""
                    )
                }
            } ?: emptyList()

            val numbersAndDoses = (data["numbersAndDoses"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<String, Any>)?.let {
                    NumberDoseItem(
                        item = it["item"] as? String ?: "",
                        exactValue = it["exactValue"] as? String ?: "",
                        context = it["context"] as? String ?: "",
                        isUnclear = it["isUnclear"] as? Boolean ?: false
                    )
                }
            } ?: emptyList()

            val examEmphasis = (data["examEmphasis"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<String, Any>)?.let {
                    ExamEmphasisItem(
                        topic = it["topic"] as? String ?: "",
                        buzzword = it["buzzword"] as? String ?: "",
                        pearl = it["pearl"] as? String ?: "",
                        trapOrWarning = it["trapOrWarning"] as? String ?: ""
                    )
                }
            } ?: emptyList()

            val questionAnswers = (data["questionAnswers"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<String, Any>)?.let {
                    QuestionAnswerItem(
                        question = it["question"] as? String ?: "",
                        answer = it["answer"] as? String ?: "",
                        lecturerNote = it["lecturerNote"] as? String ?: ""
                    )
                }
            } ?: emptyList()

            MedicalLectureNote(
                id = id,
                title = title,
                specialty = specialty,
                createdAt = createdAt,
                audioDurationSeconds = audioDurationSeconds,
                audioFilePath = audioFilePath,
                summary = summary,
                coreConcepts = coreConcepts,
                importantPoints = importantPoints,
                classifications = classifications,
                clinicalPoints = clinicalPoints,
                management = management,
                numbersAndDoses = numbersAndDoses,
                examEmphasis = examEmphasis,
                questionAnswers = questionAnswers,
                highestYieldPoints = highestYieldPoints,
                isBookmarked = isBookmarked
            )
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error parsing Firestore document", e)
            null
        }
    }
}
