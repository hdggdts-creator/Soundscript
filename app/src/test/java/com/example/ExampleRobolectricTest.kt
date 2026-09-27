package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LectureNoteEntity
import com.example.data.sample.SampleLectures
import com.example.pdf.MedicalNotesPdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SoundScript", appName)
    }

    @Test
    fun testSampleLecturesIntegrity() {
        val hfLecture = SampleLectures.heartFailureLecture
        assertEquals("Cardiology", hfLecture.specialty)
        assertTrue(hfLecture.numbersAndDoses.isNotEmpty())
        assertTrue(hfLecture.questionAnswers.isNotEmpty())
        assertTrue(hfLecture.examEmphasis.isNotEmpty())

        // Ensure [unclear] is tracked
        val unclearItem = hfLecture.numbersAndDoses.firstOrNull { it.isUnclear }
        assertNotNull("Should have at least one [unclear] marked item", unclearItem)
    }

    @Test
    fun testLectureEntityDomainConversion() {
        val sample = SampleLectures.heartFailureLecture
        val entity = LectureNoteEntity.fromDomain(sample)
        val domain = entity.toDomain()

        assertEquals(sample.title, domain.title)
        assertEquals(sample.specialty, domain.specialty)
        assertEquals(sample.numbersAndDoses.size, domain.numbersAndDoses.size)
        assertEquals(sample.questionAnswers.size, domain.questionAnswers.size)
    }

    @Test
    fun testPdfGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = MedicalNotesPdfGenerator(context)
        val pdfFile = generator.generatePdf(SampleLectures.heartFailureLecture)

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun testUserProfileModel() {
        val profile = com.example.firebase.UserProfile(
            uid = "user_123",
            email = "student@med.edu",
            displayName = "Dr. Student",
            photoUrl = null,
            isAnonymous = false
        )
        assertEquals("user_123", profile.uid)
        assertEquals("student@med.edu", profile.email)
        assertEquals("Dr. Student", profile.displayName)
    }
}
