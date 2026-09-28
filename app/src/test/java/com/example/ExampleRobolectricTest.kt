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

    @Test
    fun testLoginActivityWebClientIdAndSignInWithGoogleOption() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val activityController = org.robolectric.Robolectric.buildActivity(LoginActivity::class.java)
        val activity = activityController.get()
        assertNotNull(activity.WEB_CLIENT_ID)
        assertTrue(activity.WEB_CLIENT_ID.isNotEmpty())

        val signInOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(activity.WEB_CLIENT_ID)
            .build()

        assertEquals(activity.WEB_CLIENT_ID, signInOption.serverClientId)

        val request = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        assertEquals(1, request.credentialOptions.size)
    }

    @Test
    fun testFirebaseManagerWebClientIdPlaceholder() {
        assertNotNull(com.example.firebase.FirebaseManager.WEB_CLIENT_ID)
        assertEquals(
            "386460666392-mdl872rgt4lel8167vpo48t7kk6ohdot.apps.googleusercontent.com",
            com.example.firebase.FirebaseManager.WEB_CLIENT_ID
        )
    }

    @Test
    fun testAccessManagerUrls() {
        assertEquals(
            "https://gist.githubusercontent.com/hdggdts-creator/600c9a3eb7051025dd8510f079c1a1ca/raw/subscribers.json",
            AccessManager.SUBSCRIBERS_URL
        )
        assertEquals(
            "https://t.me/MMF5C3",
            AccessManager.TELEGRAM_CONTACT_URL
        )
    }

    @Test
    fun testAccessManagerParseSubscribersJson() {
        val jsonArray = """["doctor@hospital.org", "student@med.edu", "hdggdts@gmail.com"]"""
        val subscribers = AccessManager.parseSubscribersJson(jsonArray)
        assertEquals(3, subscribers.size)
        assertTrue(subscribers.contains("doctor@hospital.org"))
        assertTrue(subscribers.contains("student@med.edu"))
        assertTrue(subscribers.contains("hdggdts@gmail.com"))

        // Test case insensitivity and whitespace tolerance in membership checking
        val testEmail = "  STUDENT@med.edu "
        val isAllowed = subscribers.any { it.trim().equals(testEmail.trim(), ignoreCase = true) }
        assertTrue(isAllowed)

        val blockedEmail = "unauthorized@unknown.com"
        val isBlocked = subscribers.none { it.trim().equals(blockedEmail.trim(), ignoreCase = true) }
        assertTrue(isBlocked)
    }

    @Test
    fun testAccessManagerParseObjectJson() {
        val jsonObjectArray = """[{"email": "clinician@clinic.com"}, {"email": "surgeon@health.org"}]"""
        val subscribers = AccessManager.parseSubscribersJson(jsonObjectArray)
        assertEquals(2, subscribers.size)
        assertTrue(subscribers.contains("clinician@clinic.com"))
        assertTrue(subscribers.contains("surgeon@health.org"))
    }

    @Test
    fun testThemeManagerPreferencesAndNightMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Default should be System Default
        val initialTheme = com.example.ui.theme.ThemeManager.getThemePreference(context)
        assertEquals(com.example.ui.theme.ThemeManager.MODE_SYSTEM, initialTheme)

        // Switch to Dark Mode
        com.example.ui.theme.ThemeManager.setThemePreference(context, com.example.ui.theme.ThemeManager.MODE_DARK)
        assertEquals(com.example.ui.theme.ThemeManager.MODE_DARK, com.example.ui.theme.ThemeManager.getThemePreference(context))
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES, androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode())

        // Switch to Light Mode
        com.example.ui.theme.ThemeManager.setThemePreference(context, com.example.ui.theme.ThemeManager.MODE_LIGHT)
        assertEquals(com.example.ui.theme.ThemeManager.MODE_LIGHT, com.example.ui.theme.ThemeManager.getThemePreference(context))
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO, androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode())

        // Switch to System Default
        com.example.ui.theme.ThemeManager.setThemePreference(context, com.example.ui.theme.ThemeManager.MODE_SYSTEM)
        assertEquals(com.example.ui.theme.ThemeManager.MODE_SYSTEM, com.example.ui.theme.ThemeManager.getThemePreference(context))
        assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode())
    }

    @Test
    fun testArabicLocalizationStrings() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val config = android.content.res.Configuration(baseContext.resources.configuration)
        config.setLocale(java.util.Locale("ar"))
        val localizedContext = baseContext.createConfigurationContext(config)

        assertEquals("تسجيل", localizedContext.getString(R.string.nav_record))
        assertEquals("تسجيل الدخول باستخدام Google", localizedContext.getString(R.string.login_google_sign_in))
        assertEquals("تم تقييد الوصول", localizedContext.getString(R.string.access_restricted_title))
        assertEquals("ملخصات المذاكرة", localizedContext.getString(R.string.nav_study_notes))
        assertEquals("المكتبة", localizedContext.getString(R.string.nav_library))
        assertEquals("الإعدادات", localizedContext.getString(R.string.nav_settings))
        assertEquals("لغة التطبيق والتعريب", localizedContext.getString(R.string.settings_language_title))
        assertEquals("العربية (Arabic)", localizedContext.getString(R.string.settings_language_ar))
    }

    @Test
    fun testLanguageManagerPreferenceAndOptions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.ui.locale.LanguageManager.initLanguage(context)

        // Options should include system, en, ar
        val options = com.example.ui.locale.LanguageManager.LANGUAGE_OPTIONS
        assertTrue(options.contains(com.example.ui.locale.LanguageManager.LANG_SYSTEM))
        assertTrue(options.contains(com.example.ui.locale.LanguageManager.LANG_EN))
        assertTrue(options.contains(com.example.ui.locale.LanguageManager.LANG_AR))

        // Set to Arabic
        com.example.ui.locale.LanguageManager.setLanguagePreference(context, com.example.ui.locale.LanguageManager.LANG_AR, recreateActivity = false)
        assertEquals(com.example.ui.locale.LanguageManager.LANG_AR, com.example.ui.locale.LanguageManager.getLanguagePreference(context))
        assertEquals(com.example.ui.locale.LanguageManager.LANG_AR, com.example.ui.locale.LanguageManager.currentLanguage.value)

        // Set to English
        com.example.ui.locale.LanguageManager.setLanguagePreference(context, com.example.ui.locale.LanguageManager.LANG_EN, recreateActivity = false)
        assertEquals(com.example.ui.locale.LanguageManager.LANG_EN, com.example.ui.locale.LanguageManager.getLanguagePreference(context))

        // Set back to System
        com.example.ui.locale.LanguageManager.setLanguagePreference(context, com.example.ui.locale.LanguageManager.LANG_SYSTEM, recreateActivity = false)
        assertEquals(com.example.ui.locale.LanguageManager.LANG_SYSTEM, com.example.ui.locale.LanguageManager.getLanguagePreference(context))
    }
}


