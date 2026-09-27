package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ClassificationItem
import com.example.data.model.ClinicalPoint
import com.example.data.model.ExamEmphasisItem
import com.example.data.model.ManagementItem
import com.example.data.model.NumberDoseItem
import com.example.data.model.QuestionAnswerItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        return moshi.adapter<List<String>>(type).toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        return try {
            moshi.adapter<List<String>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromClassificationList(value: List<ClassificationItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ClassificationItem::class.java)
        return moshi.adapter<List<ClassificationItem>>(type).toJson(value)
    }

    @TypeConverter
    fun toClassificationList(value: String?): List<ClassificationItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ClassificationItem::class.java)
        return try {
            moshi.adapter<List<ClassificationItem>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromClinicalPointList(value: List<ClinicalPoint>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ClinicalPoint::class.java)
        return moshi.adapter<List<ClinicalPoint>>(type).toJson(value)
    }

    @TypeConverter
    fun toClinicalPointList(value: String?): List<ClinicalPoint> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ClinicalPoint::class.java)
        return try {
            moshi.adapter<List<ClinicalPoint>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromManagementList(value: List<ManagementItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ManagementItem::class.java)
        return moshi.adapter<List<ManagementItem>>(type).toJson(value)
    }

    @TypeConverter
    fun toManagementList(value: String?): List<ManagementItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ManagementItem::class.java)
        return try {
            moshi.adapter<List<ManagementItem>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromNumberDoseList(value: List<NumberDoseItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, NumberDoseItem::class.java)
        return moshi.adapter<List<NumberDoseItem>>(type).toJson(value)
    }

    @TypeConverter
    fun toNumberDoseList(value: String?): List<NumberDoseItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, NumberDoseItem::class.java)
        return try {
            moshi.adapter<List<NumberDoseItem>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromExamEmphasisList(value: List<ExamEmphasisItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ExamEmphasisItem::class.java)
        return moshi.adapter<List<ExamEmphasisItem>>(type).toJson(value)
    }

    @TypeConverter
    fun toExamEmphasisList(value: String?): List<ExamEmphasisItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ExamEmphasisItem::class.java)
        return try {
            moshi.adapter<List<ExamEmphasisItem>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromQuestionAnswerList(value: List<QuestionAnswerItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, QuestionAnswerItem::class.java)
        return moshi.adapter<List<QuestionAnswerItem>>(type).toJson(value)
    }

    @TypeConverter
    fun toQuestionAnswerList(value: String?): List<QuestionAnswerItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, QuestionAnswerItem::class.java)
        return try {
            moshi.adapter<List<QuestionAnswerItem>>(type).fromJson(value) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
