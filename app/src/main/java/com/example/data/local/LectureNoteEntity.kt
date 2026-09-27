package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ClassificationItem
import com.example.data.model.ClinicalPoint
import com.example.data.model.ExamEmphasisItem
import com.example.data.model.ManagementItem
import com.example.data.model.MedicalLectureNote
import com.example.data.model.NumberDoseItem
import com.example.data.model.QuestionAnswerItem

@Entity(tableName = "lecture_notes")
data class LectureNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val specialty: String,
    val createdAt: Long,
    val audioDurationSeconds: Int,
    val audioFilePath: String?,
    val summary: String,
    val coreConcepts: List<String>,
    val importantPoints: List<String>,
    val classifications: List<ClassificationItem>,
    val clinicalPoints: List<ClinicalPoint>,
    val management: List<ManagementItem>,
    val numbersAndDoses: List<NumberDoseItem>,
    val examEmphasis: List<ExamEmphasisItem>,
    val questionAnswers: List<QuestionAnswerItem>,
    val highestYieldPoints: List<String>,
    val isBookmarked: Boolean
) {
    fun toDomain(): MedicalLectureNote = MedicalLectureNote(
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

    companion object {
        fun fromDomain(domain: MedicalLectureNote): LectureNoteEntity = LectureNoteEntity(
            id = domain.id,
            title = domain.title,
            specialty = domain.specialty,
            createdAt = domain.createdAt,
            audioDurationSeconds = domain.audioDurationSeconds,
            audioFilePath = domain.audioFilePath,
            summary = domain.summary,
            coreConcepts = domain.coreConcepts,
            importantPoints = domain.importantPoints,
            classifications = domain.classifications,
            clinicalPoints = domain.clinicalPoints,
            management = domain.management,
            numbersAndDoses = domain.numbersAndDoses,
            examEmphasis = domain.examEmphasis,
            questionAnswers = domain.questionAnswers,
            highestYieldPoints = domain.highestYieldPoints,
            isBookmarked = domain.isBookmarked
        )
    }
}
