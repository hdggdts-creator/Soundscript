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
    fun testPdfGeneratorInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = MedicalNotesPdfGenerator(context)
        assertNotNull(generator)
    }

    @Test
    fun testGeminiModelConfiguration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = com.example.ai.GeminiMedicalService(context)
        assertEquals("gemini-3.5-flash", service.defaultModel)
    }

    @Test
    fun testNetworkClientTimeoutsAreMinimum120Seconds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = com.example.ai.GeminiMedicalService(context)
        val client = service.okHttpClient
        assertTrue(client.connectTimeoutMillis >= 120_000)
        assertTrue(client.readTimeoutMillis >= 120_000)
        assertTrue(client.writeTimeoutMillis >= 120_000)
        assertTrue(client.callTimeoutMillis >= 120_000)
    }

    @Test
    fun testGeminiRequestConfiguresResponseMimeType() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = com.example.ai.GeminiMedicalService(context)
        val request = service.buildGeminiRequest(null, null, "Test prompt")
        val config = request.getJSONObject("generationConfig")
        assertEquals("application/json", config.getString("responseMimeType"))
    }

    @Test
    fun testStripMarkdownFences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = com.example.ai.GeminiMedicalService(context)

        // Case 1: ```json ... ```
        val fencedJson = "```json\n{\"title\": \"Cardiology\"}\n```"
        assertEquals("{\"title\": \"Cardiology\"}", service.stripMarkdownFences(fencedJson))

        // Case 2: ``` ... ``` without json tag
        val plainFenced = "```\n{\"title\": \"Pulmonology\"}\n```"
        assertEquals("{\"title\": \"Pulmonology\"}", service.stripMarkdownFences(plainFenced))

        // Case 3: Embedded fences with surrounding explanation
        val embedded = "Here is your JSON output:\n```json\n{\"title\": \"Neurology\"}\n```\nHope this helps!"
        assertEquals("{\"title\": \"Neurology\"}", service.stripMarkdownFences(embedded))

        // Case 4: Raw JSON without fences
        val raw = "{\"title\": \"Pediatrics\"}"
        assertEquals("{\"title\": \"Pediatrics\"}", service.stripMarkdownFences(raw))
    }

    @Test
    fun testParseMedicalNoteWithMarkdownFences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = com.example.ai.GeminiMedicalService(context)
        val mockResponse = """
            ```json
            {
              "title": "Heart Failure Overview",
              "specialty": "Cardiology",
              "summary": "Clinical overview of HFrEF and HFpEF.",
              "coreConcepts": ["Ventricular remodeling"],
              "importantPoints": ["LVEF <= 40% defines HFrEF"],
              "highestYieldPoints": ["Spironolactone reduces mortality in NYHA II-IV"]
            }
            ```
        """.trimIndent()

        val note = service.parseMedicalNoteJson(mockResponse, 300)
        assertEquals("Heart Failure Overview", note.title)
        assertEquals("Cardiology", note.specialty)
        assertEquals(1, note.coreConcepts.size)
        assertEquals("Ventricular remodeling", note.coreConcepts[0])
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
