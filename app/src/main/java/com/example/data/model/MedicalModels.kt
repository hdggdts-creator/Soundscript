package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ClassificationItem(
    val title: String = "",
    val category: String = "",
    val criteria: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ClinicalPoint(
    val category: String = "", // "Presentation", "Physical Exam", "Diagnostics", "Gold Standard"
    val description: String = "",
    val isHighYield: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ManagementItem(
    val line: String = "First-Line", // First-Line, Second-Line, Emergency, Contraindication, Lifestyle
    val intervention: String = "",
    val rationale: String = ""
)

@JsonClass(generateAdapter = true)
data class NumberDoseItem(
    val item: String = "",
    val exactValue: String = "",
    val context: String = "",
    val isUnclear: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ExamEmphasisItem(
    val topic: String = "",
    val buzzword: String = "",
    val pearl: String = "",
    val trapOrWarning: String = ""
)

@JsonClass(generateAdapter = true)
data class QuestionAnswerItem(
    val question: String = "",
    val answer: String = "",
    val lecturerNote: String = ""
)

@JsonClass(generateAdapter = true)
data class MedicalLectureNote(
    val id: Long = 0,
    val title: String = "",
    val specialty: String = "General Medicine",
    val createdAt: Long = System.currentTimeMillis(),
    val audioDurationSeconds: Int = 0,
    val audioFilePath: String? = null,
    val summary: String = "",
    val coreConcepts: List<String> = emptyList(),
    val importantPoints: List<String> = emptyList(),
    val classifications: List<ClassificationItem> = emptyList(),
    val clinicalPoints: List<ClinicalPoint> = emptyList(),
    val management: List<ManagementItem> = emptyList(),
    val numbersAndDoses: List<NumberDoseItem> = emptyList(),
    val examEmphasis: List<ExamEmphasisItem> = emptyList(),
    val questionAnswers: List<QuestionAnswerItem> = emptyList(),
    val highestYieldPoints: List<String> = emptyList(),
    val isBookmarked: Boolean = false
)
