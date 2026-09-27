package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassificationItem
import com.example.data.model.ClinicalPoint
import com.example.data.model.ExamEmphasisItem
import com.example.data.model.ManagementItem
import com.example.data.model.MedicalLectureNote
import com.example.data.model.NumberDoseItem
import com.example.data.model.QuestionAnswerItem
import com.example.ui.SoundScriptViewModel
import com.example.ui.StudyMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudyNotesScreen(
    viewModel: SoundScriptViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentNote by viewModel.currentNote.collectAsState()
    val studyMode by viewModel.studyMode.collectAsState()
    val flashcards = remember(currentNote) { viewModel.getFlashcardsForCurrentNote() }
    val currentCardIndex by viewModel.currentFlashcardIndex.collectAsState()
    val isCardFlipped by viewModel.isCardFlipped.collectAsState()

    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val playPosition by viewModel.playerManager.currentPosition.collectAsState()
    val playDuration by viewModel.playerManager.duration.collectAsState()

    if (currentNote == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No Medical Lecture Selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Record a lecture or choose one from your library to view structured study notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { viewModel.selectTab(0) }) {
                    Text("Go to Record & Input")
                }
            }
        }
        return
    }

    val note = currentNote!!

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Lecture Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = note.specialty.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleBookmark(note) },
                                modifier = Modifier.testTag("bookmark_button")
                            ) {
                                Icon(
                                    imageVector = if (note.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (note.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("SoundScript Notes", "${note.title}\n\nSummary:\n${note.summary}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Notes copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(
                                onClick = { viewModel.exportAndSharePdf(note) },
                                modifier = Modifier.testTag("export_pdf_button")
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "Export PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(note.createdAt))
                    val durationMin = if (note.audioDurationSeconds > 0) "${note.audioDurationSeconds / 60}m" else "Direct Note"
                    Text(
                        text = "$dateStr • Duration: $durationMin • English Revision Notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Audio Player controls if local audio file is linked
                    if (!note.audioFilePath.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (isPlaying) {
                                                    viewModel.playerManager.togglePlayPause()
                                                } else {
                                                    viewModel.playerManager.playFile(note.audioFilePath)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause Audio",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (isPlaying) "Playing Lecture Audio" else "Listen to Original Audio",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    val currSec = playPosition / 1000
                                    val totalSec = playDuration / 1000
                                    Text(
                                        text = "${currSec / 60}:${String.format("%02d", currSec % 60)} / ${totalSec / 60}:${String.format("%02d", totalSec % 60)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (playDuration > 0) {
                                    Slider(
                                        value = playPosition.toFloat(),
                                        onValueChange = { viewModel.playerManager.seekTo(it.toInt()) },
                                        valueRange = 0f..playDuration.toFloat(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // PDF Export Primary Button
                    Button(
                        onClick = { viewModel.exportAndSharePdf(note) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export / Share PDF Study Guide", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mode Switcher Tabs
        item {
            TabRow(
                selectedTabIndex = studyMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = studyMode == StudyMode.NOTES,
                    onClick = { viewModel.setStudyMode(StudyMode.NOTES) },
                    text = { Text("Revision Notes") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                )
                Tab(
                    selected = studyMode == StudyMode.FLASHCARDS,
                    onClick = { viewModel.setStudyMode(StudyMode.FLASHCARDS) },
                    text = { Text("Active Recall (${flashcards.size})") },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) }
                )
                Tab(
                    selected = studyMode == StudyMode.QUIZ,
                    onClick = { viewModel.setStudyMode(StudyMode.QUIZ) },
                    text = { Text("High-Yield Checklist") },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null) }
                )
            }
        }

        // --- FLASHCARDS MODE ---
        if (studyMode == StudyMode.FLASHCARDS) {
            item {
                if (flashcards.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No flashcards available for this note.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val card = flashcards[currentCardIndex % flashcards.size]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Card ${currentCardIndex + 1} of ${flashcards.size} • Tap card to flip",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 3D Flip Card Effect
                        val rotation by animateFloatAsState(
                            targetValue = if (isCardFlipped) 180f else 0f,
                            label = "flipAnimation"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 12f * density
                                }
                                .clickable { viewModel.flipCard() }
                                .testTag("flashcard_item"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCardFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                                    .graphicsLayer {
                                        if (isCardFlipped) rotationY = 180f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = if (isCardFlipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = card.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCardFlipped) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Flip,
                                            contentDescription = "Flip",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = if (isCardFlipped) card.back else card.front,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCardFlipped) FontWeight.Medium else FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = if (isCardFlipped) "✓ ANSWER / EXPLANATION" else "❓ QUESTION / PROMPT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Navigation arrows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.prevCard(flashcards.size) }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Previous")
                            }

                            Button(
                                onClick = { viewModel.flipCard() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(if (isCardFlipped) "Show Front" else "Reveal Answer")
                            }

                            OutlinedButton(
                                onClick = { viewModel.nextCard(flashcards.size) }
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }

        // --- PRE-EXAM RAPID REVIEW CHECKLIST ---
        if (studyMode == StudyMode.QUIZ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚡ Highest-Yield Pre-Exam Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Review these high-yield bullets 10 minutes before your rounds or exam:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        note.highestYieldPoints.forEachIndexed { idx, point ->
                            var isChecked by remember { mutableStateOf(false) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isChecked = !isChecked },
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- STRUCTURED REVISION NOTES MODE ---
        if (studyMode == StudyMode.NOTES) {
            // 1. Clinical Summary
            if (note.summary.isNotEmpty()) {
                item {
                    SectionCard(title = "Clinical Summary", icon = "📋") {
                        Text(
                            text = note.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 2. Core Concepts
            if (note.coreConcepts.isNotEmpty()) {
                item {
                    SectionCard(title = "Core Concepts & Pathophysiology", icon = "🧬") {
                        note.coreConcepts.forEach { concept ->
                            BulletItem(text = concept)
                        }
                    }
                }
            }

            // 3. Key Takeaways
            if (note.importantPoints.isNotEmpty()) {
                item {
                    SectionCard(title = "Key Lecture Takeaways", icon = "⭐") {
                        note.importantPoints.forEach { point ->
                            BulletItem(text = point)
                        }
                    }
                }
            }

            // 4. Classifications & Diagnostic Criteria
            if (note.classifications.isNotEmpty()) {
                item {
                    SectionCard(title = "Classifications & Criteria", icon = "📊") {
                        note.classifications.forEach { item ->
                            ClassificationCard(item)
                        }
                    }
                }
            }

            // 5. Clinical & Diagnostic Points
            if (note.clinicalPoints.isNotEmpty()) {
                item {
                    SectionCard(title = "Clinical Presentation & Diagnostics", icon = "🩺") {
                        note.clinicalPoints.forEach { item ->
                            ClinicalPointCard(item)
                        }
                    }
                }
            }

            // 6. Management & Therapeutic Protocol
            if (note.management.isNotEmpty()) {
                item {
                    SectionCard(title = "Management & Therapeutics", icon = "💊") {
                        note.management.forEach { item ->
                            ManagementCard(item)
                        }
                    }
                }
            }

            // 7. Strict Numbers & Exact Doses (with [unclear] tags)
            if (note.numbersAndDoses.isNotEmpty()) {
                item {
                    SectionCard(title = "Numbers, Cutoffs & Exact Dosages", icon = "🔢") {
                        Text(
                            text = "Lecturer-verified values and dosages. Ambiguous items flagged as [unclear].",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        note.numbersAndDoses.forEach { item ->
                            NumberDoseCard(item)
                        }
                    }
                }
            }

            // 8. Board Pearls & Buzzwords
            if (note.examEmphasis.isNotEmpty()) {
                item {
                    SectionCard(title = "Board Exam Pearls & Buzzwords", icon = "🎯") {
                        note.examEmphasis.forEach { item ->
                            ExamEmphasisCard(item)
                        }
                    }
                }
            }

            // 9. Preserved Question & Answer Items
            if (note.questionAnswers.isNotEmpty()) {
                item {
                    SectionCard(title = "Preserved Question & Answer Items", icon = "❓") {
                        note.questionAnswers.forEach { item ->
                            QuestionAnswerCard(item)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun BulletItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 21.sp
        )
    }
}

@Composable
fun ClassificationCard(item: ClassificationItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${item.title} (${item.category})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            item.criteria.forEach { crit ->
                Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                    Text("• ", color = MaterialTheme.colorScheme.primary)
                    Text(crit, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ClinicalPointCard(item: ClinicalPoint) {
    Surface(
        color = if (item.isHighYield) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Surface(
                color = if (item.isHighYield) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = item.category.take(12),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ManagementCard(item: ManagementItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when {
                        item.line.contains("First", true) -> MaterialTheme.colorScheme.primary
                        item.line.contains("Contraindication", true) -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.line,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.intervention,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (item.rationale.isNotEmpty()) {
                Text(
                    text = "Rationale: ${item.rationale}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NumberDoseCard(item: NumberDoseItem) {
    Surface(
        color = if (item.isUnclear) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        border = if (item.isUnclear) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.item,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    color = if (item.isUnclear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.exactValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.context.isNotEmpty()) {
                Text(
                    text = item.context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.isUnclear) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Unclear",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[unclear] Lecturer was ambiguous; verify institution protocol",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ExamEmphasisCard(item: ExamEmphasisItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Topic: ${item.topic}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (item.buzzword.isNotEmpty()) {
                Text(
                    text = "🎯 Buzzword: ${item.buzzword}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (item.pearl.isNotEmpty()) {
                Text(
                    text = "💡 Board Pearl: ${item.pearl}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (item.trapOrWarning.isNotEmpty()) {
                Text(
                    text = "⚠️ Exam Trap: ${item.trapOrWarning}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun QuestionAnswerCard(item: QuestionAnswerItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Q: ${item.question}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "A: ${item.answer}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (item.lecturerNote.isNotEmpty()) {
                Text(
                    text = "Lecturer Note: ${item.lecturerNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
