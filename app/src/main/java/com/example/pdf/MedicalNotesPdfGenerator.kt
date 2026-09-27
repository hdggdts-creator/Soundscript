package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.MedicalLectureNote
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicalNotesPdfGenerator(private val context: Context) {

    private val pageWidth = 595 // A4 standard width in points
    private val pageHeight = 842 // A4 standard height in points
    private val margin = 40f
    private val contentWidth = pageWidth - (margin * 2)

    fun generatePdf(note: MedicalLectureNote): File {
        val pdfDocument = PdfDocument()
        val pages = mutableListOf<PdfDocument.Page>()

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        pages.add(currentPage)

        var currentY = margin

        // Color definitions
        val colorPrimary = Color.parseColor("#0F766E") // Dark Teal
        val colorSecondary = Color.parseColor("#0F172A") // Slate Navy
        val colorAccent = Color.parseColor("#F43F5E") // Coral Rose
        val colorBgLight = Color.parseColor("#F8FAFC") // Off-white
        val colorTextDark = Color.parseColor("#1E293B") // Charcoal
        val colorTextMuted = Color.parseColor("#64748B") // Grey
        val colorBorder = Color.parseColor("#CBD5E1")

        // Paints
        val titlePaint = Paint().apply {
            color = colorSecondary
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = colorPrimary
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = colorTextDark
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = colorTextDark
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val italicBodyPaint = Paint().apply {
            color = colorTextMuted
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = colorTextMuted
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = colorBgLight
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = colorBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val accentBarPaint = Paint().apply {
            color = colorPrimary
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Helper function to check and create new page if space needed exceeds remaining
        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > pageHeight - margin - 30f) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                pages.add(currentPage)
                currentY = margin + 15f
            }
        }

        // Helper to draw wrapped text and return height used
        fun drawWrappedText(
            text: String,
            x: Float,
            width: Float,
            paint: Paint,
            lineSpacing: Float = 14f,
            prefix: String = ""
        ) {
            val words = text.split(" ")
            var line = prefix
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > width && line.isNotEmpty()) {
                    ensureSpace(lineSpacing)
                    canvas.drawText(line, x, currentY, paint)
                    currentY += lineSpacing
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(lineSpacing)
                canvas.drawText(line, x, currentY, paint)
                currentY += lineSpacing
            }
        }

        // Helper to draw section header
        fun drawSectionHeader(title: String, iconTag: String = "") {
            ensureSpace(35f)
            currentY += 8f
            canvas.drawRect(margin, currentY - 12f, margin + 4f, currentY + 4f, accentBarPaint)
            val headerText = if (iconTag.isNotEmpty()) "$iconTag  $title" else title
            canvas.drawText(headerText.uppercase(), margin + 10f, currentY, headerPaint)
            canvas.drawLine(margin + 10f, currentY + 5f, margin + contentWidth, currentY + 5f, borderPaint)
            currentY += 16f
        }

        // --- DRAW DOCUMENT HEADER (Page 1) ---
        // SoundScript Top Banner
        canvas.drawText("SOUNDSCRIPT  •  MEDICAL REVISION NOTES", margin, currentY, metaPaint)
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(note.createdAt))
        val rightMeta = "Specialty: ${note.specialty}  |  $dateStr"
        val metaWidth = metaPaint.measureText(rightMeta)
        canvas.drawText(rightMeta, margin + contentWidth - metaWidth, currentY, metaPaint)
        currentY += 18f

        // Document Title
        drawWrappedText(note.title, margin, contentWidth, titlePaint, lineSpacing = 22f)
        currentY += 4f

        // Metadata Pills row
        val durationMin = if (note.audioDurationSeconds > 0) "${note.audioDurationSeconds / 60}m duration" else "Text parsed"
        canvas.drawText("Format: English-Only Study Guide  |  $durationMin  |  Status: Verified Clinical Pearl Extract", margin, currentY, italicBodyPaint)
        currentY += 16f

        // --- EXECUTIVE CLINICAL SUMMARY ---
        if (note.summary.isNotEmpty()) {
            drawSectionHeader("Executive Summary", "📋")
            drawWrappedText(note.summary, margin + 6f, contentWidth - 12f, bodyPaint, lineSpacing = 14f)
            currentY += 8f
        }

        // --- CORE CONCEPTS & PATHOPHYSIOLOGY ---
        if (note.coreConcepts.isNotEmpty()) {
            drawSectionHeader("Core Concepts & Pathophysiology", "🧬")
            for (concept in note.coreConcepts) {
                ensureSpace(24f)
                canvas.drawCircle(margin + 6f, currentY - 3f, 2.5f, accentBarPaint)
                drawWrappedText(concept, margin + 16f, contentWidth - 20f, bodyPaint, lineSpacing = 14f)
                currentY += 4f
            }
            currentY += 6f
        }

        // --- IMPORTANT LECTURE POINTS ---
        if (note.importantPoints.isNotEmpty()) {
            drawSectionHeader("Key Lecture Takeaways", "⭐")
            for (point in note.importantPoints) {
                ensureSpace(24f)
                canvas.drawCircle(margin + 6f, currentY - 3f, 2.5f, accentBarPaint)
                drawWrappedText(point, margin + 16f, contentWidth - 20f, bodyPaint, lineSpacing = 14f)
                currentY += 4f
            }
            currentY += 6f
        }

        // --- CLASSIFICATIONS & CRITERIA ---
        if (note.classifications.isNotEmpty()) {
            drawSectionHeader("Classifications & Diagnostic Criteria", "📊")
            for (item in note.classifications) {
                ensureSpace(30f)
                drawWrappedText("${item.title} (${item.category})", margin + 4f, contentWidth - 8f, boldBodyPaint, lineSpacing = 15f)
                for (criterion in item.criteria) {
                    drawWrappedText(criterion, margin + 16f, contentWidth - 20f, bodyPaint, lineSpacing = 13f, prefix = "• ")
                }
                currentY += 4f
            }
            currentY += 6f
        }

        // --- CLINICAL & DIAGNOSTIC POINTS ---
        if (note.clinicalPoints.isNotEmpty()) {
            drawSectionHeader("Clinical Presentation & Diagnostics", "🩺")
            for (item in note.clinicalPoints) {
                ensureSpace(25f)
                val badge = if (item.isHighYield) "[HIGH-YIELD] " else ""
                val lineHeader = "${badge}${item.category}: "
                drawWrappedText(lineHeader + item.description, margin + 6f, contentWidth - 12f, bodyPaint, lineSpacing = 14f)
                currentY += 4f
            }
            currentY += 6f
        }

        // --- MANAGEMENT & TREATMENT ---
        if (note.management.isNotEmpty()) {
            drawSectionHeader("Management & Therapeutic Protocol", "💊")
            for (item in note.management) {
                ensureSpace(30f)
                drawWrappedText("${item.line}: ${item.intervention}", margin + 6f, contentWidth - 12f, boldBodyPaint, lineSpacing = 14f)
                if (item.rationale.isNotEmpty()) {
                    drawWrappedText("Rationale: ${item.rationale}", margin + 16f, contentWidth - 22f, italicBodyPaint, lineSpacing = 13f)
                }
                currentY += 4f
            }
            currentY += 6f
        }

        // --- NUMBERS & EXACT DOSES (STRICT LECTURER SPECIFIED) ---
        if (note.numbersAndDoses.isNotEmpty()) {
            drawSectionHeader("Numbers, Cutoffs & Exact Dosages", "🔢")
            for (item in note.numbersAndDoses) {
                ensureSpace(32f)
                val unclearTag = if (item.isUnclear) " [UNCLEAR - VERIFY PROTOCOL]" else ""
                drawWrappedText("• ${item.item}: ${item.exactValue}$unclearTag", margin + 6f, contentWidth - 12f, boldBodyPaint, lineSpacing = 14f)
                if (item.context.isNotEmpty()) {
                    drawWrappedText("Context: ${item.context}", margin + 18f, contentWidth - 24f, italicBodyPaint, lineSpacing = 13f)
                }
                currentY += 4f
            }
            currentY += 6f
        }

        // --- EXAM EMPHASIS & BOARD BUZZWORDS ---
        if (note.examEmphasis.isNotEmpty()) {
            drawSectionHeader("Board Exam Pearls & Buzzwords", "🎯")
            for (item in note.examEmphasis) {
                ensureSpace(35f)
                drawWrappedText("Topic: ${item.topic}", margin + 6f, contentWidth - 12f, boldBodyPaint, lineSpacing = 14f)
                if (item.buzzword.isNotEmpty()) {
                    drawWrappedText("Buzzword: ${item.buzzword}", margin + 14f, contentWidth - 20f, bodyPaint, lineSpacing = 13f)
                }
                if (item.pearl.isNotEmpty()) {
                    drawWrappedText("High-Yield Pearl: ${item.pearl}", margin + 14f, contentWidth - 20f, bodyPaint, lineSpacing = 13f)
                }
                if (item.trapOrWarning.isNotEmpty()) {
                    drawWrappedText("Exam Trap: ${item.trapOrWarning}", margin + 14f, contentWidth - 20f, italicBodyPaint, lineSpacing = 13f)
                }
                currentY += 4f
            }
            currentY += 6f
        }

        // --- QUESTION -> ANSWER PAIRS ---
        if (note.questionAnswers.isNotEmpty()) {
            drawSectionHeader("Preserved Question & Answer Interactions", "❓")
            for (qa in note.questionAnswers) {
                ensureSpace(35f)
                drawWrappedText("Q: ${qa.question}", margin + 6f, contentWidth - 12f, boldBodyPaint, lineSpacing = 14f)
                drawWrappedText("A: ${qa.answer}", margin + 16f, contentWidth - 22f, bodyPaint, lineSpacing = 13f)
                if (qa.lecturerNote.isNotEmpty()) {
                    drawWrappedText("Prof Note: ${qa.lecturerNote}", margin + 16f, contentWidth - 22f, italicBodyPaint, lineSpacing = 12f)
                }
                currentY += 4f
            }
            currentY += 6f
        }

        // --- HIGHEST-YIELD RAPID REVIEW ---
        if (note.highestYieldPoints.isNotEmpty()) {
            drawSectionHeader("Highest-Yield Rapid Review (Pre-Exam Checklist)", "⚡")
            for (hyp in note.highestYieldPoints) {
                ensureSpace(24f)
                canvas.drawCircle(margin + 6f, currentY - 3f, 2.5f, accentBarPaint)
                drawWrappedText(hyp, margin + 16f, contentWidth - 20f, boldBodyPaint, lineSpacing = 14f)
                currentY += 4f
            }
            currentY += 8f
        }

        // Finish last page
        pdfDocument.finishPage(currentPage)

        // Save PDF to cache/exports
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()

        val cleanTitle = note.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
        val pdfFile = File(exportsDir, "SoundScript_${cleanTitle}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    fun sharePdf(pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Medical study notes generated by SoundScript.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Medical Study Notes (PDF)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun viewPdf(pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            sharePdf(pdfFile)
        }
    }
}
